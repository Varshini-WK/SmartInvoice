package com.smartinvoice.backend.repository;

import com.smartinvoice.backend.domain.Invoice;
//import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByBusinessIdAndInvoiceNumber(
            UUID businessId,
            String invoiceNumber
    );
    Optional<Invoice> findByIdAndBusinessId(UUID id, UUID businessId);
    Optional<Invoice> findByBusinessIdAndId(UUID businessId, UUID id);
    List<Invoice> findByBusinessId(UUID businessId);
}

