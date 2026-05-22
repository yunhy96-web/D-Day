package com.flasharena.global.config;

import com.flasharena.global.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 인가 인터셉터 등록 + CORS 설정.
 * /api/** 를 보호하되 로그인/헬스 경로는 제외한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // /simulator/stream/** 은 EventSource 라 Bearer 헤더를 못 보낸다 → ?token 쿼리로 컨트롤러에서 직접 검증.
                .excludePathPatterns("/api/auth/login", "/api/simulator/stream/**", "/actuator/**", "/health");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Phase 5 Vite 개발 서버. vite 가 5173 점유 시 5174/5175 로 떨어지므로
        // localhost 임의 포트를 허용한다(개발용). allowCredentials 와 함께 쓰려면
        // allowedOrigins 와일드카드 대신 allowedOriginPatterns 를 써야 한다.
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
