package com.smartinvoice.backend.service;

import com.smartinvoice.backend.domain.Invoice;
import com.smartinvoice.backend.domain.InvoiceStatus;
import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartinvoice.backend.repository.InvoiceRepository;
import com.smartinvoice.backend.tenant.BusinessContext;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request) {

        UUID businessId =
                UUID.fromString(BusinessContext.getBusinessId());

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setCurrency(request.getCurrency());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.DRAFT);

        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxTotal(BigDecimal.ZERO);
        invoice.setDiscountTotal(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setAmountPaid(BigDecimal.ZERO);

        return invoiceRepository.save(invoice);
    }

}

