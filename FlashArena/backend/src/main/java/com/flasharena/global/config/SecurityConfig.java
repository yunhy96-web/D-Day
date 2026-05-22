package com.flasharena.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * spring-security-crypto 의 BCryptPasswordEncoder 만 사용한다.
 * (Spring Security 풀 필터체인은 도입하지 않는다 — 인가는 AuthInterceptor 담당)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
