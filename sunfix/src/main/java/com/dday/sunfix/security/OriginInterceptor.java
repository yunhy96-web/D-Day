package com.dday.sunfix.security;

import com.dday.sunfix.common.AppProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/** 변경 요청: JSON 또는 JSON 만, Origin 이 있으면 base-url 과 일치해야 함 (CSRF 방어, 설계 §2). */
@Component
@RequiredArgsConstructor
public class OriginInterceptor implements HandlerInterceptor {
    private final AppProps props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if ("GET".equals(req.getMethod())) return true;
        String ct = req.getContentType();
        boolean okType = ct == null ? "DELETE".equals(req.getMethod()) : ct.startsWith("application/json");
        if (!okType) { res.setStatus(415); return false; }
        String origin = req.getHeader("Origin");
        String base = props.getBaseUrl();
        if (origin != null) {
            // base-url 을 안 정했으면 요청 Host 와 비교(포트 포함). 터널 뒤에선 Host 가 원래 도메인 그대로 온다
            String host = req.getHeader("Host");
            boolean same = !base.isBlank() ? origin.equalsIgnoreCase(base)
                    : host != null && (origin.equalsIgnoreCase("https://" + host) || origin.equalsIgnoreCase("http://" + host));
            if (!same) { res.setStatus(403); return false; }
        }
        return true;
    }
}
