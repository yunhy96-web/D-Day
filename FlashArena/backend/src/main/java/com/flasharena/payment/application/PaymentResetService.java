package com.flasharena.payment.application;

import com.flasharena.payment.infrastructure.PaymentHistoryRepository;
import org.springframework.stereotype.Service;

/**
 * payment 모듈이 외부(시뮬레이터 등)에 노출하는 공개 리셋 API.
 * <p>다른 모듈(order)은 payment 의 내부(infrastructure/domain)를 직접 건드리지 않고
 * 이 application-layer 포트를 통해서만 payment 데이터를 다룬다 — 모듈 경계 유지.
 * 실제 분리(MSA) 시 이 포트는 payment 서비스의 엔드포인트로 승격된다.
 */
@Service
public class PaymentResetService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentResetService(PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    /** 시뮬레이터 재현성 확보용: 결제 이력 전체 삭제 (payment 스키마, 자체 트랜잭션). */
    public void resetHistory() {
        paymentHistoryRepository.deleteAllHistory();
    }
}
