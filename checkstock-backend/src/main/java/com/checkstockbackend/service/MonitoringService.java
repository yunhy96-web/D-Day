package com.checkstockbackend.service;

import com.checkstockbackend.dto.MonitoringResult;
import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.entity.MonitoredSite;
import com.checkstockbackend.entity.SiteChange;
import com.checkstockbackend.entity.SiteSnapshot;
import com.checkstockbackend.repository.SiteChangeRepository;
import com.checkstockbackend.repository.SiteSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final SiteSnapshotRepository snapshotRepository;
    private final SiteChangeRepository changeRepository;
    private final KeywordFilter keywordFilter;
    private final TelegramService telegramService;
    private final ExpoPushService expoPushService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MonitoringResult process(MonitoredSite site, List<ProductDto> allProducts) {
        List<ProductDto> matched = keywordFilter.filter(
                allProducts, site.getIncludeKeywords(), site.getExcludeKeywords());
        int totalCount = allProducts.size();
        int matchedCount = matched.size();

        Optional<SiteSnapshot> prevOpt = snapshotRepository
                .findTopBySiteIdOrderByCheckedAtDesc(site.getId());

        LocalDateTime now = LocalDateTime.now();
        SiteSnapshot snapshot = new SiteSnapshot();
        snapshot.setSiteId(site.getId());
        snapshot.setTotalCount(totalCount);
        snapshot.setMatchedCount(matchedCount);
        snapshot.setMatchedProducts(toJson(matched));
        snapshot.setCheckedAt(now);
        snapshotRepository.save(snapshot);

        List<ProductDto> prevMatched = prevOpt
                .map(s -> fromJson(s.getMatchedProducts()))
                .orElse(List.of());

        Set<String> prevNames = new HashSet<>();
        for (ProductDto p : prevMatched) prevNames.add(p.getName());
        Set<String> newNames = new HashSet<>();
        for (ProductDto p : matched) newNames.add(p.getName());

        List<ProductDto> added = matched.stream().filter(p -> !prevNames.contains(p.getName())).toList();
        List<ProductDto> removed = prevMatched.stream().filter(p -> !newNames.contains(p.getName())).toList();

        if (!added.isEmpty() || !removed.isEmpty()) {
            SiteChange change = new SiteChange();
            change.setSiteId(site.getId());
            change.setAddedProducts(toJson(added));
            change.setRemovedProducts(toJson(removed));
            change.setOldMatchedCount(prevOpt.map(SiteSnapshot::getMatchedCount).orElse(0));
            change.setNewMatchedCount(matchedCount);
            change.setDetectedAt(now);
            changeRepository.save(change);

            log.info("[{}] 변동 감지! +{}개, -{}개 (총 매칭 {} → {})",
                    site.getName(), added.size(), removed.size(),
                    prevOpt.map(SiteSnapshot::getMatchedCount).orElse(0), matchedCount);
        } else {
            log.info("[{}] 변동 없음. 매칭 {}개 유지", site.getName(), matchedCount);
        }

        // 텔레그램은 새로 추가된 상품이 있을 때만 (스팸 방지)
        if (!added.isEmpty()) {
            telegramService.sendNewMatchAlert(site, added, matchedCount);
        }

        // 푸시는 매칭 상품이 있는 동안 매 사이클 발송 (수면 중 깨우기 목적)
        if (!matched.isEmpty()) {
            expoPushService.sendInStockAlert(site, matched);
        }

        return new MonitoringResult(totalCount, matchedCount, matched);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    private List<ProductDto> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", json);
            return List.of();
        }
    }
}
