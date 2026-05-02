package com.checkstockbackend.service;

import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.entity.MonitoredSite;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCrawlerService {

    private final ObjectMapper objectMapper;

    @Value("${checkstock.crawler.python:python3}")
    private String pythonCmd;

    @Value("${checkstock.crawler.script:crawler/crawl_listing.py}")
    private String scriptPath;

    @Value("${checkstock.crawler.timeout-sec:180}")
    private int timeoutSec;

    @Value("${checkstock.crawler.lock-wait-sec:300}")
    private int lockWaitSec;

    /** Chrome 프로필 동시 접근 방지: 한 번에 하나의 Python만 실행 */
    private final ReentrantLock crawlLock = new ReentrantLock(true);

    public List<ProductDto> crawl(MonitoredSite site) throws IOException {
        boolean acquired = false;
        try {
            acquired = crawlLock.tryLock(lockWaitSec, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IOException("크롤러 사용 중 (다른 요청 처리 중). 잠시 후 다시 시도해주세요.");
            }
            return runPython(site);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("크롤러 대기 중 인터럽트", e);
        } finally {
            if (acquired) crawlLock.unlock();
        }
    }

    private List<ProductDto> runPython(MonitoredSite site) throws IOException {
        Path absoluteScript = Paths.get(scriptPath).toAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                pythonCmd,
                absoluteScript.toString(),
                site.getUrl(),
                site.getListSelector(),
                site.getBaseUrl()
        );
        pb.redirectErrorStream(false);

        log.info("[{}] Python 크롤러 실행: {}", site.getName(), absoluteScript);
        Process process = pb.start();

        try {
            boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Python 크롤러 타임아웃 (" + timeoutSec + "s)");
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!stderr.isBlank()) {
                for (String line : stderr.split("\\r?\\n")) {
                    if (!line.isBlank()) {
                        log.info("[{}] [py] {}", site.getName(), line);
                    }
                }
            }

            int exit = process.exitValue();
            if (exit != 0) {
                throw new IOException("Python 크롤러 비정상 종료. exit=" + exit + ", stdout=" + stdout);
            }

            JsonNode root;
            try {
                root = objectMapper.readTree(stdout.trim());
            } catch (Exception e) {
                throw new IOException("Python 출력 JSON 파싱 실패. stdout=" + stdout, e);
            }

            if (!root.path("success").asBoolean(false)) {
                throw new IOException("크롤러 실패: " + root.path("error").asText("unknown"));
            }

            List<ProductDto> products = new ArrayList<>();
            for (JsonNode node : root.path("products")) {
                String name = node.path("name").asText();
                String url = node.path("url").asText();
                if (name.isBlank()) continue;
                products.add(new ProductDto(name, url));
            }

            log.info("[{}] 크롤링 완료: {}개 상품", site.getName(), products.size());
            return products;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("크롤러 인터럽트", e);
        }
    }
}
