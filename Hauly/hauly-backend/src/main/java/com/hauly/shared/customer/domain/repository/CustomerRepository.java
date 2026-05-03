package com.hauly.shared.customer.domain.repository;

import com.hauly.shared.customer.domain.model.Customer;

import java.util.Optional;

/**
 * Customer repository — domain interface (depended upon by application layer).
 * Infrastructure provides the implementation.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    Optional<Customer> findByLineId(String lineId);

    Optional<Customer> findByPhone(String phone);
}
