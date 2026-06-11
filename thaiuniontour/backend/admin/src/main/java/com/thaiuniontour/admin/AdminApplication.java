package com.thaiuniontour.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 운영자용 Thymeleaf 서버렌더 애플리케이션.
 * 스키마는 api 모듈의 Flyway가 관리하므로 admin은 ddl-auto=none, flyway.enabled=false.
 */
@SpringBootApplication(scanBasePackages = "com.thaiuniontour")
@EntityScan("com.thaiuniontour.domain")
@EnableJpaRepositories("com.thaiuniontour.domain")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
