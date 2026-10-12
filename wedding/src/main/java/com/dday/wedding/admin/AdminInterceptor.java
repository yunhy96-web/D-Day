package com.dday.wedding.admin;

import com.dday.wedding.config.WeddingProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /admin/api/**: 로그인 세션 확인 + 변경 요청은 Origin·Content-Type 검사 (설계 §15-1). */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {
    static final String ATTR = "admin";
    private final WeddingProps props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!"GET".equals(req.getMethod())) {
            String ct = req.getContentType();
            if (ct == null || !(ct.startsWith("application/json") || ct.startsWith("image/jpeg") || ct.startsWith("application/octet-stream"))) { res.setStatus(415); return false; }
            String origin = req.getHeader("Origin");
            String base = props.getBaseUrl();
            if (origin != null && !base.isBlank() && !origin.equalsIgnoreCase(base)) { res.setStatus(403); return false; }
        }
        if (req.getRequestURI().equals("/admin/api/login")) return true;
        if (!loggedIn(req)) { res.setStatus(401); return false; }
        return true;
    }

    static boolean loggedIn(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s != null && Boolean.TRUE.equals(s.getAttribute(ATTR));
    }
}
