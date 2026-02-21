package com.smartinvoice.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinvoice.backend.domain.*;
import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import com.smartinvoice.backend.dto.InvoiceResponse;
import com.smartinvoice.backend.dto.RecordPaymentRequest;
import com.smartinvoice.backend.dto.RefundRequest;
import com.smartinvoice.backend.mapper.InvoiceMapper;
import com.smartinvoice.backend.repository.IdempotencyRepository;
import com.smartinvoice.backend.repository.InvoiceRepository;
import com.smartinvoice.backend.repository.PaymentRepository;
import com.smartinvoice.backend.repository.RefundRepository;
import com.smartinvoice.backend.tenant.BusinessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static com.smartinvoice.backend.tenant.BusinessContext.getBusinessId;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    private final IdempotencyRepository idempotencyRepository;   // ✅ add
    private final ObjectMapper objectMapper;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        UUID businessId = UUID.fromString(getBusinessId());

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

    @Transactional
    public InvoiceResponse sendInvoice(UUID invoiceId) {

        Invoice invoice = getInvoice(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be sent");
        }

        if (invoice.getLineItems().isEmpty()) {
            throw new IllegalStateException("Cannot send invoice without line items");
        }


        if (invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice total must be greater than zero");
        }


        invoice.setStatus(InvoiceStatus.SENT);

        return InvoiceMapper.toResponse(invoice);
    }

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


    private Invoice getInvoice(UUID invoiceId) {

        UUID businessId = UUID.fromString(getBusinessId());

        return invoiceRepository
                .findByBusinessIdAndId(businessId, invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }


    private void ensureDraft(Invoice invoice) {

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be modified"
            );
        }
    }

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


    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    @Transactional
    public InvoiceResponse recordPayment(UUID invoiceId,
                                         RecordPaymentRequest request,
                                         String idempotencyKey) {

        UUID businessId = UUID.fromString(getBusinessId());

        // 1️⃣ Check idempotency
        Optional<IdempotencyKey> existing =
                idempotencyRepository
                        .findByBusinessIdAndIdempotencyKey(
                                businessId,
                                idempotencyKey
                        );

        if (existing.isPresent()) {
            try {
                return objectMapper.readValue(
                        existing.get().getResponseBody(),
                        InvoiceResponse.class
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize stored response");
            }
        }

        // 2️⃣ Normal payment logic
        Invoice invoice = getInvoice(invoiceId);

        if (!(invoice.getStatus() == InvoiceStatus.SENT
                || invoice.getStatus() == InvoiceStatus.OVERDUE
                || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)) {

            throw new IllegalStateException("Invoice is not payable");
        }

        if (!invoice.getCurrency().equals(request.getCurrency())) {
            throw new IllegalArgumentException("Payment currency mismatch");
        }

        BigDecimal newAmountPaid =
                invoice.getAmountPaid().add(request.getAmount());

        if (newAmountPaid.compareTo(invoice.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Payment exceeds remaining balance");
        }

        Payment payment = new Payment();
        payment.setBusinessId(invoice.getBusinessId());
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentReference(request.getPaymentReference());
        payment.setStatus(PaymentStatus.RECEIVED);

        paymentRepository.save(payment);

        invoice.setAmountPaid(newAmountPaid);

        if (newAmountPaid.compareTo(invoice.getTotalAmount()) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        InvoiceResponse response =
                InvoiceMapper.toResponse(invoice);

        // 3️⃣ Store idempotency record
        try {
            String json = objectMapper.writeValueAsString(response);

            IdempotencyKey record = new IdempotencyKey();
            record.setBusinessId(businessId);
            record.setIdempotencyKey(idempotencyKey);
            record.setResponseBody(json);

            idempotencyRepository.save(record);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store idempotency record");
        }

        return response;
    }
    @Transactional
    public InvoiceResponse refundPayment(UUID paymentId,
                                         RefundRequest request) {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Calculate total refunded so far
        java.util.List<Refund> existingRefunds =
                refundRepository.findByPaymentId(paymentId);

        BigDecimal totalRefunded = existingRefunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotalRefunded =
                totalRefunded.add(request.getAmount());

        if (newTotalRefunded.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund exceeds payment amount");
        }

        // Create refund record
        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());

        refundRepository.save(refund);

        // Update invoice
        Invoice invoice = getInvoice(payment.getInvoiceId());

        BigDecimal updatedAmountPaid =
                invoice.getAmountPaid().subtract(request.getAmount());

        invoice.setAmountPaid(updatedAmountPaid);

        // Update invoice status
        if (updatedAmountPaid.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.SENT);
        } else if (updatedAmountPaid.compareTo(invoice.getTotalAmount()) < 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        invoiceRepository.save(invoice);
        return InvoiceMapper.toResponse(invoice);
    }

}
