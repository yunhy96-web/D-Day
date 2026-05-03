package com.hauly.intake.order.application;

import com.hauly.intake.order.application.command.ChangeFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ChangePaymentStatusCommand;
import com.hauly.intake.order.application.command.CreateOrderCommand;
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

    public IntakeOrderService(OrderRepository orderRepository,
                              CustomerLookupService customerLookupService,
                              OrderNoGenerator orderNoGenerator,
                              BlobStorage blobStorage) {
        this.orderRepository = orderRepository;
        this.customerLookupService = customerLookupService;
        this.orderNoGenerator = orderNoGenerator;
        this.blobStorage = blobStorage;
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
            return OrderListItemView.from(o, c.getName());
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
        order = orderRepository.save(order);
        flushPendingLogs(order);
        return getOrder(order.getId());
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

    private Order loadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private void flushPendingLogs(Order order) {
        for (Order.PendingLog pending : order.drainPendingLogs()) {
            orderRepository.saveStatusLog(OrderStatusLog.record(
                    order.getId(),
                    pending.dimension(),
                    pending.fromCode(),
                    pending.toCode(),
                    pending.changedBy(),
                    pending.note()
            ));
        }
    }
}
