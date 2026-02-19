package com.smartinvoice.backend.repository;


import com.smartinvoice.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByBusinessIdAndId(UUID businessId, UUID id);

    List<Customer> findAllByBusinessId(UUID businessId);
}