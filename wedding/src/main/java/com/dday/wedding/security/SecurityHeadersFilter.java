package com.dday.wedding.security;

import com.dday.wedding.config.WeddingProps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

/** 설계 §13-2. Spring Security 없이 헤더만. 인라인 스크립트는 요청별 nonce. */
@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private static final SecureRandom RND = new SecureRandom();
    private final WeddingProps props;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // 정식 도메인(base-url) 이 아닌 호스트(www·예전 wedding.thaiuniontour.com) 는 301. API 의 Origin 검사가 base-url 하나만 허용하므로
        String base = props.getBaseUrl();
        if (base != null && !base.isBlank() && !req.getServerName().equalsIgnoreCase(URI.create(base).getHost())) {
            String q = req.getQueryString();
            res.setStatus(301);
            res.setHeader("Location", base + req.getRequestURI() + (q == null ? "" : "?" + q));
            return;
        }
        byte[] b = new byte[16]; RND.nextBytes(b);
        String nonce = Base64.getEncoder().encodeToString(b);
        req.setAttribute("cspNonce", nonce);
        res.setHeader("Content-Security-Policy", String.join("; ",
                "default-src 'self'",
                // 카카오맵 SDK 는 dapi → *.kakaocdn.net(메인 js) → *.daumcdn.net(타일) 순으로 부른다. 스킴 생략 = 페이지 스킴 따름(로컬 http·운영 https)
                "script-src 'self' 'nonce-" + nonce + "' dapi.kakao.com *.kakaocdn.net *.daumcdn.net https://challenges.cloudflare.com https://cdn.jsdelivr.net/npm/sortablejs@1.15.2/",   // 어드민 SortableJS 한 파일만
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://hangeul.pstatic.net *.daumcdn.net",   // 카카오맵 SDK 의 인라인 스타일
                "font-src https://fonts.gstatic.com https://cdn.jsdelivr.net https://hangeul.pstatic.net",
                "img-src 'self' data: blob: *.daumcdn.net *.kakaocdn.net https://*.kr.ncloudstorage.com",   // blob: 업로드 미리보기, ncloud: 사진 서명 URL
                "connect-src 'self' dapi.kakao.com *.daumcdn.net *.kakaocdn.net",
                "frame-src https://challenges.cloudflare.com",
                "base-uri 'self'", "form-action 'self'", "frame-ancestors 'none'"));
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        chain.doFilter(req, res);
    }
}
