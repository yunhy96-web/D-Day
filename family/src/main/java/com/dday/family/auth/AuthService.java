package com.dday.family.auth;

import com.dday.family.common.BadRequest;
import com.dday.family.common.Times;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 로그인·가입·세션 (설계 §4). 비밀번호는 BCrypt 해시만, 세션 토큰은 DB 에 SHA-256 만 둔다. */
@Service
@RequiredArgsConstructor
public class AuthService {
    public static final long SESSION_DAYS = 90;
    /** 지금은 가족 1개(설계 §3). 가입 신청은 여기로 */
    public static final long DEFAULT_FAMILY = 1;
    private static final SecureRandom RND = new SecureRandom();
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(10);
    /** 없는 아이디도 같은 시간 걸리게 비교할 더미 해시 */
    private static final String DUMMY_HASH = BCRYPT.encode("dummy-password");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public static String hash(String password) { return BCRYPT.encode(password); }

    public static boolean matches(String password, String hash) { return BCRYPT.matches(password, hash); }

    public record LoginResult(String token, String status) {}

    /** 실패면 null. BLOCKED 면 BadRequest("blocked") */
    public LoginResult login(String loginId, String password) {
        List<Object[]> rows = jdbc.query("""
                SELECT m.id, m.password_hash, fm.family_id, fm.status FROM member m
                JOIN family_member fm ON fm.member_id = m.id
                WHERE m.login_id = ? ORDER BY fm.family_id LIMIT 1""",
                (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)}, loginId);
        if (rows.isEmpty()) { BCRYPT.matches(password, DUMMY_HASH); return null; }
        Object[] r = rows.get(0);
        if (!BCRYPT.matches(password, (String) r[1])) return null;
        if ("BLOCKED".equals(r[3])) throw new BadRequest("blocked");
        return new LoginResult(newSession((long) r[0], (long) r[2]), (String) r[3]);
    }

    public String newSession(long memberId, long familyId) {
        byte[] b = new byte[32]; RND.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        jdbc.update("INSERT INTO session(token_hash, member_id, family_id, expires_at) VALUES (?,?,?,?)",
                sha256(token), memberId, familyId, Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString());
        return token;
    }

    public void logout(String token) {
        if (token != null) jdbc.update("DELETE FROM session WHERE token_hash=?", sha256(token));
    }

    public void killSessions(long memberId) { jdbc.update("DELETE FROM session WHERE member_id=?", memberId); }

    /** 가입 신청 → PENDING. 아이디 중복이면 BadRequest("loginIdTaken") */
    public long signup(String loginId, String password, String name, String lang, String theme, String fontScale) {
        String h = hash(password);
        try {
            return tx.execute(s -> {
                if (jdbc.queryForObject("SELECT COUNT(*) FROM member WHERE login_id=?", Integer.class, loginId) > 0) throw new BadRequest("loginIdTaken");
                long n = jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class);
                Long id = jdbc.queryForObject("""
                        INSERT INTO member(login_id, password_hash, name, lang, theme, font_scale, color)
                        VALUES (?,?,?,?,?,?,?) RETURNING id""", Long.class,
                        loginId, h, name, lang, theme, fontScale, Colors.forIndex((int) n));
                jdbc.update("INSERT INTO family_member(family_id, member_id, status) VALUES (?,?, 'PENDING')", DEFAULT_FAMILY, id);
                return id;
            });
        } catch (DataAccessException e) {   // 동시에 같은 아이디로 신청한 경우
            if (String.valueOf(e.getMessage()).contains("UNIQUE")) throw new BadRequest("loginIdTaken");
            throw e;
        }
    }

    private record Row(Instant expires, long memberId, long familyId, String loginId, String name, String color, String lang,
                       String theme, String font, boolean fontSet, String tz, String status, boolean mustChange) {}

    /** 쿠키 토큰 → Me. 만료·차단·없음이면 null. 남은 기간이 89일 미만이면 90일로 연장(하루 1번꼴) */
    public Me resolve(String token) {
        if (token == null || token.length() != 43) return null;
        String th = sha256(token);
        List<Row> rows = jdbc.query("""
                SELECT s.expires_at, m.id, s.family_id, m.login_id, m.name, m.color, m.lang, m.theme, m.font_scale,
                       m.font_scale_set_yn, m.timezone, fm.status, m.must_change_pw
                FROM session s JOIN member m ON m.id = s.member_id
                JOIN family_member fm ON fm.family_id = s.family_id AND fm.member_id = m.id
                WHERE s.token_hash = ?""", (rs, i) -> new Row(Instant.parse(rs.getString(1)), rs.getLong(2), rs.getLong(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                "Y".equals(rs.getString(10)), rs.getString(11), rs.getString(12), "Y".equals(rs.getString(13))), th);
        if (rows.isEmpty()) return null;
        Row r = rows.get(0);
        Instant now = Instant.now();
        if (r.expires().isBefore(now) || "BLOCKED".equals(r.status())) return null;
        if (r.expires().isBefore(now.plus(SESSION_DAYS - 1, ChronoUnit.DAYS))) {
            jdbc.update("UPDATE session SET expires_at=? WHERE token_hash=?",
                    now.plus(SESSION_DAYS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString(), th);
            jdbc.update("UPDATE member SET last_seen_at=? WHERE id=?", Times.now(), r.memberId());
        }
        return new Me(r.memberId(), r.familyId(), r.loginId(), r.name(), r.color(), r.lang(), r.theme(), r.font(), r.fontSet(),
                r.tz(), r.status(), r.mustChange(), permissions(r.familyId(), r.memberId()));
    }

    public Set<String> permissions(long familyId, long memberId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT rp.permission FROM member_role mr JOIN role_permission rp ON rp.role_code = mr.role_code
                WHERE mr.family_id = ? AND mr.member_id = ?""", String.class, familyId, memberId));
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** 관리자 비밀번호 초기화용 임시 비밀번호. 헷갈리는 글자(0 O 1 l I) 제외 */
    public static String tempPassword() {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(RND.nextInt(chars.length())));
        return sb.toString();
    }
}
