package com.smartinvoice.backend.repository;

import com.smartinvoice.backend.domain.Invoice;
//import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByBusinessIdAndInvoiceNumber(
            UUID businessId,
            String invoiceNumber
    );

    Optional<Invoice> findByBusinessIdAndId(UUID businessId, UUID id);
}

