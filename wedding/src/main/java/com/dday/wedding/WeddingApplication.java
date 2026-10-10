package com.dday.wedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class WeddingApplication {
    public static void main(String[] args) throws IOException {
        String dataDir = System.getenv().getOrDefault("APP_DATA_DIR", "./data");
        Files.createDirectories(Path.of(dataDir, "photos"));  // SQLite 파일·사진 디렉토리. DataSource 초기화 전에 보장
        SpringApplication.run(WeddingApplication.class, args);
    }
}
