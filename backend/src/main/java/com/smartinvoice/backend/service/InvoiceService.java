package com.smartinvoice.backend.service;

import com.smartinvoice.backend.domain.Invoice;
import com.smartinvoice.backend.domain.InvoiceLineItem;
import com.smartinvoice.backend.domain.InvoiceStatus;
import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import com.smartinvoice.backend.dto.InvoiceResponse;
import com.smartinvoice.backend.mapper.InvoiceMapper;
import com.smartinvoice.backend.repository.InvoiceRepository;
import com.smartinvoice.backend.tenant.BusinessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    // =============================
    // CREATE INVOICE
    // =============================
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setCurrency(request.getCurrency());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.DRAFT);

        if (invoice.getLineItems() == null) {
            invoice.setLineItems(new ArrayList<>());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (CreateInvoiceRequest.LineItemRequest itemRequest : request.getLineItems()) {

            BigDecimal quantity = itemRequest.getQuantity();
            BigDecimal unitPrice = itemRequest.getUnitPrice();

            BigDecimal base = quantity.multiply(unitPrice);

            BigDecimal taxPercent = defaultIfNull(itemRequest.getTaxPercent());
            BigDecimal discountPercent = defaultIfNull(itemRequest.getDiscountPercent());

            BigDecimal tax = base.multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100));

            BigDecimal discount = base.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100));

            BigDecimal lineTotal = base.add(tax).subtract(discount);

            subtotal = subtotal.add(base);
            taxTotal = taxTotal.add(tax);
            discountTotal = discountTotal.add(discount);

            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(itemRequest.getDescription());
            lineItem.setQuantity(quantity);
            lineItem.setUnitPrice(unitPrice);
            lineItem.setTaxPercent(taxPercent);
            lineItem.setDiscountPercent(discountPercent);
            lineItem.setLineTotal(lineTotal);

            invoice.getLineItems().add(lineItem);
        }

        BigDecimal totalAmount = subtotal.add(taxTotal).subtract(discountTotal);

        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setDiscountTotal(discountTotal);
        invoice.setTotalAmount(totalAmount);
        invoice.setAmountPaid(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);

        return InvoiceMapper.toResponse(saved);
    }

    // =============================
    // ADD LINE ITEM
    // =============================
    @Transactional
    public InvoiceResponse addLineItem(UUID invoiceId,
                                       CreateInvoiceRequest.LineItemRequest request) {

        Invoice invoice = getInvoice(invoiceId);
        ensureDraft(invoice);

        InvoiceLineItem item = new InvoiceLineItem();

        item.setInvoice(invoice);
        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setTaxPercent(defaultIfNull(request.getTaxPercent()));
        item.setDiscountPercent(defaultIfNull(request.getDiscountPercent()));

        invoice.getLineItems().add(item);

        recalculateTotals(invoice);

        invoice = invoiceRepository.saveAndFlush(invoice);

        return InvoiceMapper.toResponse(invoice);
    }


    // =============================
    // UPDATE LINE ITEM
    // =============================
    @Transactional
    public InvoiceResponse updateLineItem(UUID invoiceId,
                                          UUID itemId,
                                          CreateInvoiceRequest.LineItemRequest request) {

        Invoice invoice = getInvoice(invoiceId);
        ensureDraft(invoice);

        InvoiceLineItem item = invoice.getLineItems()
                .stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Line item not found"));

        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setTaxPercent(defaultIfNull(request.getTaxPercent()));
        item.setDiscountPercent(defaultIfNull(request.getDiscountPercent()));

        recalculateTotals(invoice);

        return InvoiceMapper.toResponse(invoice);
    }

    // =============================
    // DELETE LINE ITEM
    // =============================
    @Transactional
    public InvoiceResponse deleteLineItem(UUID invoiceId, UUID itemId) {

        Invoice invoice = getInvoice(invoiceId);
        ensureDraft(invoice);

        boolean removed = invoice.getLineItems()
                .removeIf(i -> i.getId().equals(itemId));

        if (!removed) {
            throw new RuntimeException("Line item not found");
        }

        recalculateTotals(invoice);

        return InvoiceMapper.toResponse(invoice);
    }

    // =============================
    // GET INVOICE (TENANT SAFE)
    // =============================
    private Invoice getInvoice(UUID invoiceId) {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        return invoiceRepository
                .findByBusinessIdAndId(businessId, invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }


    // =============================
    // ENSURE INVOICE IS EDITABLE
    // =============================
    private void ensureDraft(Invoice invoice) {

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be modified"
            );
        }
    }

    // =============================
    // RECALCULATE TOTALS
    // =============================
    private void recalculateTotals(Invoice invoice) {

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (InvoiceLineItem item : invoice.getLineItems()) {

            BigDecimal base = item.getQuantity()
                    .multiply(item.getUnitPrice());

            BigDecimal taxPercent = defaultIfNull(item.getTaxPercent());
            BigDecimal discountPercent = defaultIfNull(item.getDiscountPercent());

            BigDecimal tax = base.multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100));

            BigDecimal discount = base.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100));

            BigDecimal lineTotal = base.add(tax).subtract(discount);

            item.setLineTotal(lineTotal);

            subtotal = subtotal.add(base);
            taxTotal = taxTotal.add(tax);
            discountTotal = discountTotal.add(discount);
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setDiscountTotal(discountTotal);
        invoice.setTotalAmount(
                subtotal.add(taxTotal).subtract(discountTotal)
        );
    }

    // =============================
    // HELPER
    // =============================
    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
