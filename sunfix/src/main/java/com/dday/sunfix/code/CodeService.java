package com.dday.sunfix.code;

import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.NotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 공통코드 트리 메모리 캐시 (설계 §4). 관리자 저장 시 invalidate(). 이름은 라벨 'code.{code}'. */
@Service
@RequiredArgsConstructor
public class CodeService {
    public static final String CATEGORY = "CATEGORY", OPT_GROUP = "OPT_GROUP", OPT = "OPT", REGION = "REGION", REGION_SUB = "REGION_SUB";
    /** 부모 그룹 → 자식 그룹 */
    public static final Map<String, String> CHILD_GROUP = Map.of(CATEGORY, OPT_GROUP, OPT_GROUP, OPT, REGION, REGION_SUB);

    public record Code(String code, String group, String parent, int sort, boolean use) {}
    public record OptionGroup(Code group, List<Code> options) {}
    public record Region(Code sido, List<Code> subs) {}
    /** 검증된 사용자 선택. options 는 옵션 그룹 순서 */
    public record Selection(Code category, List<Code> options, Code region) {
        public String sido() { return region.parent(); }
        /** 같은 조합 비교용 정렬 키 'A,B,C' */
        public String optionKey() { return key(options.stream().map(Code::code).toList()); }
    }

    private final JdbcTemplate jdbc;
    private volatile Map<String, Code> byCode;
    private volatile Map<String, List<Code>> children;

    public static String key(List<String> codes) { return codes.stream().sorted().collect(Collectors.joining(",")); }

    public void invalidate() { byCode = null; }

    private void load() {
        if (byCode != null) return;
        List<Code> all = jdbc.query("SELECT code, group_code, parent_code, sort_no, use_yn FROM common_code ORDER BY sort_no, code",
                (rs, i) -> new Code(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), "Y".equals(rs.getString(5))));
        Map<String, List<Code>> ch = new HashMap<>();
        Map<String, Code> by = new HashMap<>();
        for (Code c : all) { by.put(c.code(), c); ch.computeIfAbsent(c.parent() == null ? c.group() : c.parent(), k -> new ArrayList<>()).add(c); }
        children = ch;
        byCode = by;
    }

    public Code find(String code) { load(); return code == null ? null : byCode.get(code); }

    /** 루트 그룹(CATEGORY·REGION)이나 부모 코드의 자식. activeOnly 면 use_yn='Y' 만 */
    public List<Code> children(String parentOrRootGroup, boolean activeOnly) {
        load();
        return children.getOrDefault(parentOrRootGroup, List.of()).stream().filter(c -> !activeOnly || c.use()).toList();
    }

    public Code category(String code) {
        Code c = find(code);
        if (c == null || !CATEGORY.equals(c.group()) || !c.use()) throw new NotFound();
        return c;
    }

    public List<OptionGroup> optionGroups(String category, boolean activeOnly) {
        return children(category, activeOnly).stream().map(g -> new OptionGroup(g, children(g.code(), activeOnly))).filter(g -> !g.options().isEmpty()).toList();
    }

    public List<Region> regions(boolean activeOnly) {
        return children(REGION, activeOnly).stream().map(s -> new Region(s, children(s.code(), activeOnly))).toList();
    }

    /** 옵션 그룹마다 정확히 하나 + 시/군/구 하나. 아니면 BadRequest */
    public Selection select(String category, List<String> options, String region) {
        Code cat = category(category);
        Code reg = find(region);
        if (reg == null || !REGION_SUB.equals(reg.group()) || !reg.use()) throw new BadRequest("region");
        List<OptionGroup> groups = optionGroups(cat.code(), true);
        Set<String> picked = new HashSet<>(options == null ? List.of() : options);
        List<Code> chosen = new ArrayList<>();
        for (OptionGroup g : groups) {
            List<Code> hit = g.options().stream().filter(o -> picked.contains(o.code())).toList();
            if (hit.size() != 1) throw new BadRequest("options");
            chosen.add(hit.get(0));
        }
        if (chosen.size() != picked.size()) throw new BadRequest("options");
        return new Selection(cat, chosen, reg);
    }

    /** 저장된 'A,B' 를 옵션 그룹 순서대로 (제보·문의 표시용). 없어진 코드는 건너뜀 */
    public List<Code> options(String optionKey) {
        load();
        return Arrays.stream(optionKey.split(",")).map(byCode::get).filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(o -> find(o.parent()) == null ? 0 : find(o.parent()).sort())).toList();
    }
}
