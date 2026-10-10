package com.dday.wedding.security;

import jakarta.servlet.http.HttpServletRequest;

/** 터널 경유라 CF-Connecting-IP 만 신뢰. X-Forwarded-For 는 보지 않는다. */
public final class ClientIp {
    private ClientIp() {}
    public static String of(HttpServletRequest req) {
        String cf = req.getHeader("CF-Connecting-IP");
        return cf != null && !cf.isBlank() ? cf : req.getRemoteAddr();
    }
}
