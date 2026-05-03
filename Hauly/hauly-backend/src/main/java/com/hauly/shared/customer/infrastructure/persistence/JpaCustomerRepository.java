package com.hauly.shared.customer.infrastructure.persistence;

import com.hauly.shared.customer.domain.model.Customer;
import com.hauly.shared.customer.domain.repository.CustomerRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaCustomerRepository extends JpaRepository<Customer, Long>, CustomerRepository {

    Optional<Customer> findByLineId(String lineId);

    Optional<Customer> findByPhone(String phone);
}
