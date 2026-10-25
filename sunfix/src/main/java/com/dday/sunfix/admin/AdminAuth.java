package com.dday.sunfix.admin;

import com.dday.sunfix.common.AppProps;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.Times;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * 관리자 1명(sunflower) 로그인 + /admin/** 문지기 (설계 §8). 역할·권한 테이블 없음.
 * 비밀번호는 BCrypt 해시와 비교(화면에서 바꾼 admin_account 가 있으면 그것, 없으면 .env), 세션 토큰은 DB 에 SHA-256 만.
 */
@Component
@RequiredArgsConstructor
public class AdminAuth implements HandlerInterceptor {
    public static final String COOKIE = "sa";
    private static final long SESSION_SEC = 30L * 24 * 3600;
    private static final SecureRandom RND = new SecureRandom();
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(10);
    private static final String DUMMY_HASH = BCRYPT.encode("dummy-password");
    private final JdbcTemplate jdbc;
    private final AppProps props;

    /** 성공하면 세션 쿠키를 심고 true */
    public boolean login(String loginId, String password, HttpServletResponse res) {
        if (!props.getAdminLoginId().equals(loginId) | !matches(password)) return false;   // | : 아이디가 틀려도 같은 시간
        newSession(res);
        return true;
    }

    /** 현재 비밀번호 확인 후 변경. 모든 세션을 끊고 지금 기기만 새 세션 */
    public void changePassword(String current, String next, HttpServletResponse res) {
        if (!matches(current)) throw new BadRequest("currentPassword");
        if (next == null || next.length() < 8 || next.getBytes(StandardCharsets.UTF_8).length > 72) throw new BadRequest("password");   // BCrypt 는 72바이트까지만 본다
        jdbc.update("""
                INSERT INTO admin_account(id, password_hash, updated_at) VALUES (1, ?, ?)
                ON CONFLICT(id) DO UPDATE SET password_hash = excluded.password_hash, updated_at = excluded.updated_at""",
                BCRYPT.encode(next), Times.now());
        jdbc.update("DELETE FROM admin_session");
        newSession(res);
    }

    private boolean matches(String password) {
        List<String> db = jdbc.queryForList("SELECT password_hash FROM admin_account WHERE id = 1", String.class);
        String hash = db.isEmpty() ? props.getAdminPwHash() : db.get(0);
        boolean ok = BCRYPT.matches(password == null ? "" : password, hash.isBlank() ? DUMMY_HASH : hash);
        return ok && !hash.isBlank();
    }

    private void newSession(HttpServletResponse res) {
        byte[] b = new byte[32]; RND.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        jdbc.update("DELETE FROM admin_session WHERE expires_at < ?", Times.now());
        jdbc.update("INSERT INTO admin_session(token_hash, expires_at) VALUES (?,?)", sha256(token), Times.after(SESSION_SEC));
        cookie(res, token, Duration.ofSeconds(SESSION_SEC));
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String t = token(req);
        if (t != null) jdbc.update("DELETE FROM admin_session WHERE token_hash = ?", sha256(t));
        cookie(res, "", Duration.ZERO);
    }

    public boolean isAdmin(HttpServletRequest req) {
        String t = token(req);
        return t != null && jdbc.queryForObject("SELECT COUNT(*) FROM admin_session WHERE token_hash = ? AND expires_at > ?",
                Integer.class, sha256(t), Times.now()) > 0;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        String p = req.getRequestURI();
        if (p.equals("/admin/login") || p.equals("/admin/api/login") || isAdmin(req)) return true;
        if (p.startsWith("/admin/api/")) res.setStatus(401); else res.sendRedirect("/admin/login");
        return false;
    }

    private void cookie(HttpServletResponse res, String value, Duration age) {
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, value).path("/admin").httpOnly(true)
                .secure(props.isCookieSecure()).sameSite("Strict").maxAge(age).build().toString());
    }

    private static String token(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (c.getName().equals(COOKIE)) return c.getValue();
        return null;
    }

    private static String sha256(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
