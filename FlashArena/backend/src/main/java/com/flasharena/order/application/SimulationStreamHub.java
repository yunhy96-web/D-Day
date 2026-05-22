package com.flasharena.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flasharena.order.presentation.dto.SimulationResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * runId 별 {@link SseEmitter} 구독자를 관리하는 SSE 허브 (스레드 안전).
 * <p>로거가 한 줄 적재할 때마다 {@link #pushLog} 로 살아있는 구독자에게 {@code event: log} 를 흘리고,
 * run 이 끝나면 {@link #pushResult} 로 {@code event: result}(SimulationResult JSON) 를 보낸 뒤 emitter 를 완료한다.
 * 죽은 클라이언트의 IOException 은 삼키고 해당 emitter 만 제거한다.
 */
@Component
public class SimulationStreamHub {

    private static final Logger log = LoggerFactory.getLogger(SimulationStreamHub.class);

    /** SSE 타임아웃 5분. 빠른 run 도 있지만 느린 run 을 대비해 넉넉히 둔다. */
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SimulationStreamHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** runId 구독: emitter 를 등록하고 완료/타임아웃/에러 시 자동 제거한다. */
    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> remove(runId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(runId, emitter);
        });
        emitter.onError(e -> remove(runId, emitter));
        return emitter;
    }

    /** 살아있는 모든 구독자에게 {@code event: log} 한 줄을 전송. 죽은 emitter 는 제거. */
    public void pushLog(String runId, String line) {
        List<SseEmitter> list = emitters.get(runId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("log").data(line));
            } catch (IOException | IllegalStateException e) {
                remove(runId, emitter);
            }
        }
    }

    /** 결과를 {@code event: result}(JSON) 로 보내고 해당 run 의 모든 emitter 를 완료한다. */
    public void pushResult(String runId, SimulationResult result) {
        List<SseEmitter> list = emitters.remove(runId);
        if (list == null) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("[sim {}] result 직렬화 실패", runId, e);
            for (SseEmitter emitter : list) {
                emitter.completeWithError(e);
            }
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("result").data(json));
                emitter.complete();
            } catch (IOException | IllegalStateException e) {
                // 이미 끊긴 클라이언트 — 무시.
            }
        }
    }

    private void remove(String runId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(runId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(runId, list);
            }
        }
    }
}
