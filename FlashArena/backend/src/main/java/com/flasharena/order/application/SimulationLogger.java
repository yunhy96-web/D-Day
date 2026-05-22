package com.flasharena.order.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 시뮬레이션 엔진이 호출하는 사람이 읽기 좋은 한국어 로그 emitter.
 * 인메모리 버퍼(레지스트리)에 적재하고, 주요 이벤트는 SLF4J 로도 남긴다.
 * concurrency 가 큰 경우 요청마다 INFO 를 찍으면 폭주하므로 주요 이벤트만 기록한다
 * (락 실패 / 오버셀 / 재고 부족 / 요약). 개별 성공은 버퍼에만 샘플링해 담는다.
 */
@Component
public class SimulationLogger {

    private static final Logger log = LoggerFactory.getLogger(SimulationLogger.class);

    private final SimulationLogRegistry registry;

    public SimulationLogger(SimulationLogRegistry registry) {
        this.registry = registry;
    }

    /** run 시작/종료 등 요약 라인 — 버퍼 + INFO. */
    public void summary(String runId, String line) {
        registry.append(runId, line);
        log.info("[sim {}] {}", runId, line);
    }

    /** 구매 성공 — 버퍼에만 적재(폭주 방지를 위해 호출부에서 샘플링). */
    public void success(String runId, String line) {
        registry.append(runId, line);
    }

    /** 재고 부족 실패 — 버퍼에만 적재(샘플링). */
    public void outOfStock(String runId, String line) {
        registry.append(runId, line);
    }

    /** 락 획득 실패 — 버퍼 + WARN. 정상 직렬화에선 발생하지 않아야 한다. */
    public void lockFail(String runId, String line) {
        registry.append(runId, line);
        log.warn("[sim {}] {}", runId, line);
    }

    /** 오버셀 감지 — 버퍼 + WARN. SYNC 모드 버그의 핵심 증거. */
    public void oversell(String runId, String line) {
        registry.append(runId, line);
        log.warn("[sim {}] {}", runId, line);
    }
}
