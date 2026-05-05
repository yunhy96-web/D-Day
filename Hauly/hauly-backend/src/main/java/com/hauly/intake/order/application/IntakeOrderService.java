package com.hauly.intake.order.application;

import com.hauly.intake.deposit.application.DepositService;
import com.hauly.intake.order.application.command.ChangeFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ChangePaymentStatusCommand;
import com.hauly.intake.order.application.command.CreateOrderCommand;
import com.hauly.intake.order.application.command.ForceFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ForcePaymentStatusCommand;
import com.hauly.intake.order.application.query.OrderDetailView;
import com.hauly.intake.order.application.query.OrderListItemView;
import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.domain.model.Order;
import com.hauly.intake.order.domain.model.OrderType;
import com.hauly.intake.order.domain.model.OrderStatusLog;
import com.hauly.intake.order.domain.model.OrderItem;
import com.hauly.intake.order.domain.repository.OrderRepository;
import com.hauly.intake.order.domain.service.OrderNoGenerator;
import com.hauly.platform.storage.domain.BlobStorage;
import com.hauly.shared.customer.application.CustomerLookupService;
import com.hauly.shared.customer.application.command.IdentifyCustomerCommand;
import com.hauly.shared.customer.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service orchestrating the INTAKE order use cases.
 * - createOrder: identify-or-create customer, build aggregate, persist, assign order_no, flush logs.
 * - listOrders / getOrder: read models for the admin SPA.
 * - changeFulfillmentStatus / changePaymentStatus: state machine transitions with audit logging.
 */
@Service
@Transactional
public class IntakeOrderService {

    private final OrderRepository orderRepository;
    private final CustomerLookupService customerLookupService;
    private final OrderNoGenerator orderNoGenerator;
    private final BlobStorage blobStorage;
    private final DepositService depositService;

    public IntakeOrderService(OrderRepository orderRepository,
                              CustomerLookupService customerLookupService,
                              OrderNoGenerator orderNoGenerator,
                              BlobStorage blobStorage,
                              DepositService depositService) {
        this.orderRepository = orderRepository;
        this.customerLookupService = customerLookupService;
        this.orderNoGenerator = orderNoGenerator;
        this.blobStorage = blobStorage;
        this.depositService = depositService;
    }

    public OrderDetailView createOrder(CreateOrderCommand cmd, Long createdBy) {
        if (cmd.items() == null || cmd.items().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }

        // SET orders bundle items that ship together (e.g. 2+1 promo) — all items
        // must therefore share the same categoryId. INDIVIDUAL orders have no such constraint.
        OrderType orderType = cmd.orderType() == null ? OrderType.INDIVIDUAL : cmd.orderType();
        if (orderType == OrderType.SET && cmd.items().size() > 1) {
            Long firstCategory = cmd.items().get(0).categoryId();
            for (CreateOrderCommand.Item item : cmd.items()) {
                if (item.categoryId() == null || !item.categoryId().equals(firstCategory)) {
                    throw new IllegalArgumentException(
                            "set_items_must_share_category: all items in a SET order must use the same category");
                }
            }
        }

        // Validate that any temp image keys belong to the caller — prevents one user from
        // attaching another user's uploads. Keys are namespaced as `temp/{userId}/...`.
        String tempPrefix = "temp/" + createdBy + "/";
        for (CreateOrderCommand.Item item : cmd.items()) {
            if (item.tempImageKeys() == null) continue;
            for (String key : item.tempImageKeys()) {
                if (key == null || !key.startsWith(tempPrefix)) {
                    throw new IllegalArgumentException("invalid temp image key: " + key);
                }
            }
        }

        Customer customer = customerLookupService.findOrCreate(new IdentifyCustomerCommand(
                cmd.customerName(), cmd.customerLineId(), cmd.customerPhone()));

        Order order = Order.createIntake(
                customer.getId(),
                orderType,
                cmd.customerMemo(),
                cmd.internalMemo(),
                cmd.koreanTrackingNo(),
                cmd.koreanCourier(),
                createdBy);

        order.setShippingAddress(
                cmd.recipientName(),
                cmd.recipientPhone(),
                cmd.postalCode(),
                cmd.addressLine(),
                cmd.country());
        order.setShippingAddressLabel(cmd.shippingAddressLabel());

        for (CreateOrderCommand.Item item : cmd.items()) {
            order.addItem(item.productName(), item.productUrl(), item.quantity(),
                    item.categoryId(), item.attributes(),
                    item.unitPriceAmount(), item.unitPriceCurrency());
        }

        // First save → DB assigns id (both order and items)
        order = orderRepository.save(order);

        // Replace placeholder order_no with HL-yyyy-#### derived from id
        order.assignOrderNo(orderNoGenerator.generate(order.getId()));

        // Promote temp image keys → permanent. Items now have IDs.
        // Positional pairing: cmd.items().get(i) ↔ order.getItems().get(i).
        List<OrderItem> savedItems = order.getItems();
        for (int i = 0; i < cmd.items().size(); i++) {
            List<String> tempKeys = cmd.items().get(i).tempImageKeys();
            if (tempKeys == null || tempKeys.isEmpty()) continue;
            OrderItem entityItem = savedItems.get(i);
            List<String> permanentKeys = new ArrayList<>(tempKeys.size());
            for (String tempKey : tempKeys) {
                String ext = extractExt(tempKey);
                String permanentKey = "orders/" + order.getId() + "/items/" + entityItem.getId()
                        + "/" + UUID.randomUUID() + "." + ext;
                blobStorage.copy(tempKey, permanentKey);
                blobStorage.delete(tempKey);
                permanentKeys.add(permanentKey);
            }
            entityItem.setRequestImageKeys(permanentKeys);
        }
        order = orderRepository.save(order);

        flushPendingLogs(order);

        List<OrderStatusLog> history = orderRepository.findStatusLogsByOrderId(order.getId());
        return OrderDetailView.from(
                order, customer.getName(), customer.getLineId(), customer.getPhone(),
                history, blobStorage);
    }

    private static String extractExt(String key) {
        int dot = key.lastIndexOf('.');
        return (dot >= 0 && dot < key.length() - 1) ? key.substring(dot + 1) : "bin";
    }

    @Transactional(readOnly = true)
    public Page<OrderListItemView> listOrders(FulfillmentStatus filter, String q,
                                               String sort, String dir,
                                               int page, int size) {
        String safeQ = q == null ? "" : q.trim();
        Page<Order> orders;
        if ("productName".equals(sort)) {
            // Custom JPQL — productName lives on OrderItem, not Order. Direction fixed ASC.
            orders = orderRepository.findAllByFilterSortByProductName(
                    filter, safeQ, PageRequest.of(page, size));
        } else {
            Sort sortObj = Sort.by(parseDirection(dir), parseSortField(sort));
            orders = orderRepository.findAllByFilter(
                    filter, safeQ, PageRequest.of(page, size, sortObj));
        }
        return orders.map(o -> {
            Customer c = customerLookupService.getById(o.getCustomerId());
            return OrderListItemView.from(o, c.getName(), blobStorage);
        });
    }

    private static Sort.Direction parseDirection(String dir) {
        return "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /** Whitelist sort fields — anything else falls back to createdAt to prevent abuse. */
    private static String parseSortField(String sort) {
        if (sort == null) return "createdAt";
        return switch (sort) {
            case "fulfillmentStatus", "orderNo", "createdAt" -> sort;
            default -> "createdAt";
        };
    }

    @Transactional(readOnly = true)
    public OrderDetailView getOrder(Long id) {
        Order order = loadOrder(id);
        Customer customer = customerLookupService.getById(order.getCustomerId());
        List<OrderStatusLog> history = orderRepository.findStatusLogsByOrderId(order.getId());
        return OrderDetailView.from(
                order, customer.getName(), customer.getLineId(), customer.getPhone(),
                history, blobStorage);
    }

    public OrderDetailView changeFulfillmentStatus(ChangeFulfillmentStatusCommand cmd, Long actorId) {
        Order order = loadOrder(cmd.orderId());
        order.changeFulfillmentStatus(cmd.target(), actorId, cmd.note());
        if (cmd.target() == FulfillmentStatus.PURCHASED) {
            if (cmd.paidAmountKrw() != null) {
                order.recordPaidAmountKrw(cmd.paidAmountKrw());
            }
            // 결제 증빙 임시 키 → 영구 키 이동.
            List<String> proofTemp = cmd.proofTempKeys();
            if (proofTemp != null && !proofTemp.isEmpty()) {
                List<String> permanent = new ArrayList<>(proofTemp.size());
                for (String tempKey : proofTemp) {
                    String ext = extractExt(tempKey);
                    String permanentKey = "orders/" + order.getId() + "/proof/"
                            + UUID.randomUUID() + "." + ext;
                    blobStorage.copy(tempKey, permanentKey);
                    blobStorage.delete(tempKey);
                    permanent.add(permanentKey);
                }
                // 기존 키와 합쳐서 누적 (재차 PURCHASED 진입 시 추가만).
                List<String> existing = new ArrayList<>(order.getPurchaseProofKeys());
                existing.addAll(permanent);
                order.recordPurchaseProofKeys(existing);
            }
        }
        order = orderRepository.save(order);
        flushPendingLogs(order);
        applyDepositSideEffects(order.getId(), cmd.target(), cmd.paidAmountKrw(), actorId);
        return getOrder(order.getId());
    }

    /**
     * 재무 필드 일괄 업데이트 (고객입금/물류비/태국배송비/환율). PURCHASED 전이여도 입력 가능.
     */
    public OrderDetailView updateFinancials(
            Long orderId,
            java.math.BigDecimal customerRevenueAmount, String customerRevenueCurrency,
            java.math.BigDecimal logisticsKrToThAmount, String logisticsKrToThCurrency,
            java.math.BigDecimal logisticsThDomesticAmount, String logisticsThDomesticCurrency,
            java.math.BigDecimal krwPerThb) {
        Order order = loadOrder(orderId);
        order.updateFinancials(
                customerRevenueAmount, customerRevenueCurrency,
                logisticsKrToThAmount, logisticsKrToThCurrency,
                logisticsThDomesticAmount, logisticsThDomesticCurrency,
                krwPerThb);
        orderRepository.save(order);
        return getOrder(orderId);
    }

    /**
     * paidAmountKrw 직접 수정. 기존 값과의 차액만큼 디파짓 원장에 ADJUSTMENT 트랜잭션을 자동 추가.
     * 기존 PURCHASE 트랜잭션은 건드리지 않고 append-only 유지.
     *
     * <p>oldPaid가 null인 경우 (V20 디파짓 도입 전 등록된 옛 주문 또는 데이터 마이그레이션):
     * PURCHASE 트랜잭션을 신규 작성하여 정상 상태로 복구.
     */
    public OrderDetailView updatePaidAmount(Long orderId, java.math.BigDecimal newPaidAmount, Long actorId) {
        Order order = loadOrder(orderId);
        java.math.BigDecimal oldPaid = order.getPaidAmountKrw();
        order.overridePaidAmountKrw(newPaidAmount);
        orderRepository.save(order);
        if (oldPaid == null) {
            depositService.recordPurchase(orderId, newPaidAmount, actorId);
        } else if (oldPaid.compareTo(newPaidAmount) != 0) {
            String note = "Order " + order.getOrderNo()
                    + ": paidAmountKrw " + oldPaid.toPlainString() + " → " + newPaidAmount.toPlainString();
            depositService.recordPaidAmountAdjustment(orderId, oldPaid, newPaidAmount, note, actorId);
        }
        return getOrder(orderId);
    }

    /** 트래킹 정보 후속 입력/수정. PURCHASED 이후에도 호출 가능. */
    public OrderDetailView updateTracking(Long orderId, String courier, String trackingNo) {
        Order order = loadOrder(orderId);
        order.updateTracking(courier, trackingNo);
        orderRepository.save(order);
        return getOrder(orderId);
    }

    /**
     * PURCHASED 이후 결제 증빙 사진 후속 추가. 기존 키와 누적 저장.
     * temp 키 소유권 검증은 controller 또는 호출자에서 별도 처리.
     */
    public OrderDetailView addPurchaseProofs(Long orderId, List<String> proofTempKeys, Long actorId) {
        if (proofTempKeys == null || proofTempKeys.isEmpty()) {
            throw new IllegalArgumentException("proof_keys_required");
        }
        Order order = loadOrder(orderId);
        String tempPrefix = "temp/" + actorId + "/";
        for (String key : proofTempKeys) {
            if (key == null || !key.startsWith(tempPrefix)) {
                throw new IllegalArgumentException("invalid temp image key: " + key);
            }
        }
        List<String> permanent = new ArrayList<>(proofTempKeys.size());
        for (String tempKey : proofTempKeys) {
            String ext = extractExt(tempKey);
            String permanentKey = "orders/" + order.getId() + "/proof/"
                    + UUID.randomUUID() + "." + ext;
            blobStorage.copy(tempKey, permanentKey);
            blobStorage.delete(tempKey);
            permanent.add(permanentKey);
        }
        List<String> existing = new ArrayList<>(order.getPurchaseProofKeys());
        existing.addAll(permanent);
        order.recordPurchaseProofKeys(existing);
        orderRepository.save(order);
        return getOrder(orderId);
    }

    /** Hard-delete an order. Irreversible. ADMIN-only — gate enforced at the controller. */
    public void deleteOrder(Long orderId) {
        // Existence check first so a not-found returns 404 instead of silently no-op'ing.
        loadOrder(orderId);
        orderRepository.deleteOrderAndLogs(orderId);
    }

    public OrderDetailView changePaymentStatus(ChangePaymentStatusCommand cmd, Long actorId) {
        Order order = loadOrder(cmd.orderId());
        order.changePaymentStatus(cmd.target(), actorId, cmd.note());
        order = orderRepository.save(order);
        flushPendingLogs(order);
        return getOrder(order.getId());
    }

    /** ADMIN-only: bypass the state machine and jump fulfillment to any target. Reason mandatory. */
    public OrderDetailView forceChangeFulfillmentStatus(ForceFulfillmentStatusCommand cmd, Long actorId) {
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw new IllegalArgumentException("force_reason_required");
        }
        Order order = loadOrder(cmd.orderId());
        order.forceChangeFulfillmentStatus(cmd.target(), actorId, cmd.reason().trim());
        order = orderRepository.save(order);
        flushPendingLogs(order);
        // Force jumps don't carry a paid amount — only the auto-refund-on-cancel hook fires.
        applyDepositSideEffects(order.getId(), cmd.target(), null, actorId);
        return getOrder(order.getId());
    }

    /** ADMIN-only: bypass the state machine and jump payment to any target. Reason mandatory. */
    public OrderDetailView forceChangePaymentStatus(ForcePaymentStatusCommand cmd, Long actorId) {
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw new IllegalArgumentException("force_reason_required");
        }
        Order order = loadOrder(cmd.orderId());
        order.forceChangePaymentStatus(cmd.target(), actorId, cmd.reason().trim());
        order = orderRepository.save(order);
        flushPendingLogs(order);
        return getOrder(order.getId());
    }

    private Order loadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    /**
     * Bridge to the deposit ledger. PURCHASED debits the deposit by the paid amount;
     * CANCELLED auto-refunds any prior PURCHASE for the same order. Both transitions
     * happen in the same JPA transaction as the order save.
     */
    private void applyDepositSideEffects(Long orderId, FulfillmentStatus target,
                                         java.math.BigDecimal paidAmountKrw, Long actorId) {
        if (target == FulfillmentStatus.PURCHASED) {
            if (paidAmountKrw == null || paidAmountKrw.signum() <= 0) {
                throw new IllegalArgumentException("paid_amount_required");
            }
            depositService.recordPurchase(orderId, paidAmountKrw, actorId);
        } else if (target == FulfillmentStatus.CANCELLED) {
            depositService.refundIfPurchased(orderId, actorId, "Auto-refund on cancel");
        }
    }

    private void flushPendingLogs(Order order) {
        for (Order.PendingLog pending : order.drainPendingLogs()) {
            OrderStatusLog log = pending.forced()
                    ? OrderStatusLog.recordForced(
                            order.getId(),
                            pending.dimension(),
                            pending.fromCode(),
                            pending.toCode(),
                            pending.changedBy(),
                            pending.note())
                    : OrderStatusLog.record(
                            order.getId(),
                            pending.dimension(),
                            pending.fromCode(),
                            pending.toCode(),
                            pending.changedBy(),
                            pending.note());
            orderRepository.saveStatusLog(log);
        }
    }
}
