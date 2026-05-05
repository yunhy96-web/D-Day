package com.hauly.intake.deposit.presentation.rest;

import com.hauly.intake.deposit.application.DepositService;
import com.hauly.intake.deposit.application.command.RecordAdjustmentCommand;
import com.hauly.intake.deposit.application.query.DepositSummaryView;
import com.hauly.intake.deposit.application.query.DepositTransactionView;
import com.hauly.intake.deposit.presentation.dto.RecordAdjustmentRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deposit ledger endpoints. 조회는 인증된 모든 사용자, 잔액 조정은 ADMIN만.
 * 자동 차감/환원은 주문 서비스 내부에서 발생하며, 이 컨트롤러는 읽기 뷰와 수동 조정만 노출함.
 */
@RestController
@RequestMapping("/api/intake/deposits")
public class IntakeDepositController {

    private final DepositService deposits;

    public IntakeDepositController(DepositService deposits) {
        this.deposits = deposits;
    }

    @GetMapping
    public ResponseEntity<DepositSummaryView> summary() {
        return ResponseEntity.ok(deposits.summary());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<DepositTransactionView>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(deposits.listTransactions(page, size));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepositTransactionView> adjust(
            @Valid @RequestBody RecordAdjustmentRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(deposits.recordAdjustment(
                new RecordAdjustmentCommand(request.amountKrw(), request.note()), userId));
    }
}
