package com.hauly.intake.shipping.presentation.rest;

import com.hauly.intake.shipping.application.ShippingTemplateService;
import com.hauly.intake.shipping.application.command.CreateShippingTemplateCommand;
import com.hauly.intake.shipping.application.query.ShippingAddressTemplateView;
import com.hauly.intake.shipping.presentation.dto.CreateShippingTemplateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 배송지 템플릿 — 조회는 모든 인증 사용자, 생성/삭제는 ADMIN.
 */
@RestController
@RequestMapping("/api/intake/shipping-templates")
public class IntakeShippingTemplateController {

    private final ShippingTemplateService service;

    public IntakeShippingTemplateController(ShippingTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ShippingAddressTemplateView>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingAddressTemplateView> create(
            @Valid @RequestBody CreateShippingTemplateRequest req,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.create(new CreateShippingTemplateCommand(
                req.label(), req.recipientName(), req.recipientPhone(),
                req.postalCode(), req.addressLine(), req.country()), userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
