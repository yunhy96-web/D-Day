package com.thaiuniontour.domain.payment.repository;

import com.thaiuniontour.domain.payment.model.PaymentHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    boolean existsByReservationId(UUID reservationId);

    Optional<PaymentHistory> findByReservationId(UUID reservationId);
}
