package com.smartinvoice.backend.repository;

import com.smartinvoice.backend.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository
        extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey>
    findByBusinessIdAndIdempotencyKey(UUID businessId, String key);
}
