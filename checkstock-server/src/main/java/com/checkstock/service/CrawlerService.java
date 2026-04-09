package com.checkstock.service;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.entity.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final ObjectMapper objectMapper;

    @Value("${checkstock.crawler.python-path:python3}")
    private String pythonPath;

    public List<SizeOptionDto> crawl(Product product) {
        try {
            // crawler/crawl.py의 절대 경로 계산
            Path scriptPath = Paths.get(System.getProperty("user.dir"), "crawler", "crawl.py");

            ProcessBuilder pb;
            if (product.getTargetSize() != null && !product.getTargetSize().isEmpty()) {
                pb = new ProcessBuilder(
                        pythonPath,
                        scriptPath.toString(),
                        product.getUrl(),
                        product.getSizeSelector(),
                        product.getSoldOutIndicator(),
                        product.getTargetSize()
                );
            } else {
                pb = new ProcessBuilder(
                        pythonPath,
                        scriptPath.toString(),
                        product.getUrl(),
                        product.getSizeSelector(),
                        product.getSoldOutIndicator()
                );
            }
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // stdout 읽기
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            // stderr 읽기 (로깅용)
            String errorOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("크롤링 타임아웃 (60초)");
            }

            if (!errorOutput.isEmpty()) {
                log.debug("Crawler stderr: {}", errorOutput);
            }

            // JSON 파싱
            JsonNode root = objectMapper.readTree(output);

            if (!root.has("success") || !root.get("success").asBoolean()) {
                String error = root.has("error") ? root.get("error").asText() : "Unknown error";
                throw new RuntimeException("크롤링 실패: " + error);
            }

            String title = root.has("title") ? root.get("title").asText() : "";
            log.info("페이지 타이틀: {}", title);

            return objectMapper.readValue(
                    root.get("sizes").toString(),
                    new TypeReference<>() {}
            );

        } catch (Exception e) {
            log.error("크롤링 실패 [{}]: {}", product.getName(), e.getMessage());
            throw new RuntimeException("크롤링 실패: " + e.getMessage(), e);
        }
    }
}
