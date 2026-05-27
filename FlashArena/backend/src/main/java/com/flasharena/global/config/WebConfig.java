package com.flasharena.global.config;

import com.flasharena.global.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
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
    /** 허용 origin 패턴(콤마 구분). 기본은 개발용 localhost, 배포 시 CORS_ALLOWED_ORIGINS 로 override. */
    private final String[] allowedOriginPatterns;

    public WebConfig(AuthInterceptor authInterceptor,
            @Value("${app.cors.allowed-origin-patterns}") String[] allowedOriginPatterns) {
        this.authInterceptor = authInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns;
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
        // 허용 origin 은 환경별로 다르다(개발=localhost 임의 포트, 배포=프론트 도메인).
        // 그래서 app.cors.allowed-origin-patterns(=CORS_ALLOWED_ORIGINS env)로 주입받는다.
        // allowCredentials 와 함께 쓰려면 allowedOrigins 와일드카드 대신 allowedOriginPatterns 를 써야 한다.
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
