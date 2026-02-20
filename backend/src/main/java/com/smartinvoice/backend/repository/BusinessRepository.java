package com.smartinvoice.backend.repository;

import com.smartinvoice.backend.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByName(String name);
}