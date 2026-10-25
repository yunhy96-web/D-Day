package com.dday.sunfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SunfixApplication {
    public static void main(String[] args) throws IOException {
        // SQLite 는 폴더가 없으면 DB 파일을 못 만든다. Flyway 보다 먼저
        Files.createDirectories(Path.of(System.getenv().getOrDefault("APP_DATA_DIR", "./data")));
        SpringApplication.run(SunfixApplication.class, args);
    }
}
