package com.hauly.intake.order.presentation.rest;

import com.hauly.intake.order.application.OrderNoteService;
import com.hauly.intake.order.application.command.CreateOrderNoteCommand;
import com.hauly.intake.order.application.command.UpdateOrderNoteCommand;
import com.hauly.intake.order.application.query.OrderNoteView;
import com.hauly.intake.order.presentation.dto.CreateOrderNoteRequest;
import com.hauly.intake.order.presentation.dto.UpdateOrderNoteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST entry point for collaborative notes on an order. Auth required (any logged-in INTAKE user
 * can comment); edit/delete is restricted to the original author and enforced inside the service.
 */
@RestController
@RequestMapping("/api/intake/orders/{orderId}/notes")
public class IntakeOrderNoteController {

    private final OrderNoteService orderNoteService;

    public IntakeOrderNoteController(OrderNoteService orderNoteService) {
        this.orderNoteService = orderNoteService;
    }

    @GetMapping
    public ResponseEntity<List<OrderNoteView>> list(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderNoteService.listByOrder(orderId));
    }

    @PostMapping
    public ResponseEntity<OrderNoteView> create(@PathVariable Long orderId,
                                                @Valid @RequestBody CreateOrderNoteRequest request,
                                                @AuthenticationPrincipal Long userId) {
        OrderNoteView created = orderNoteService.create(
                new CreateOrderNoteCommand(orderId, request.body()), userId);
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{noteId}")
    public ResponseEntity<OrderNoteView> update(@PathVariable Long orderId,
                                                @PathVariable Long noteId,
                                                @Valid @RequestBody UpdateOrderNoteRequest request,
                                                @AuthenticationPrincipal Long userId) {
        OrderNoteView updated = orderNoteService.update(
                new UpdateOrderNoteCommand(orderId, noteId, request.body()), userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId,
                                       @PathVariable Long noteId,
                                       @AuthenticationPrincipal Long userId) {
        orderNoteService.delete(orderId, noteId, userId);
        return ResponseEntity.noContent().build();
    }
}
