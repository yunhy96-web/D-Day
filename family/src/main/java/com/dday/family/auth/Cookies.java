package com.dday.family.auth;

import com.dday.family.common.AppProps;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 세션 쿠키(fs) + 로그인 전 화면 설정 쿠키(lang·theme·font). 전부 HttpOnly, SameSite=Lax. */
@Component
@RequiredArgsConstructor
public class Cookies {
    public static final String SESSION = "fs";
    private final AppProps props;

    public static String get(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (c.getName().equals(name)) return c.getValue();
        return null;
    }

    public void set(HttpServletResponse res, String name, String value, Duration maxAge) {
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value).path("/").httpOnly(true)
                .secure(props.isCookieSecure()).sameSite("Lax").maxAge(maxAge).build().toString());
    }

    public void session(HttpServletResponse res, String token) { set(res, SESSION, token, Duration.ofDays(AuthService.SESSION_DAYS)); }

    public void clearSession(HttpServletResponse res) { set(res, SESSION, "", Duration.ZERO); }
}
