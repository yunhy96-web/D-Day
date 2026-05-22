package com.flasharena.auth.application;

import com.flasharena.auth.domain.User;
import com.flasharena.auth.infrastructure.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 기동 시 고정 공용 계정을 멱등하게 시딩한다.
 * (DDL 은 BCrypt 인코딩 불가라 Phase 2 앱에서 1회 삽입 — V2__auth.sql 주석 참조)
 */
@Slf4j
@Component
public class DefaultAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUsername;
    private final String defaultPassword;
    private final String defaultRole;

    public DefaultAccountSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.default-username}") String defaultUsername,
            @Value("${app.auth.default-password}") String defaultPassword,
            @Value("${app.auth.default-role}") String defaultRole) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
        this.defaultRole = defaultRole;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(defaultUsername)) {
            log.info("[seed] 기본 계정 '{}' 이미 존재 → 스킵", defaultUsername);
            return;
        }
        User user = User.builder()
                .username(defaultUsername)
                .password(passwordEncoder.encode(defaultPassword))
                .role(defaultRole)
                .build();
        userRepository.save(user);
        log.info("[seed] 기본 계정 '{}' (role={}) 생성 완료", defaultUsername, defaultRole);
    }
}
