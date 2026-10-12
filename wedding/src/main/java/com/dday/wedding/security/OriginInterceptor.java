package com.dday.wedding.security;

import com.dday.wedding.config.WeddingProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/** 변경 요청: JSON(방명록 사진만 image/jpeg), Origin 이 있으면 base-url 과 일치해야 함 (설계 §13-2). */
@Component
@RequiredArgsConstructor
public class OriginInterceptor implements HandlerInterceptor {
    private final WeddingProps props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if ("GET".equals(req.getMethod())) return true;
        String ct = req.getContentType();
        if (ct == null || !(ct.startsWith("application/json") || ct.startsWith("image/jpeg"))) { res.setStatus(415); return false; }
        String origin = req.getHeader("Origin");
        String base = props.getBaseUrl();
        if (origin != null && !base.isBlank() && !origin.equalsIgnoreCase(base)) { res.setStatus(403); return false; }
        return true;
    }
}
