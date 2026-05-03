package com.hauly.platform.auth.presentation.rest;

import com.hauly.platform.auth.application.AuthService;
import com.hauly.platform.auth.application.command.ChangePasswordCommand;
import com.hauly.platform.auth.application.command.LoginCommand;
import com.hauly.platform.auth.application.command.RefreshCommand;
import com.hauly.platform.auth.application.query.CurrentUserView;
import com.hauly.platform.auth.presentation.dto.ChangePasswordRequest;
import com.hauly.platform.auth.presentation.dto.LoginRequest;
import com.hauly.platform.auth.presentation.dto.LoginResponse;
import com.hauly.platform.auth.presentation.dto.MeResponse;
import com.hauly.platform.auth.presentation.dto.RefreshRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth REST controller.
 * Presentation layer: calls only application layer (AuthService), never domain or infra directly.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_COOKIE  = "hauly_at";
    private static final String REFRESH_COOKIE = "hauly_rt";
    private static final String ACCESS_COOKIE_PATH  = "/";
    private static final String REFRESH_COOKIE_PATH = "/api/auth/refresh";

    // Access token TTL in seconds (30 min)
    private static final int ACCESS_COOKIE_MAX_AGE  = 30 * 60;
    // Refresh token TTL in seconds (14 days)
    private static final int REFRESH_COOKIE_MAX_AGE = 14 * 24 * 60 * 60;

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${hauly.security.cookie.secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    /**
     * POST /api/auth/login
     * Returns tokens in response body AND sets HttpOnly cookies.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(
                new LoginCommand(request.email(), request.password()));

        addAccessCookie(response, result.accessToken());
        addRefreshCookie(response, result.refreshToken());

        return ResponseEntity.ok(new LoginResponse(
                result.accessToken(), result.refreshToken(), result.user()));
    }

    /**
     * POST /api/auth/refresh
     * Reads refresh token from cookie or request body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody(required = false) RefreshRequest body,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request, body);
        String newAccessToken = authService.refresh(new RefreshCommand(refreshToken));

        addAccessCookie(response, newAccessToken);

        // Return new access token; refresh token unchanged
        return ResponseEntity.ok(new LoginResponse(newAccessToken, refreshToken, null));
    }

    /**
     * GET /api/auth/me — requires authentication (enforced by SecurityConfig).
     * Principal is the userId (Long) set by JwtAuthenticationFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Long userId) {
        CurrentUserView view = authService.currentUser(userId);
        return ResponseEntity.ok(MeResponse.from(view));
    }

    /**
     * POST /api/auth/logout — clears cookies.
     * Each cookie must be cleared with the SAME path it was set with — browsers key
     * cookies by (name, domain, path), so a delete cookie at a different path is a no-op.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearCookie(response, ACCESS_COOKIE, ACCESS_COOKIE_PATH);
        clearCookie(response, REFRESH_COOKIE, REFRESH_COOKIE_PATH);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/password — change password for the authenticated user.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                @AuthenticationPrincipal Long userId) {
        authService.changePassword(new ChangePasswordCommand(
                userId, request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------

    private String extractRefreshToken(HttpServletRequest request, RefreshRequest body) {
        // Prefer cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (REFRESH_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        // Fallback to body
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        throw new org.springframework.security.authentication.BadCredentialsException(
                "No refresh token provided");
    }

    private void addAccessCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ACCESS_COOKIE_PATH);
        cookie.setMaxAge(ACCESS_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
