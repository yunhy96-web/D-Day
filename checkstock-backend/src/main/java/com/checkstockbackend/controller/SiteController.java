package com.checkstockbackend.controller;

import com.checkstockbackend.dto.CheckNowResponse;
import com.checkstockbackend.dto.KeywordUpdateRequest;
import com.checkstockbackend.dto.MonitoringResult;
import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.dto.SiteResponse;
import com.checkstockbackend.dto.SubmitProductsRequest;
import com.checkstockbackend.entity.MonitoredSite;
import com.checkstockbackend.repository.MonitoredSiteRepository;
import com.checkstockbackend.service.AppSubmitTracker;
import com.checkstockbackend.service.KeywordFilter;
import com.checkstockbackend.service.MonitoringService;
import com.checkstockbackend.service.SiteCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
@Slf4j
public class SiteController {

    private final MonitoredSiteRepository siteRepository;
    private final SiteCrawlerService crawlerService;
    private final KeywordFilter keywordFilter;
    private final MonitoringService monitoringService;
    private final AppSubmitTracker appSubmitTracker;

    @GetMapping
    public List<SiteResponse> list() {
        return siteRepository.findAll().stream()
                .map(SiteResponse::from)
                .toList();
    }

    @PutMapping("/{id}/keywords")
    @Transactional
    public ResponseEntity<SiteResponse> updateKeywords(
            @PathVariable String id,
            @RequestBody KeywordUpdateRequest req) {
        MonitoredSite site = siteRepository.findById(id).orElse(null);
        if (site == null) {
            return ResponseEntity.notFound().build();
        }

        replaceAll(site.getIncludeKeywords(), cleanList(req.getIncludeKeywords()));
        replaceAll(site.getExcludeKeywords(), cleanList(req.getExcludeKeywords()));

        log.info("[{}] 키워드 업데이트: include={}, exclude={}",
                site.getName(), site.getIncludeKeywords(), site.getExcludeKeywords());
        return ResponseEntity.ok(SiteResponse.from(site));
    }

    private List<String> cleanList(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) continue;
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) dedup.add(trimmed);
        }
        return new ArrayList<>(dedup);
    }

    private void replaceAll(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String id) {
        if (!siteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        appSubmitTracker.markSubmitted(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit-products")
    public ResponseEntity<CheckNowResponse> submitProducts(
            @PathVariable String id,
            @RequestBody SubmitProductsRequest req) {
        MonitoredSite site = siteRepository.findById(id).orElse(null);
        if (site == null) {
            return ResponseEntity.notFound().build();
        }
        List<ProductDto> products = req.getProducts() == null ? List.of() : req.getProducts();
        appSubmitTracker.markSubmitted(id);
        MonitoringResult result = monitoringService.process(site, products);
        log.info("[{}] submit-products: total={}, matched={}",
                site.getName(), result.getTotalCount(), result.getMatchedCount());
        return ResponseEntity.ok(new CheckNowResponse(
                result.getTotalCount(), result.getMatchedCount(), result.getMatched(), LocalDateTime.now()));
    }

    @PostMapping("/{id}/check-now")
    public ResponseEntity<CheckNowResponse> checkNow(
            @PathVariable String id,
            @RequestBody(required = false) KeywordUpdateRequest override) {
        MonitoredSite site = siteRepository.findById(id).orElse(null);
        if (site == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            List<ProductDto> all = crawlerService.crawl(site);
            List<String> include = override != null && override.getIncludeKeywords() != null
                    ? override.getIncludeKeywords()
                    : site.getIncludeKeywords();
            List<String> exclude = override != null && override.getExcludeKeywords() != null
                    ? override.getExcludeKeywords()
                    : site.getExcludeKeywords();
            List<ProductDto> matched = keywordFilter.filter(all, include, exclude);
            log.info("[{}] check-now: total={}, matched={} (override={})",
                    site.getName(), all.size(), matched.size(), override != null);
            return ResponseEntity.ok(new CheckNowResponse(
                    all.size(), matched.size(), matched, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("[{}] check-now 실패: {}", site.getName(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
