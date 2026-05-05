package com.hauly.intake.order.presentation.rest;

import com.hauly.intake.order.application.IntakeOrderService;
import com.hauly.intake.order.application.command.ChangeFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ChangePaymentStatusCommand;
import com.hauly.intake.order.application.command.CreateOrderCommand;
import com.hauly.intake.order.application.command.ForceFulfillmentStatusCommand;
import com.hauly.intake.order.application.command.ForcePaymentStatusCommand;
import com.hauly.intake.order.application.query.OrderDetailView;
import com.hauly.intake.order.domain.model.OrderType;
import com.hauly.intake.order.application.query.OrderListItemView;
import com.hauly.intake.order.domain.model.FulfillmentStatus;
import com.hauly.intake.order.presentation.dto.AddPurchaseProofsRequest;
import com.hauly.intake.order.presentation.dto.ChangeFulfillmentStatusRequest;
import com.hauly.intake.order.presentation.dto.ChangePaymentStatusRequest;
import com.hauly.intake.order.presentation.dto.CreateOrderRequest;
import com.hauly.intake.order.presentation.dto.ForceFulfillmentStatusRequest;
import com.hauly.intake.order.presentation.dto.ForcePaymentStatusRequest;
import com.hauly.intake.order.presentation.dto.UpdateFinancialsRequest;
import com.hauly.intake.order.presentation.dto.UpdatePaidAmountRequest;
import com.hauly.intake.order.presentation.dto.UpdateTrackingRequest;
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
    @PreAuthorize("hasRole('ADMIN')")
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
                request.shippingAddressLabel(),
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> changeFulfillment(
            @PathVariable Long id,
            @Valid @RequestBody ChangeFulfillmentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.changeFulfillmentStatus(
                new ChangeFulfillmentStatusCommand(
                        id, request.target(), request.note(),
                        request.paidAmountKrw(), request.proofTempKeys()),
                userId));
    }

    /** Hard delete — irreversible. ADMIN-only, intended for purging test data. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        intakeOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> changePayment(
            @PathVariable Long id,
            @Valid @RequestBody ChangePaymentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.changePaymentStatus(
                new ChangePaymentStatusCommand(id, request.target(), request.note()), userId));
    }

    /** PURCHASED 이후에도 호출 가능한 트래킹 정보 후속 입력/수정. */
    @PatchMapping("/{id}/tracking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> updateTracking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrackingRequest request) {
        return ResponseEntity.ok(intakeOrderService.updateTracking(
                id, request.koreanCourier(), request.koreanTrackingNo()));
    }

    /** 재무 필드 일괄 수정 (고객입금/물류비/태국배송비/환율). */
    @PatchMapping("/{id}/financials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> updateFinancials(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFinancialsRequest request) {
        return ResponseEntity.ok(intakeOrderService.updateFinancials(
                id,
                request.customerRevenueAmount(), request.customerRevenueCurrency(),
                request.logisticsKrToThAmount(), request.logisticsKrToThCurrency(),
                request.logisticsThDomesticAmount(), request.logisticsThDomesticCurrency(),
                request.krwPerThb()));
    }

    /** paidAmountKrw 직접 수정. 차액만큼 디파짓 원장에 ADJUSTMENT 자동 기록. */
    @PatchMapping("/{id}/paid-amount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> updatePaidAmount(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaidAmountRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.updatePaidAmount(
                id, request.paidAmountKrw(), userId));
    }

    /** PURCHASED 이후 결제 증빙 사진 후속 추가. 누적 저장. */
    @PostMapping("/{id}/proof")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> addProof(
            @PathVariable Long id,
            @Valid @RequestBody AddPurchaseProofsRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.addPurchaseProofs(
                id, request.proofTempKeys(), userId));
    }

    /** ADMIN-only override: jump fulfillment to any status, bypassing the state machine. */
    @PatchMapping("/{id}/fulfillment-status/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> forceFulfillment(
            @PathVariable Long id,
            @Valid @RequestBody ForceFulfillmentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.forceChangeFulfillmentStatus(
                new ForceFulfillmentStatusCommand(id, request.target(), request.reason()), userId));
    }

    /** ADMIN-only override: jump payment to any status, bypassing the state machine. */
    @PatchMapping("/{id}/payment-status/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDetailView> forcePayment(
            @PathVariable Long id,
            @Valid @RequestBody ForcePaymentStatusRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(intakeOrderService.forceChangePaymentStatus(
                new ForcePaymentStatusCommand(id, request.target(), request.reason()), userId));
    }
}
