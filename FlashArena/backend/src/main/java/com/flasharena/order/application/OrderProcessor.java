package com.flasharena.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flasharena.order.domain.OrderEntity;
import com.flasharena.order.domain.OutboxEvent;
import com.flasharena.order.domain.Product;
import com.flasharena.order.infrastructure.OrderRepository;
import com.flasharena.order.infrastructure.OutboxRepository;
import com.flasharena.order.infrastructure.ProductRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워커 1건당 "1개 구매 시도" 를 자체 트랜잭션으로 처리하는 빈.
 * SimulationService 가 직접 호출하면 self-invocation 이라 프록시 트랜잭션이 안 걸리므로
 * 별도 빈으로 분리해 각 워커 호출마다 REQUIRES_NEW 트랜잭션이 시작되게 한다.
 * <p>구매 성공 시 orders INSERT 와 outbox INSERT 를 같은 트랜잭션에서 원자적으로 커밋한다
 * (Transactional Outbox). Redis 는 이 트랜잭션 안에서 절대 건드리지 않는다(dual-write 회피).
 */
@Component
public class OrderProcessor {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderProcessor(ProductRepository productRepository,
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 매 run 전 재고를 initialStock 으로 리셋하고 기존 주문을 모두 지운다 (재현 가능성 확보).
     * 자체 트랜잭션으로 동작해야 하므로 별도 빈 메서드로 둔다.
     *
     * @return 시뮬레이션 대상 단일 시드 상품의 id
     */
    @Transactional
    public UUID resetForRun(int initialStock) {
        Product product = productRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new IllegalStateException("시드 상품이 없습니다. \"order\".product 를 확인하세요."));
        UUID productId = product.getId();
        orderRepository.deleteByProductId(productId);
        product.resetQuantity(initialStock);
        productRepository.save(product);
        return productId;
    }

    /** 현재 재고 조회 (run 종료 후 최종 재고 확인용). */
    @Transactional(readOnly = true)
    public int currentStock(UUID productId) {
        return productRepository.findById(productId).orElseThrow().getQuantity();
    }

    /**
     * SYNC(No-Lock) 모드 구매 시도. 락도, DB 원자 차감도, {@code WHERE quantity>0} 가드도 없다.
     * 교과서적 read-modify-write lost-update: 읽고 → (경쟁 창을 살짝 벌린 뒤) → 차감 후 저장.
     * 여러 워커가 동시에 같은 quantity 를 읽으므로 oversell/음수 재고가 재현된다.
     *
     * @return 구매 성공(CREATED) 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean attemptPurchaseNoLock(UUID productId, UUID userId) {
        Product product = productRepository.findById(productId).orElseThrow();
        int current = product.getQuantity();

        // 읽기와 쓰기 사이의 경쟁 창을 의도적으로 벌려 lost-update 를 안정적으로 노출시킨다.
        Thread.yield();

        if (current > 0) {
            return succeed(product, userId);
        }
        fail(productId, userId);
        return false;
    }

    /**
     * REDIS_LOCK 모드의 임계영역 본문. 호출부(SimulationService)가 분산 락을 잡은 상태에서만 부른다.
     * 락이 직렬화를 보장하므로 단순 read → 차감 → 저장으로도 정확하다.
     *
     * @return 구매 성공(CREATED) 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean attemptPurchaseLocked(UUID productId, UUID userId) {
        Product product = productRepository.findById(productId).orElseThrow();

        if (product.getQuantity() > 0) {
            return succeed(product, userId);
        }
        fail(productId, userId);
        return false;
    }

    /**
     * REDIS_COUNTER 모드 정산. 게이트(Redis DECR)는 호출부(SimulationService)에서 이미 판정했다.
     * 락도 대기도 없이, 당첨(won)이면 원자 차감 + 판매 기록, 낙첨이면 FAILED 만 남긴다.
     *
     * @param won Redis DECR 결과가 0 이상(=재고 한 자리 확보)인지
     * @return 구매 성공(CREATED) 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean settleCounter(UUID productId, UUID userId, boolean won) {
        if (!won) {
            fail(productId, userId);
            return false;
        }
        // 가격만 읽고(불변), 수량은 원자 UPDATE 로 줄인다 → 동시 당첨자끼리도 lost-update 없음.
        long unitPrice = productRepository.findById(productId).orElseThrow().getPrice();
        productRepository.decreaseQuantityAtomic(productId);
        recordSale(productId, userId, unitPrice);
        return true;
    }

    /**
     * 구매 성공 처리: 재고 차감 + CREATED 주문 INSERT + ORDER_COMPLETED 아웃박스 INSERT.
     * 세 작업이 같은 트랜잭션에서 원자적으로 커밋된다 → 주문은 있는데 이벤트가 없는 상태가 불가능.
     */
    private boolean succeed(Product product, UUID userId) {
        product.decrease();
        productRepository.save(product);
        recordSale(product.getId(), userId, product.getPrice());
        return true;
    }

    /** CREATED 주문 INSERT + ORDER_COMPLETED 아웃박스 INSERT (같은 트랜잭션). 재고 차감은 호출부 책임. */
    private void recordSale(UUID productId, UUID userId, long unitPrice) {
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .userId(userId)
                .productId(productId)
                .quantity(1)
                .status("CREATED")
                .build());

        long amount = unitPrice * order.getQuantity();
        String payload = buildPayload(order.getId(), userId, productId, order.getQuantity(), amount);
        outboxRepository.save(OutboxEvent.orderCompleted(order.getId(), payload));
    }

    /** 재고 부족 실패: FAILED 주문만 기록 (아웃박스 이벤트는 발행하지 않는다). */
    private void fail(UUID productId, UUID userId) {
        orderRepository.save(OrderEntity.builder()
                .userId(userId)
                .productId(productId)
                .quantity(1)
                .status("FAILED")
                .build());
    }

    /** ORDER_COMPLETED 이벤트 페이로드(JSON) 생성. */
    private String buildPayload(UUID orderId, UUID userId, UUID productId, int quantity, long amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("userId", userId.toString());
        payload.put("productId", productId.toString());
        payload.put("quantity", quantity);
        payload.put("amount", amount);
        payload.put("occurredAt", OffsetDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // 단순 Map 직렬화라 사실상 발생하지 않지만, 트랜잭션을 롤백시켜 주문/이벤트 원자성을 지킨다.
            throw new IllegalStateException("아웃박스 페이로드 직렬화 실패 orderId=" + orderId, e);
        }
    }
}
