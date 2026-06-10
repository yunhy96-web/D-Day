package com.thaiuniontour.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 운영자용 Thymeleaf 서버렌더 애플리케이션.
 * 스키마는 api 모듈의 Flyway가 관리하므로 admin은 ddl-auto=none.
 */
@SpringBootApplication(scanBasePackages = "com.thaiuniontour")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
