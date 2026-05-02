package com.hauly.platform.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configures BCrypt password encoder with cost factor 12.
 * Plan section 7: BCrypt cost 12 is intentionally slow (~250ms hash).
 */
@Configuration
public class BcryptPasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
