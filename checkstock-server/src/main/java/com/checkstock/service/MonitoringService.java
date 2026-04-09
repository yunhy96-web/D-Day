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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final SizeSnapshotRepository snapshotRepository;
    private final SizeChangeRepository changeRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper;

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

            if (!becameAvailable.isEmpty() || !becameSoldOut.isEmpty()) {
                // 변동 기록 저장
                SizeChange change = new SizeChange();
                change.setProductId(product.getId());
                change.setBecameAvailable(toJson(becameAvailable));
                change.setBecameSoldOut(toJson(becameSoldOut));
                change.setOldAvailableCount(oldAvailable);
                change.setNewAvailableCount(availableCount);
                change.setDetectedAt(LocalDateTime.now());
                changeRepository.save(change);

                log.info("[{}] 사이즈 변동 감지! 구매가능: {} → {}", product.getName(), oldAvailable, availableCount);

                // 텔레그램 알림
                telegramService.sendChangeAlert(product, becameAvailable, becameSoldOut, oldAvailable, availableCount, totalCount);
            }
        } else {
            log.info("[{}] 첫 스냅샷 저장. 사이즈 {}개, 구매가능 {}개", product.getName(), totalCount, availableCount);
        }
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
