package com.smartinvoice.backend.service;

import com.smartinvoice.backend.domain.Invoice;
import com.smartinvoice.backend.domain.InvoiceLineItem;
import com.smartinvoice.backend.domain.InvoiceStatus;
import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import com.smartinvoice.backend.dto.InvoiceResponse;
import com.smartinvoice.backend.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
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
    public InvoiceResponse createInvoice(CreateInvoiceRequest request)
    {

        // Get tenant/business ID from current request
        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        // 1️⃣ Create invoice object
        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setCurrency(request.getCurrency());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.DRAFT);

        // Initialize totals
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal totalAmount;

        // Initialize lineItems list if null
        if (invoice.getLineItems() == null) {
            invoice.setLineItems(new ArrayList<>());
        }

        // 2️⃣ Loop through all line items in request
        for (CreateInvoiceRequest.LineItemRequest itemRequest : request.getLineItems()) {

            BigDecimal quantity = itemRequest.getQuantity();
            BigDecimal unitPrice = itemRequest.getUnitPrice();

            // Base amount = quantity * unitPrice
            BigDecimal base = quantity.multiply(unitPrice);

            // Use 0 if taxPercent or discountPercent is null
            BigDecimal taxPercent = itemRequest.getTaxPercent() == null
                    ? BigDecimal.ZERO
                    : itemRequest.getTaxPercent();

            BigDecimal discountPercent = itemRequest.getDiscountPercent() == null
                    ? BigDecimal.ZERO
                    : itemRequest.getDiscountPercent();

            // tax = base * taxPercent / 100
            BigDecimal tax = base.multiply(taxPercent).divide(BigDecimal.valueOf(100));

            // discount = base * discountPercent / 100
            BigDecimal discount = base.multiply(discountPercent).divide(BigDecimal.valueOf(100));

            // line total = base + tax - discount
            BigDecimal lineTotal = base.add(tax).subtract(discount);

            // 3️⃣ Add to invoice totals
            subtotal = subtotal.add(base);
            taxTotal = taxTotal.add(tax);
            discountTotal = discountTotal.add(discount);

            // 4️⃣ Create InvoiceLineItem entity
            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(invoice); // link to parent invoice
            lineItem.setDescription(itemRequest.getDescription());
            lineItem.setQuantity(quantity);
            lineItem.setUnitPrice(unitPrice);
            lineItem.setTaxPercent(taxPercent);
            lineItem.setDiscountPercent(discountPercent);
            lineItem.setLineTotal(lineTotal);

            // Add line item to invoice
            invoice.getLineItems().add(lineItem);
        }

        // 5️⃣ Calculate total amount for invoice
        totalAmount = subtotal.add(taxTotal).subtract(discountTotal);

        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setDiscountTotal(discountTotal);
        invoice.setTotalAmount(totalAmount);
        invoice.setAmountPaid(BigDecimal.ZERO); // always zero at creation

        // 6️⃣ Save invoice with line items (cascade = ALL saves line items automatically)
        Invoice saved = invoiceRepository.save(invoice);

        return InvoiceMapper.toResponse(saved);

    }
}
