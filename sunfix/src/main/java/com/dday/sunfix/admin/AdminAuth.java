package com.dday.sunfix.admin;

import com.dday.sunfix.common.AppProps;
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

/**
 * 관리자 1명(sunflower) 로그인 + /admin/** 문지기 (설계 §8). 역할·권한 테이블 없음.
 * 비밀번호는 .env 의 BCrypt 해시와 비교, 세션 토큰은 DB 에 SHA-256 만.
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
        String hash = props.getAdminPwHash();
        boolean ok = BCRYPT.matches(password == null ? "" : password, hash.isBlank() ? DUMMY_HASH : hash)
                && !hash.isBlank() && props.getAdminLoginId().equals(loginId);
        if (!ok) return false;
        byte[] b = new byte[32]; RND.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        jdbc.update("DELETE FROM admin_session WHERE expires_at < ?", Times.now());
        jdbc.update("INSERT INTO admin_session(token_hash, expires_at) VALUES (?,?)", sha256(token), Times.after(SESSION_SEC));
        cookie(res, token, Duration.ofSeconds(SESSION_SEC));
        return true;
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
