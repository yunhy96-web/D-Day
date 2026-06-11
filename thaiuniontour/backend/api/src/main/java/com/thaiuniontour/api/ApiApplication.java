package com.thaiuniontour.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 고객 대면 REST API + (MVP) Outbox Relayer + 결제 Consumer 호스트.
 * 컴포넌트/엔티티/리포지토리 스캔: api + domain + common (domain 모듈이 별도 패키지라 명시).
 */
@SpringBootApplication(scanBasePackages = "com.thaiuniontour")
@EntityScan("com.thaiuniontour.domain")
@EnableJpaRepositories("com.thaiuniontour.domain")
@EnableScheduling
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
