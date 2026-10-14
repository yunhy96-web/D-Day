package com.dday.family.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * 모든 요청의 문지기 (설계 §11). 쿠키 → Me 를 request 속성에 두고, 상태별로 갈 수 있는 곳을 제한한다.
 * 페이지는 리다이렉트, API(/api/**, /admin/api/**, /files/**)는 401·403.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    public static final String ATTR = "me";
    private static final Set<String> PUBLIC = Set.of("/login", "/signup", "/api/login", "/api/signup", "/api/prefs", "/api/logout");
    private static final Set<String> PENDING_OK = Set.of("/pending", "/api/logout", "/api/prefs");
    private static final Set<String> MUST_CHANGE_OK = Set.of("/password", "/api/me/password", "/api/logout", "/api/prefs");
    private final AuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        Me me = auth.resolve(Cookies.get(req, Cookies.SESSION));
        if (me != null) req.setAttribute(ATTR, me);
        String p = req.getRequestURI();
        boolean api = p.startsWith("/api/") || p.startsWith("/admin/api/") || p.startsWith("/files/");

        if (PUBLIC.contains(p)) {
            if (me != null && (p.equals("/login") || p.equals("/signup"))) { res.sendRedirect("/"); return false; }
            return true;
        }
        if (me == null) return deny(res, api, 401, "/login");
        if (!me.active()) return PENDING_OK.contains(p) || deny(res, api, 403, "/pending");
        if (me.mustChangePw()) return MUST_CHANGE_OK.contains(p) || deny(res, api, 403, "/password");
        if (!me.fontScaleSet() && !api && !p.equals("/welcome")) { res.sendRedirect("/welcome"); return false; }
        if (p.startsWith("/admin") && !me.admin()) return deny(res, api, 403, "/");
        return true;
    }

    private static boolean deny(HttpServletResponse res, boolean api, int status, String page) throws IOException {
        if (api) res.setStatus(status); else res.sendRedirect(page);
        return false;
    }
}
