package com.flasharena.order.application;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * runId 별 인메모리 로그 버퍼 레지스트리 (스레드 안전).
 * RAM 보호를 위해 run 당 최근 {@value #MAX_LINES} 줄만 보관한다.
 * Phase 5 의 SSE 엔드포인트가 여기서 로그를 읽어 스트리밍한다 (지금은 적재만).
 */
@Component
public class SimulationLogRegistry {

    static final int MAX_LINES = 500;

    private final Map<String, ConcurrentLinkedDeque<String>> buffers = new ConcurrentHashMap<>();
    private final SimulationStreamHub streamHub;

    public SimulationLogRegistry(SimulationStreamHub streamHub) {
        this.streamHub = streamHub;
    }

    /**
     * runId 버퍼에 한 줄 추가. 최대 줄 수 초과 시 오래된 줄부터 버린다.
     * 적재 후 SSE 허브에도 즉시 push 해 버퍼(리플레이) + 라이브 구독자 모두에게 전달한다.
     */
    public void append(String runId, String line) {
        ConcurrentLinkedDeque<String> buffer = buffers.computeIfAbsent(runId, k -> new ConcurrentLinkedDeque<>());
        buffer.addLast(line);
        while (buffer.size() > MAX_LINES) {
            buffer.pollFirst();
        }
        streamHub.pushLog(runId, line);
    }

    /** runId 버퍼의 현재 스냅샷. (Phase 5 SSE 초기 적재용) */
    public List<String> snapshot(String runId) {
        ConcurrentLinkedDeque<String> buffer = buffers.get(runId);
        return buffer == null ? List.of() : List.copyOf(buffer);
    }
}
