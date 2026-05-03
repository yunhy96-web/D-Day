package com.hauly.shared.customer.application;

import com.hauly.shared.customer.application.command.IdentifyCustomerCommand;
import com.hauly.shared.customer.domain.model.Customer;
import com.hauly.shared.customer.domain.model.LineId;
import com.hauly.shared.customer.domain.model.Phone;
import com.hauly.shared.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service: find-or-create a customer for an INTAKE order.
 *
 * Matching priority: line_id → phone → create new GUEST. If a match is found and the
 * stored name differs, we update it to the latest input (girlfriend's intake is authoritative).
 */
@Service
@Transactional
public class CustomerLookupService {

    private final CustomerRepository customerRepository;

    public CustomerLookupService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer findOrCreate(IdentifyCustomerCommand cmd) {
        if (cmd.name() == null || cmd.name().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        LineId lineId = (cmd.lineId() == null || cmd.lineId().isBlank())
                ? null : LineId.of(cmd.lineId());
        Phone phone = (cmd.phone() == null || cmd.phone().isBlank())
                ? null : Phone.of(cmd.phone());

        Optional<Customer> match = Optional.empty();
        if (lineId != null) {
            match = customerRepository.findByLineId(lineId.value());
        }
        if (match.isEmpty() && phone != null) {
            match = customerRepository.findByPhone(phone.value());
        }

        if (match.isPresent()) {
            Customer existing = match.get();
            if (!existing.getName().equals(cmd.name().trim())) {
                existing.rename(cmd.name());
                customerRepository.save(existing);
            }
            return existing;
        }

        Customer fresh = Customer.createGuest(cmd.name(), lineId, phone);
        return customerRepository.save(fresh);
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }
}
