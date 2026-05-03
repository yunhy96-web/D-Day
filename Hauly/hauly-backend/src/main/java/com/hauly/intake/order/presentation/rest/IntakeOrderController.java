package com.hauly.intake.order.presentation.rest;

import com.hauly.intake.order.application.IntakeOrderService;
import com.hauly.intake.order.application.command.ChangeFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ChangePaymentStatusCommand;
import com.hauly.intake.order.application.command.CreateOrderCommand;
import com.hauly.intake.order.application.query.OrderDetailView;
import com.hauly.intake.order.domain.model.OrderType;
import com.hauly.intake.order.application.query.OrderListItemView;
import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.presentation.dto.ChangeFulfillmentStatusRequest;
import com.hauly.intake.order.presentation.dto.ChangePaymentStatusRequest;
import com.hauly.intake.order.presentation.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST entry point for INTAKE order management.
 * Auth required (enforced by SecurityConfig — only /api/auth/login etc. are permitAll).
 */
@RestController
@RequestMapping("/api/intake/orders")
public class IntakeOrderController {

    private final IntakeOrderService intakeOrderService;

    public IntakeOrderController(IntakeOrderService intakeOrderService) {
        this.intakeOrderService = intakeOrderService;
    }

    @PostMapping
    public ResponseEntity<OrderDetailView> create(@Valid @RequestBody CreateOrderRequest request,
                                                  @AuthenticationPrincipal Long userId) {
        List<CreateOrderCommand.Item> items = request.items().stream()
                .map(i -> new CreateOrderCommand.Item(
                        i.productName(), i.productUrl(), i.quantity(),
                        i.categoryId(), i.attributes(),
                        i.unitPriceAmount(), i.unitPriceCurrency(),
                        i.tempImageKeys()))
                .toList();

        OrderType orderType = (request.orderType() == null || request.orderType().isBlank())
                ? OrderType.INDIVIDUAL
                : OrderType.valueOf(request.orderType());

        OrderDetailView detail = intakeOrderService.createOrder(new CreateOrderCommand(
                request.customerName(),
                request.customerLineId(),
                request.customerPhone(),
                orderType,
                request.customerMemo(),
                request.internalMemo(),
                request.koreanTrackingNo(),
                request.koreanCourier(),
                request.recipientName(),
                request.recipientPhone(),
                request.postalCode(),
                request.addressLine(),
                request.country(),
                items
        ), userId);
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<Page<OrderListItemView>> list(
            @RequestParam(required = false) FulfillmentStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(intakeOrderService.listOrders(status, q, sort, dir, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailView> get(@PathVariable Long id) {
        return ResponseEntity.ok(intakeOrderService.getOrder(id));
    }

    @PatchMapping("/{id}/fulfillment-status")
    public ResponseEntity<OrderDetailView> changeFulfillment(
            @PathVariable Long id,
            @Valid @RequestBody ChangeFulfillmentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.changeFulfillmentStatus(
                new ChangeFulfillmentStatusCommand(id, request.target(), request.note()), userId));
    }

    /** Hard delete — irreversible. ADMIN-only, intended for purging test data. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        intakeOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<OrderDetailView> changePayment(
            @PathVariable Long id,
            @Valid @RequestBody ChangePaymentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.changePaymentStatus(
                new ChangePaymentStatusCommand(id, request.target(), request.note()), userId));
    }
}
