package com.checkstockbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckstockBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CheckstockBackendApplication.class, args);
    }
}
