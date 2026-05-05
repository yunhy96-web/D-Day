package com.hauly.intake.shipping.application;

import com.hauly.intake.shipping.application.command.CreateShippingTemplateCommand;
import com.hauly.intake.shipping.application.query.ShippingAddressTemplateView;
import com.hauly.intake.shipping.domain.model.ShippingAddressTemplate;
import com.hauly.intake.shipping.domain.repository.ShippingAddressTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 배송지 템플릿 CRUD 서비스. 운영자가 공유하는 풀.
 */
@Service
@Transactional
public class ShippingTemplateService {

    private final ShippingAddressTemplateRepository repository;

    public ShippingTemplateService(ShippingAddressTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ShippingAddressTemplateView> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(ShippingAddressTemplateView::from)
                .toList();
    }

    public ShippingAddressTemplateView create(CreateShippingTemplateCommand cmd, Long actorId) {
        repository.findByLabelIgnoreCase(cmd.label().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("shipping_template_label_duplicated");
                });
        ShippingAddressTemplate t = ShippingAddressTemplate.create(
                cmd.label(), cmd.recipientName(), cmd.recipientPhone(),
                cmd.postalCode(), cmd.addressLine(), cmd.country(), actorId);
        return ShippingAddressTemplateView.from(repository.save(t));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("shipping_template_not_found");
        }
        repository.deleteById(id);
    }
}
