package com.dday.family.shift;

import com.dday.family.auth.Me;
import com.dday.family.auth.Perm;
import com.dday.family.common.BadRequest;
import com.dday.family.common.NotFound;
import com.dday.family.common.Rs;
import com.dday.family.common.Times;
import com.dday.family.common.Valid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 근무 캘린더 (설계 §5-1). 사람마다 교대 종류(shift_type) + 순환 패턴(shift_setting) + 날짜별 근무(shift_day).
 * 패턴 채우기도 shift_day 에 행으로 쓴다. 고칠 수 있는 건 자기 것, shift.write.any 권한자는 남의 것도.
 */
@Service
@RequiredArgsConstructor
public class ShiftService {
    public static final Set<String> SYSTEMS = Set.of("2", "3", "4", "custom");
    private static final int MAX_FILL_MONTHS = 12, MAX_BULK = 400;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;

    public record Type(long id, String name, String shortName, String color, String startTime, String endTime, int sort, boolean deleted) {}
    public record Setting(String system, List<Long> pattern, String patternStart) {}
    public record Day(String day, Long typeId, String note) {}
    public record Person(long id, String name, String color, boolean editable, Setting setting, List<Type> types, List<Day> days) {}
    public record Today(String name, String color, String typeName, String shortName, String typeColor, String startTime, String endTime, String note) {}
    public record TypeBody(String name, String shortName, String color, String startTime, String endTime) {}

    private record Preset(String ko, String th, String shortKo, String shortTh, String color, String start, String end) {}
    private static final Map<String, List<Preset>> PRESETS = Map.of(
            "2", List.of(new Preset("주간", "กะเช้า", "주", "ช", "blue", "08:00", "20:00"),
                    new Preset("야간", "กะดึก", "야", "ด", "purple", "20:00", "08:00"),
                    new Preset("휴무", "วันหยุด", "휴", "ห", "gray", null, null)),
            "3", List.of(new Preset("주간", "กะเช้า", "D", "D", "sky", "06:00", "14:00"),
                    new Preset("오후", "กะบ่าย", "E", "E", "amber", "14:00", "22:00"),
                    new Preset("야간", "กะดึก", "N", "N", "purple", "22:00", "06:00"),
                    new Preset("휴무", "วันหยุด", "휴", "ห", "gray", null, null)),
            "4", List.of(new Preset("주간", "กะเช้า", "주", "ช", "blue", "07:00", "19:00"),
                    new Preset("야간", "กะดึก", "야", "ด", "purple", "19:00", "07:00"),
                    new Preset("비번", "ออฟ", "비", "อ", "green", null, null),
                    new Preset("휴무", "วันหยุด", "휴", "ห", "gray", null, null)),
            "custom", List.of(new Preset("근무", "ทำงาน", "근", "ท", "blue", "09:00", "18:00"),
                    new Preset("휴무", "วันหยุด", "휴", "ห", "gray", null, null)));
    /** 프리셋 순환: PRESETS 목록의 인덱스 */
    private static final Map<String, int[]> PRESET_PATTERN = Map.of(
            "2", new int[]{0, 0, 1, 1, 2, 2},
            "3", new int[]{0, 0, 1, 1, 2, 2, 3, 3},
            "4", new int[]{0, 0, 1, 1, 2, 3, 3, 3},
            "custom", new int[]{});

    /** 고칠 대상 사람. null 이면 나 */
    public long target(Me me, Long memberId) {
        if (memberId == null || memberId == me.memberId()) { me.require(Perm.SHIFT_WRITE); return me.memberId(); }
        me.require(Perm.SHIFT_WRITE_ANY);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM family_member WHERE family_id=? AND member_id=? AND status='ACTIVE'", Integer.class, me.familyId(), memberId);
        if (n == null || n == 0) throw new NotFound();
        return memberId;
    }

    // ---- 조회

    /** scope=family 면 근무표가 있는 가족 전원, 아니면 한 사람(memberId 또는 나) */
    public List<Person> month(Me me, String month, boolean family, Long memberId) {
        YearMonth ym;
        try { ym = YearMonth.parse(month); } catch (DateTimeParseException | NullPointerException e) { throw new BadRequest("month"); }
        // 앞뒤 달 일부도 달력에 보이므로 1주씩 여유
        String from = ym.atDay(1).minusDays(7).toString(), to = ym.atEndOfMonth().plusDays(7).toString();
        List<Object[]> people;
        if (family) {
            people = jdbc.query("""
                    SELECT m.id, m.name, m.color FROM member m JOIN family_member fm ON fm.member_id=m.id AND fm.family_id=? AND fm.status='ACTIVE'
                    WHERE EXISTS(SELECT 1 FROM shift_setting s WHERE s.family_id=fm.family_id AND s.member_id=m.id)
                       OR EXISTS(SELECT 1 FROM shift_day d WHERE d.family_id=fm.family_id AND d.member_id=m.id AND d.day BETWEEN ? AND ?)
                    ORDER BY m.id = ? DESC, m.id""", (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)},
                    me.familyId(), from, to, me.memberId());
        } else {
            long who = memberId == null ? me.memberId() : memberId;
            people = jdbc.query("""
                    SELECT m.id, m.name, m.color FROM member m JOIN family_member fm ON fm.member_id=m.id AND fm.family_id=? AND fm.status='ACTIVE'
                    WHERE m.id=?""", (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)}, me.familyId(), who);
            if (people.isEmpty()) throw new NotFound();
        }
        List<Person> out = new ArrayList<>();
        for (Object[] p : people) {
            long mid = (long) p[0];
            boolean editable = mid == me.memberId() ? me.has(Perm.SHIFT_WRITE) : me.has(Perm.SHIFT_WRITE_ANY);
            List<Day> days = jdbc.query("SELECT day, shift_type_id, note FROM shift_day WHERE family_id=? AND member_id=? AND day BETWEEN ? AND ? ORDER BY day",
                    (rs, i) -> new Day(rs.getString(1), Rs.lng(rs, 2), rs.getString(3)), me.familyId(), mid, from, to);
            out.add(new Person(mid, (String) p[1], (String) p[2], editable, setting(me.familyId(), mid), types(me.familyId(), mid), days));
        }
        return out;
    }

    public List<Type> types(long fid, long mid) {
        return jdbc.query("SELECT id, name, short, color, start_time, end_time, sort, deleted_at FROM shift_type WHERE family_id=? AND member_id=? ORDER BY sort, id",
                (rs, i) -> new Type(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8) != null),
                fid, mid);
    }

    public Setting setting(long fid, long mid) {
        List<Setting> r = jdbc.query("SELECT system, pattern, pattern_start FROM shift_setting WHERE family_id=? AND member_id=?",
                (rs, i) -> new Setting(rs.getString(1), parsePattern(rs.getString(2)), rs.getString(3)), fid, mid);
        return r.isEmpty() ? null : r.get(0);
    }

    /** 홈 "오늘" 줄: 오늘 근무·메모가 있는 가족 */
    public List<Today> today(long fid, String day) {
        return jdbc.query("""
                SELECT m.name, m.color, t.name, t.short, t.color, t.start_time, t.end_time, d.note
                FROM shift_day d JOIN member m ON m.id=d.member_id
                JOIN family_member fm ON fm.family_id=d.family_id AND fm.member_id=m.id AND fm.status='ACTIVE'
                LEFT JOIN shift_type t ON t.id=d.shift_type_id
                WHERE d.family_id=? AND d.day=? ORDER BY m.id""",
                (rs, i) -> new Today(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)),
                fid, day);
    }

    // ---- 설정

    /** 근무 방식 고르기: 기존 교대는 숨기고(과거 기록 표시는 유지) 프리셋으로 새로 만든다 */
    public Setting applyPreset(Me me, long mid, String system, String lang) {
        Valid.oneOf(system, SYSTEMS, "system");
        long fid = me.familyId();
        boolean th = "th".equals(lang);
        return tx.execute(s -> {
            jdbc.update("UPDATE shift_type SET deleted_at=? WHERE family_id=? AND member_id=? AND deleted_at IS NULL", Times.now(), fid, mid);
            List<Long> ids = new ArrayList<>();
            List<Preset> presets = PRESETS.get(system);
            for (int i = 0; i < presets.size(); i++) {
                Preset p = presets.get(i);
                ids.add(jdbc.queryForObject("INSERT INTO shift_type(family_id, member_id, name, short, color, start_time, end_time, sort) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
                        Long.class, fid, mid, th ? p.th() : p.ko(), th ? p.shortTh() : p.shortKo(), p.color(), p.start(), p.end(), i));
            }
            List<Long> pattern = new ArrayList<>();
            for (int idx : PRESET_PATTERN.get(system)) pattern.add(ids.get(idx));
            Setting st = new Setting(system, pattern, null);
            saveSetting(fid, mid, st);
            return st;
        });
    }

    public void savePattern(Me me, long mid, List<Long> pattern) {
        Setting cur = setting(me.familyId(), mid);
        if (cur == null) throw new BadRequest("noSetting");
        List<Long> p = pattern == null ? List.of() : pattern;
        if (p.size() > 60) throw new BadRequest("pattern");
        Set<Long> live = liveTypeIds(me.familyId(), mid);
        for (Long id : p) if (!live.contains(id)) throw new BadRequest("pattern");
        saveSetting(me.familyId(), mid, new Setting(cur.system(), p, cur.patternStart()));
    }

    private void saveSetting(long fid, long mid, Setting st) {
        jdbc.update("""
                INSERT INTO shift_setting(family_id, member_id, system, pattern, pattern_start, updated_at) VALUES (?,?,?,?,?,?)
                ON CONFLICT(family_id, member_id) DO UPDATE SET system=excluded.system, pattern=excluded.pattern,
                  pattern_start=excluded.pattern_start, updated_at=excluded.updated_at""",
                fid, mid, st.system(), writePattern(st.pattern()), st.patternStart(), Times.now());
    }

    public long createType(Me me, long mid, TypeBody b) {
        TypeBody v = validate(b);
        if (setting(me.familyId(), mid) == null) throw new BadRequest("noSetting");
        return jdbc.queryForObject("""
                INSERT INTO shift_type(family_id, member_id, name, short, color, start_time, end_time, sort)
                VALUES (?,?,?,?,?,?,?, (SELECT COALESCE(MAX(sort),0)+1 FROM shift_type WHERE family_id=? AND member_id=?)) RETURNING id""",
                Long.class, me.familyId(), mid, v.name(), v.shortName(), v.color(), v.startTime(), v.endTime(), me.familyId(), mid);
    }

    public void updateType(Me me, long typeId, TypeBody b) {
        long mid = target(me, ownerOf(me, typeId));
        TypeBody v = validate(b);
        jdbc.update("UPDATE shift_type SET name=?, short=?, color=?, start_time=?, end_time=? WHERE id=? AND family_id=? AND member_id=?",
                v.name(), v.shortName(), v.color(), v.startTime(), v.endTime(), typeId, me.familyId(), mid);
    }

    /** 숨김. 패턴에서도 뺀다. 이미 칠한 날은 그대로 보인다 */
    public void deleteType(Me me, long typeId) {
        long mid = target(me, ownerOf(me, typeId));
        tx.executeWithoutResult(s -> {
            jdbc.update("UPDATE shift_type SET deleted_at=? WHERE id=? AND family_id=? AND member_id=?", Times.now(), typeId, me.familyId(), mid);
            Setting st = setting(me.familyId(), mid);
            if (st != null) saveSetting(me.familyId(), mid, new Setting(st.system(), st.pattern().stream().filter(x -> x != typeId).toList(), st.patternStart()));
        });
    }

    private long ownerOf(Me me, long typeId) {
        List<Long> r = jdbc.queryForList("SELECT member_id FROM shift_type WHERE id=? AND family_id=? AND deleted_at IS NULL", Long.class, typeId, me.familyId());
        if (r.isEmpty()) throw new NotFound();
        return r.get(0);
    }

    private static TypeBody validate(TypeBody b) {
        String name = Valid.text(b.name(), 10, "typeName");
        String sh = Valid.text(b.shortName(), 2, "typeShort");
        String color = Valid.oneOf(b.color(), Valid.COLORS, "color");
        String st = Valid.optTime(b.startTime()), en = Valid.optTime(b.endTime());
        if ((st == null) != (en == null)) throw new BadRequest("time");
        return new TypeBody(name, sh, color, st, en);
    }

    // ---- 날짜

    /** 하나 바꾸고 이전 값을 돌려준다(되돌리기용). typeId·note 둘 다 비면 그 날을 지운다 */
    public Day setDay(Me me, long mid, String day, Long typeId, String note) {
        Map<String, Day> prev = setDays(me, mid, List.of(new Day(day, typeId, note)));
        return prev.values().iterator().next();
    }

    /** 여러 날. 반환: 날짜 → 이전 값(없던 날은 typeId·note null) */
    public Map<String, Day> setDays(Me me, long mid, List<Day> days) {
        if (days == null || days.isEmpty() || days.size() > MAX_BULK) throw new BadRequest("days");
        long fid = me.familyId();
        Set<Long> allowed = allTypeIds(fid, mid);
        List<Day> clean = new ArrayList<>();
        for (Day d : days) {
            if (d.typeId() != null && !allowed.contains(d.typeId())) throw new BadRequest("type");
            clean.add(new Day(Valid.date(d.day()), d.typeId(), Valid.optText(d.note(), 30, "note")));
        }
        return tx.execute(s -> {
            Map<String, Day> prev = new HashMap<>();
            for (Day d : clean) {
                List<Day> old = jdbc.query("SELECT day, shift_type_id, note FROM shift_day WHERE family_id=? AND member_id=? AND day=?",
                        (rs, i) -> new Day(rs.getString(1), Rs.lng(rs, 2), rs.getString(3)), fid, mid, d.day());
                prev.putIfAbsent(d.day(), old.isEmpty() ? new Day(d.day(), null, null) : old.get(0));
                write(fid, mid, d, me.memberId());
            }
            return prev;
        });
    }

    private void write(long fid, long mid, Day d, long by) {
        if (d.typeId() == null && d.note() == null) {
            jdbc.update("DELETE FROM shift_day WHERE family_id=? AND member_id=? AND day=?", fid, mid, d.day());
            return;
        }
        jdbc.update("""
                INSERT INTO shift_day(family_id, member_id, day, shift_type_id, note, updated_at, updated_by) VALUES (?,?,?,?,?,?,?)
                ON CONFLICT(family_id, member_id, day) DO UPDATE SET shift_type_id=excluded.shift_type_id, note=excluded.note,
                  updated_at=excluded.updated_at, updated_by=excluded.updated_by""",
                fid, mid, d.day(), d.typeId(), d.note(), Times.now(), by);
    }

    /** 패턴으로 start 부터 months 개월 채움. 메모는 유지. 반환: 덮어쓴 날들의 이전 값 */
    public List<Day> fill(Me me, long mid, String start, int months) {
        if (months < 1 || months > MAX_FILL_MONTHS) throw new BadRequest("months");
        LocalDate from = LocalDate.parse(Valid.date(start));
        Setting st = setting(me.familyId(), mid);
        if (st == null || st.pattern().isEmpty()) throw new BadRequest("pattern");
        LocalDate to = from.plusMonths(months);
        long fid = me.familyId();
        return tx.execute(s -> {
            Map<String, Day> old = new HashMap<>();
            jdbc.query("SELECT day, shift_type_id, note FROM shift_day WHERE family_id=? AND member_id=? AND day >= ? AND day < ?",
                    (rs, i) -> old.put(rs.getString(1), new Day(rs.getString(1), Rs.lng(rs, 2), rs.getString(3))), fid, mid, from.toString(), to.toString());
            List<Day> prev = new ArrayList<>();
            int n = st.pattern().size();
            for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
                String key = d.toString();
                Day o = old.getOrDefault(key, new Day(key, null, null));
                prev.add(o);
                long type = st.pattern().get((int) Math.floorMod(ChronoUnit.DAYS.between(from, d), (long) n));
                write(fid, mid, new Day(key, type, o.note()), me.memberId());
            }
            saveSetting(fid, mid, new Setting(st.system(), st.pattern(), from.toString()));
            return prev;
        });
    }

    private Set<Long> liveTypeIds(long fid, long mid) {
        return new HashSet<>(jdbc.queryForList("SELECT id FROM shift_type WHERE family_id=? AND member_id=? AND deleted_at IS NULL", Long.class, fid, mid));
    }

    private Set<Long> allTypeIds(long fid, long mid) {
        return new HashSet<>(jdbc.queryForList("SELECT id FROM shift_type WHERE family_id=? AND member_id=?", Long.class, fid, mid));
    }

    private List<Long> parsePattern(String s) {
        if (s == null || s.isBlank()) return List.of();
        try { return json.readValue(s, new TypeReference<List<Long>>() {}); } catch (JsonProcessingException e) { return List.of(); }
    }

    private String writePattern(List<Long> p) {
        try { return json.writeValueAsString(p == null ? List.of() : p); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
