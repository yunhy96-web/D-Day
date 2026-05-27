package com.flasharena.order.application;

import com.flasharena.order.domain.SimulationMode;
import com.flasharena.order.infrastructure.OutboxRepository;
import com.flasharena.order.presentation.dto.SimulationRequest;
import com.flasharena.order.presentation.dto.SimulationResult;
import com.flasharena.payment.application.PaymentResetService;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 동시성 시뮬레이터 엔진.
 * <p>매 run 전 재고/주문을 리셋(자체 트랜잭션)한 뒤, concurrency 개의 "1개 구매 시도" 태스크를
 * RAM-1GB 제약상 최대 50 스레드 풀에 밀어넣어 Race Condition 을 강제한다.
 * CountDownLatch 시작 게이트로 가능한 한 동시에 출발시켜 경합을 극대화한다.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    // RAM 1GB 제약 → 스레드 풀은 50 으로 고정. 그래도 concurrency 개 태스크를 통과시켜 경합을 만든다.
    private static final int MAX_POOL_SIZE = 50;
    private static final String LOCK_KEY_PREFIX = "lock:product:";
    // REDIS_COUNTER 모드의 게이트키핑 카운터 키. 매 run 전 초기 재고로 적재하고 DECR 로 자리를 나눠 갖는다.
    private static final String STOCK_KEY_PREFIX = "stock:product:";
    private static final long LOCK_WAIT_SECONDS = 10L;   // 경합 시 spurious 실패 대신 줄서서 직렬화되도록 넉넉히
    private static final long LOCK_LEASE_SECONDS = 5L;    // 데드락 방지용 자동 해제
    // 버퍼 폭주 방지를 위한 로그 샘플링 간격.
    private static final int LOG_SAMPLE_INTERVAL = 50;
    // SSE 가 끊겨도 폴링으로 결과를 받을 수 있도록 최근 결과를 보관 (RAM 보호용 상한).
    private static final int MAX_STORED_RESULTS = 100;

    private final OrderProcessor orderProcessor;
    private final RedissonClient redissonClient;
    private final SimulationLogger logger;
    private final SimulationStreamHub streamHub;
    private final OutboxRepository outboxRepository;
    private final PaymentResetService paymentResetService;
    private final StringRedisTemplate redisTemplate;
    private final String streamKey;

    // run 오케스트레이션 전용 executor (워커 50-풀과 별개). 각 run 1 스레드를 점유한 채 50-풀을 굴린다.
    private final ExecutorService orchestrator = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sim-orchestrator");
        t.setDaemon(true);
        return t;
    });

    // runId → 결과. 삽입 순서 LRU 로 MAX_STORED_RESULTS 만 보관.
    private final Map<String, SimulationResult> results = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SimulationResult> eldest) {
            return size() > MAX_STORED_RESULTS;
        }
    };

    public SimulationService(OrderProcessor orderProcessor,
            RedissonClient redissonClient,
            SimulationLogger logger,
            SimulationStreamHub streamHub,
            OutboxRepository outboxRepository,
            PaymentResetService paymentResetService,
            StringRedisTemplate redisTemplate,
            @Value("${app.stream.key:flasharena:order-events}") String streamKey) {
        this.orderProcessor = orderProcessor;
        this.redissonClient = redissonClient;
        this.logger = logger;
        this.streamHub = streamHub;
        this.outboxRepository = outboxRepository;
        this.paymentResetService = paymentResetService;
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    /**
     * 비동기 실행 진입점. runId 를 미리 만들어 즉시 반환하고, 실제 시뮬레이션은 오케스트레이터 스레드에서 돌린다.
     * 클라이언트는 반환된 runId 로 곧바로 SSE 를 구독할 수 있다.
     * userId 는 요청 스레드에서 UserContext 로부터 미리 추출해 넘긴다 (풀/오케스트레이터 스레드엔 ThreadLocal 없음).
     */
    public String startAsync(SimulationRequest request, UUID userId) {
        String runId = UUID.randomUUID().toString();
        orchestrator.submit(() -> {
            try {
                SimulationResult result = run(runId, request, userId);
                storeResult(runId, result);
                streamHub.pushResult(runId, result);
            } catch (RuntimeException e) {
                log.error("[sim {}] 시뮬레이션 실행 중 오류", runId, e);
            }
        });
        return runId;
    }

    /** 저장된 결과 조회 (SSE 가 끊긴 경우 폴링 폴백). 없으면 null. */
    public SimulationResult findResult(String runId) {
        synchronized (results) {
            return results.get(runId);
        }
    }

    private void storeResult(String runId, SimulationResult result) {
        synchronized (results) {
            results.put(runId, result);
        }
    }

    @PreDestroy
    void shutdown() {
        orchestrator.shutdownNow();
    }

    /** 외부에서 만든 runId 로 동기 실행. (오케스트레이터 스레드에서 호출됨) */
    public SimulationResult run(String runId, SimulationRequest request, UUID userId) {
        int concurrency = request.concurrencyOrDefault();
        int initialStock = request.initialStockOrDefault();
        SimulationMode mode = request.mode();

        UUID productId = orderProcessor.resetForRun(initialStock);
        resetMessagingResidue();
        if (mode == SimulationMode.REDIS_COUNTER) {
            // 게이트키핑 카운터를 초기 재고로 적재. 이후 DECR 한 번 = '한 자리 차지'.
            redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productId, Integer.toString(initialStock));
        }
        logger.summary(runId, String.format(
                "🚀 시뮬레이션 시작 mode=%s 동시요청=%d 초기재고=%d", mode, concurrency, initialStock));

        OffsetDateTime startedAt = OffsetDateTime.now();
        long startNanos = System.nanoTime();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(MAX_POOL_SIZE);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(concurrency);
        try {
            for (int i = 0; i < concurrency; i++) {
                final int seq = i;
                futures.add(pool.submit(() -> {
                    awaitGate(startGate);
                    boolean ok = switch (mode) {
                        case SYNC -> buyNoLock(runId, productId, userId, seq);
                        case REDIS_LOCK -> buyWithRedisLock(runId, productId, userId, seq);
                        case REDIS_COUNTER -> buyWithCounter(runId, productId, userId, seq);
                    };
                    (ok ? successCount : failCount).incrementAndGet();
                }));
            }
            // 게이트 오픈 — 모든 태스크를 거의 동시에 출발시켜 경합을 극대화한다.
            startGate.countDown();
            joinAll(futures);
        } finally {
            pool.shutdownNow();
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        OffsetDateTime finishedAt = OffsetDateTime.now();

        int finalStock = orderProcessor.currentStock(productId);
        int success = successCount.get();
        int fail = failCount.get();
        int expectedStock = Math.max(0, initialStock - success);
        boolean oversold = success > initialStock || finalStock < 0 || finalStock != initialStock - success;

        if (oversold) {
            logger.oversell(runId, String.format(
                    "⚠️ 오버셀 감지: 성공=%d > 초기재고=%d, 최종재고=%d (기대 %d)",
                    success, initialStock, finalStock, initialStock - success));
        }
        logger.summary(runId, String.format(
                "🏁 종료 성공=%d 실패=%d 최종재고=%d oversold=%b (%dms)",
                success, fail, finalStock, oversold, elapsedMs));

        return new SimulationResult(runId, mode, concurrency, initialStock,
                success, fail, finalStock, expectedStock, oversold, elapsedMs, startedAt, finishedAt);
    }

    /**
     * 매 run 전 메시징 잔여물을 정리해 재현 가능성을 확보한다.
     * ⚠️ 이들은 서로 다른 스키마/저장소에 대한 "별개의" 작업이다 — 단일 교차 스키마 트랜잭션이 아니다.
     *   - outbox 행 삭제(order 스키마, 자체 트랜잭션)
     *   - payment_history 행 삭제(payment 스키마, 자체 트랜잭션)
     *   - 스트림은 XTRIM MAXLEN 0 으로 비운다(키를 지우지 않아 소비자 그룹은 유지).
     */
    private void resetMessagingResidue() {
        outboxRepository.deleteAllOutbox();
        paymentResetService.resetHistory();
        try {
            // 스트림 키를 삭제하면 소비자 그룹까지 사라지므로, 길이만 0 으로 잘라 잔여 메시지를 제거한다.
            redisTemplate.opsForStream().trim(streamKey, 0);
        } catch (RuntimeException e) {
            // 첫 run 등 스트림이 아직 없을 수 있음 — 무시.
        }
    }

    /** SYNC: 락 없이 lost-update 버그를 그대로 노출. */
    private boolean buyNoLock(String runId, UUID productId, UUID userId, int seq) {
        boolean ok = orderProcessor.attemptPurchaseNoLock(productId, userId);
        sample(runId, ok, seq);
        return ok;
    }

    /** REDIS_LOCK: 상품 키 분산 락을 잡아 직렬화한 뒤 임계영역을 실행. */
    private boolean buyWithRedisLock(String runId, UUID productId, UUID userId, int seq) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + productId);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                logger.lockFail(runId, String.format("🔒 락 획득 실패 seq=%d (대기 %ds 초과)", seq, LOCK_WAIT_SECONDS));
                return false;
            }
            boolean ok = orderProcessor.attemptPurchaseLocked(productId, userId);
            sample(runId, ok, seq);
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * REDIS_COUNTER: Redis 원자 DECR 로 게이트키핑.
     * 락도 대기도 없이 DECR 한 번으로 당첨/낙첨이 즉시 갈린다(잔여>=0 이면 당첨). 인메모리 연산이라 빠르다.
     * DB 에는 당첨자만 들어가고, 수량 차감도 원자 UPDATE 라 동시 당첨자끼리 lost-update 가 없다.
     */
    private boolean buyWithCounter(String runId, UUID productId, UUID userId, int seq) {
        Long remaining = redisTemplate.opsForValue().decrement(STOCK_KEY_PREFIX + productId);
        boolean won = remaining != null && remaining >= 0;
        boolean ok = orderProcessor.settleCounter(productId, userId, won);
        sample(runId, ok, seq);
        return ok;
    }

    /** 주요 이벤트만 샘플링해 버퍼에 적재 (concurrency 가 커도 폭주하지 않도록). */
    private void sample(String runId, boolean ok, int seq) {
        if (seq % LOG_SAMPLE_INTERVAL != 0) {
            return;
        }
        if (ok) {
            logger.success(runId, String.format("✅ 구매 성공 seq=%d", seq));
        } else {
            logger.outOfStock(runId, String.format("❌ 재고 부족 실패 seq=%d", seq));
        }
    }

    private void awaitGate(CountDownLatch gate) {
        try {
            gate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void joinAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                // 개별 태스크 예외는 성공/실패 집계에 영향 없음 (실패로 카운트되지 않은 건 무시).
            }
        }
    }
}
