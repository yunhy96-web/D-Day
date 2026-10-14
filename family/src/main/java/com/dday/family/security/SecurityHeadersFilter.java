package com.dday.family.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/** 설계 §11. Spring Security 없이 헤더만. 인라인 스크립트는 요청별 nonce. 검색엔진 차단. */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private static final SecureRandom RND = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        byte[] b = new byte[16]; RND.nextBytes(b);
        String nonce = Base64.getEncoder().encodeToString(b);
        req.setAttribute("cspNonce", nonce);
        res.setHeader("Content-Security-Policy", String.join("; ",
                "default-src 'self'",
                "script-src 'self' 'nonce-" + nonce + "'",
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com",
                "font-src https://cdn.jsdelivr.net https://fonts.gstatic.com",
                "img-src 'self' data: blob: https://*.kr.ncloudstorage.com",
                "connect-src 'self'",
                "base-uri 'self'", "form-action 'self'", "frame-ancestors 'none'"));
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("X-Robots-Tag", "noindex, nofollow");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        chain.doFilter(req, res);
    }
}
