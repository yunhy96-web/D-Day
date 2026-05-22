package com.flasharena.order.presentation;

import com.flasharena.global.context.UserContext;
import com.flasharena.global.jwt.JwtProvider;
import com.flasharena.order.application.SimulationLogRegistry;
import com.flasharena.order.application.SimulationService;
import com.flasharena.order.application.SimulationStreamHub;
import com.flasharena.order.presentation.dto.SimulationRequest;
import com.flasharena.order.presentation.dto.SimulationResult;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 동시성 시뮬레이터 API.
 * <p>{@code /run} 과 {@code /result/{runId}} 는 AuthInterceptor(Bearer) 로 보호된다.
 * {@code /stream/{runId}} 는 EventSource 가 Authorization 헤더를 못 보내므로 인터셉터에서 제외하고
 * {@code ?token=<jwt>} 쿼리 파라미터로 직접 검증한다.
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulationService simulationService;
    private final SimulationStreamHub streamHub;
    private final SimulationLogRegistry logRegistry;
    private final JwtProvider jwtProvider;

    public SimulatorController(SimulationService simulationService,
            SimulationStreamHub streamHub,
            SimulationLogRegistry logRegistry,
            JwtProvider jwtProvider) {
        this.simulationService = simulationService;
        this.streamHub = streamHub;
        this.logRegistry = logRegistry;
        this.jwtProvider = jwtProvider;
    }

    /**
     * 시뮬레이션 실행 (비동기).
     * ⚠️ UserContext 는 ThreadLocal 이라 워커/오케스트레이터 스레드엔 전파되지 않는다.
     * 따라서 요청 스레드에서 미리 userId 를 추출해 서비스로 명시 전달한다.
     * 즉시 202 + runId 를 반환하고, 클라이언트는 그 runId 로 SSE 를 구독한다.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@Valid @RequestBody SimulationRequest request) {
        UUID userId = UserContext.getUserId();
        String runId = simulationService.startAsync(request, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "runId", runId,
                "mode", request.mode().name(),
                "concurrency", request.concurrencyOrDefault(),
                "initialStock", request.initialStockOrDefault()));
    }

    /**
     * SSE 스트림. 먼저 현재 버퍼 스냅샷을 {@code log} 이벤트로 리플레이한 뒤 라이브로 흘리고,
     * run 종료 시 {@code result} 이벤트를 보내고 완료한다.
     * EventSource 는 헤더를 못 보내므로 {@code ?token} 으로 인증한다 (인터셉터 제외 경로).
     */
    @GetMapping(value = "/stream/{runId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@PathVariable String runId,
            @RequestParam(name = "token", required = false) String token) {
        if (token == null || !isValidToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SseEmitter emitter = streamHub.subscribe(runId);
        replaySnapshot(runId, emitter);
        // run 이 구독 전에 이미 끝났다면(빠른 run) 저장된 결과를 바로 보내고 닫는다 — result 이벤트 유실 방지.
        SimulationResult finished = simulationService.findResult(runId);
        if (finished != null) {
            streamHub.pushResult(runId, finished);
        }
        return ResponseEntity.ok(emitter);
    }

    /** 저장된 결과 조회 (SSE 폴백). Bearer 보호. 없으면 404. */
    @GetMapping("/result/{runId}")
    public ResponseEntity<SimulationResult> result(@PathVariable String runId) {
        SimulationResult result = simulationService.findResult(runId);
        return result == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(result);
    }

    /** 구독 직후 현재 버퍼를 log 이벤트로 리플레이 (라이브 이전에 쌓인 줄을 놓치지 않도록). */
    private void replaySnapshot(String runId, SseEmitter emitter) {
        List<String> snapshot = logRegistry.snapshot(runId);
        for (String line : snapshot) {
            try {
                emitter.send(SseEmitter.event().name("log").data(line));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
                return;
            }
        }
    }

    private boolean isValidToken(String token) {
        try {
            jwtProvider.parse(token);
            return true;
        } catch (JwtProvider.InvalidTokenException e) {
            return false;
        }
    }
}
