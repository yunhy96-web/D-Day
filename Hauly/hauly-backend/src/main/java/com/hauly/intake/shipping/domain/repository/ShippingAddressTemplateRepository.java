package com.hauly.intake.shipping.domain.repository;

import com.hauly.intake.shipping.domain.model.ShippingAddressTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressTemplateRepository extends JpaRepository<ShippingAddressTemplate, Long> {

    List<ShippingAddressTemplate> findAllByOrderByCreatedAtDesc();

    Optional<ShippingAddressTemplate> findByLabelIgnoreCase(String label);
}
