package com.checkstock.service;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.entity.Product;
import com.checkstock.entity.SizeChange;
import com.checkstock.entity.SizeSnapshot;
import com.checkstock.repository.SizeChangeRepository;
import com.checkstock.repository.SizeSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final SizeSnapshotRepository snapshotRepository;
    private final SizeChangeRepository changeRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper;

    /**
     * 사이즈별 연속 상태 카운트 추적
     * Key: "productId|label:group" → 연속 횟수 (양수=available, 음수=soldOut)
     */
    private final Map<String, Integer> consecutiveCountMap = new ConcurrentHashMap<>();

    /** 알림을 보내기 위한 최소 연속 확인 횟수 */
    private static final int CONFIRM_THRESHOLD = 3;

    @Transactional
    public void process(Product product, List<SizeOptionDto> newSizes) {
        int availableCount = (int) newSizes.stream().filter(SizeOptionDto::isAvailable).count();
        int totalCount = newSizes.size();

        // 최신 스냅샷 조회
        Optional<SizeSnapshot> latestOpt = snapshotRepository
                .findTopByProductIdOrderByCheckedAtDesc(product.getId());

        // 스냅샷 저장
        SizeSnapshot snapshot = new SizeSnapshot();
        snapshot.setProductId(product.getId());
        snapshot.setSizes(toJson(newSizes));
        snapshot.setAvailableCount(availableCount);
        snapshot.setTotalCount(totalCount);
        snapshot.setCheckedAt(LocalDateTime.now());
        snapshotRepository.save(snapshot);

        // 이전 스냅샷과 비교
        if (latestOpt.isPresent()) {
            List<SizeOptionDto> oldSizes = fromJson(latestOpt.get().getSizes());
            int oldAvailable = latestOpt.get().getAvailableCount();

            List<SizeOptionDto> becameAvailable = new ArrayList<>();
            List<SizeOptionDto> becameSoldOut = new ArrayList<>();

            diffSizes(oldSizes, newSizes, becameAvailable, becameSoldOut);

            // 연속 확인 카운트 업데이트 및 필터링
            List<SizeOptionDto> confirmedAvailable = updateAndFilter(product.getId(), becameAvailable, true);
            List<SizeOptionDto> confirmedSoldOut = updateAndFilter(product.getId(), becameSoldOut, false);

            // 변동이 없는 사이즈는 카운트 리셋
            resetUnchangedSizes(product.getId(), newSizes, becameAvailable, becameSoldOut);

            if (!confirmedAvailable.isEmpty() || !confirmedSoldOut.isEmpty()) {
                // 변동 기록 저장
                SizeChange change = new SizeChange();
                change.setProductId(product.getId());
                change.setBecameAvailable(toJson(confirmedAvailable));
                change.setBecameSoldOut(toJson(confirmedSoldOut));
                change.setOldAvailableCount(oldAvailable);
                change.setNewAvailableCount(availableCount);
                change.setDetectedAt(LocalDateTime.now());
                changeRepository.save(change);

                log.info("[{}] 사이즈 변동 확정! ({}회 연속 확인) 구매가능: {} → {}",
                        product.getName(), CONFIRM_THRESHOLD, oldAvailable, availableCount);

                // alertSizeFilter가 있으면 해당 사이즈만 알림
                List<SizeOptionDto> alertAvailable = filterByAlertSize(product, confirmedAvailable);
                List<SizeOptionDto> alertSoldOut = filterByAlertSize(product, confirmedSoldOut);

                // 텔레그램 알림 (필터 후 변동이 있을 때만)
                if (!alertAvailable.isEmpty() || !alertSoldOut.isEmpty()) {
                    telegramService.sendChangeAlert(product, alertAvailable, alertSoldOut, oldAvailable, availableCount, totalCount);
                }

                // 알림 보낸 사이즈는 카운트 리셋
                for (SizeOptionDto s : confirmedAvailable) {
                    consecutiveCountMap.remove(buildKey(product.getId(), s));
                }
                for (SizeOptionDto s : confirmedSoldOut) {
                    consecutiveCountMap.remove(buildKey(product.getId(), s));
                }
            }
        } else {
            log.info("[{}] 첫 스냅샷 저장. 사이즈 {}개, 구매가능 {}개", product.getName(), totalCount, availableCount);
        }
    }

    /**
     * 변동 감지된 사이즈의 연속 카운트를 업데이트하고, 임계치에 도달한 것만 반환
     */
    private List<SizeOptionDto> updateAndFilter(String productId, List<SizeOptionDto> changed, boolean isAvailable) {
        List<SizeOptionDto> confirmed = new ArrayList<>();
        for (SizeOptionDto size : changed) {
            String key = buildKey(productId, size);
            int current = consecutiveCountMap.getOrDefault(key, 0);

            if (isAvailable) {
                // available 상태는 양수로 카운트
                current = current > 0 ? current + 1 : 1;
            } else {
                // soldOut 상태는 음수로 카운트
                current = current < 0 ? current - 1 : -1;
            }
            consecutiveCountMap.put(key, current);

            int absCount = Math.abs(current);
            if (absCount >= CONFIRM_THRESHOLD) {
                confirmed.add(size);
                log.info("[{}] {}({}) {}회 연속 {} 확인 → 알림 발송",
                        productId, size.getLabel(), size.getGroup(), absCount,
                        isAvailable ? "구매가능" : "품절");
            } else {
                log.info("[{}] {}({}) {}회 연속 {} (임계치 {}회 미달, 알림 보류)",
                        productId, size.getLabel(), size.getGroup(), absCount,
                        isAvailable ? "구매가능" : "품절", CONFIRM_THRESHOLD);
            }
        }
        return confirmed;
    }

    /**
     * 변동이 없는(상태 유지) 사이즈의 카운트를 리셋
     */
    private void resetUnchangedSizes(String productId, List<SizeOptionDto> newSizes,
                                      List<SizeOptionDto> becameAvailable, List<SizeOptionDto> becameSoldOut) {
        // 변동 감지된 사이즈 키 목록
        var changedKeys = new java.util.HashSet<String>();
        for (SizeOptionDto s : becameAvailable) changedKeys.add(buildKey(productId, s));
        for (SizeOptionDto s : becameSoldOut) changedKeys.add(buildKey(productId, s));

        // 변동이 없는 사이즈는 카운트 리셋 (상태가 안정됨)
        for (SizeOptionDto s : newSizes) {
            String key = buildKey(productId, s);
            if (!changedKeys.contains(key)) {
                consecutiveCountMap.remove(key);
            }
        }
    }

    /**
     * alertSizeFilter가 설정된 경우, 해당 사이즈(라벨)만 필터링
     */
    private List<SizeOptionDto> filterByAlertSize(Product product, List<SizeOptionDto> sizes) {
        String filter = product.getAlertSizeFilter();
        if (filter == null || filter.isBlank()) {
            return sizes; // 필터 없으면 전체
        }
        Set<String> allowedLabels = Arrays.stream(filter.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return sizes.stream()
                .filter(s -> allowedLabels.contains(s.getLabel().toUpperCase()))
                .collect(Collectors.toList());
    }

    private String buildKey(String productId, SizeOptionDto size) {
        return productId + "|" + size.getLabel() + ":" + (size.getGroup() != null ? size.getGroup() : "");
    }

    private void diffSizes(List<SizeOptionDto> oldSizes, List<SizeOptionDto> newSizes,
                           List<SizeOptionDto> becameAvailable, List<SizeOptionDto> becameSoldOut) {
        // 키: "label:group" → available 매핑
        Map<String, Boolean> oldMap = oldSizes.stream()
                .collect(Collectors.toMap(
                        s -> s.getLabel() + ":" + (s.getGroup() != null ? s.getGroup() : ""),
                        SizeOptionDto::isAvailable,
                        (a, b) -> b // 중복 시 마지막 값
                ));

        for (SizeOptionDto newSize : newSizes) {
            String key = newSize.getLabel() + ":" + (newSize.getGroup() != null ? newSize.getGroup() : "");
            Boolean wasAvailable = oldMap.get(key);

            if (wasAvailable != null) {
                if (!wasAvailable && newSize.isAvailable()) {
                    becameAvailable.add(newSize);
                } else if (wasAvailable && !newSize.isAvailable()) {
                    becameSoldOut.add(newSize);
                }
            } else if (newSize.isAvailable()) {
                // 새로 등장한 사이즈가 구매가능이면
                becameAvailable.add(newSize);
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    private List<SizeOptionDto> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", json);
            return List.of();
        }
    }
}
