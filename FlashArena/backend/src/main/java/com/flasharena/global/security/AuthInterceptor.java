package com.flasharena.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flasharena.global.context.UserContext;
import com.flasharena.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bearer 토큰을 검증해 인가를 수행하는 인터셉터 (Spring Security 풀 필터체인 미사용).
 *
 * <p>실제 MSA 라면 API 게이트웨이가 토큰을 검증하고 {@code X-User-Id} 헤더로 사용자 UUID 를
 * 다운스트림 서비스에 forward 한다. 여기서는 단일 애플리케이션이므로 이 인터셉터가 그 역할을 흉내내
 * UserContext(ThreadLocal) 와 request attribute(userId / X-User-Id) 에 사용자 UUID 를 채워
 * order / payment 도메인이 읽을 수 있게 한다.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORS 프리플라이트(OPTIONS)는 토큰 없이 통과시킨다. 브라우저는 프리플라이트가 2xx 여야
        // 본 요청(POST /run 등)을 보내므로 여기서 401 로 막으면 대시보드가 동작하지 않는다.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return reject(response, "Authorization Bearer 토큰이 필요합니다.");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtProvider.AuthPayload payload = jwtProvider.parse(token);
            UserContext.set(payload.userId(), payload.role());
            // 다운스트림(order/payment) 전파용 — 게이트웨이의 X-User-Id forward 를 흉내낸다.
            request.setAttribute("userId", payload.userId());
            request.setAttribute("X-User-Id", payload.userId().toString());
            return true;
        } catch (JwtProvider.InvalidTokenException e) {
            return reject(response, e.getMessage());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of("error", "UNAUTHORIZED", "message", message)));
        return false;
    }
}
