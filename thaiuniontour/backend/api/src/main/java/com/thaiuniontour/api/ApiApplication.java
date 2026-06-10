package com.thaiuniontour.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 고객 대면 REST API + (MVP) Outbox Relayer + 결제 Consumer 호스트.
 * 컴포넌트 스캔: api + domain + common.
 */
@SpringBootApplication(scanBasePackages = "com.thaiuniontour")
@EnableScheduling
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
