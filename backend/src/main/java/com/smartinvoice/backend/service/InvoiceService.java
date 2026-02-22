package com.smartinvoice.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinvoice.backend.domain.*;
import com.smartinvoice.backend.dto.*;
import com.smartinvoice.backend.mapper.InvoiceMapper;
import com.smartinvoice.backend.mapper.PaymentMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartinvoice.backend.tenant.BusinessContext.getBusinessId;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

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
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        Invoice invoice = invoiceRepository
                .findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return InvoiceMapper.toResponse(invoice);
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

        // 1️⃣ Idempotency Check
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

        Invoice invoice = getInvoice(invoiceId);

        // 2️⃣ Status validation
        if (!(invoice.getStatus() == InvoiceStatus.SENT
                || invoice.getStatus() == InvoiceStatus.OVERDUE
                || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)) {

            throw new IllegalStateException("Invoice is not payable");
        }

        // ❌ REMOVE currency validation completely
        // if (!invoice.getCurrency().equals(request.getCurrency())) {
        //     throw new IllegalArgumentException("Payment currency mismatch");
        // }

        // 3️⃣ Validate amount
        BigDecimal newAmountPaid =
                invoice.getAmountPaid().add(request.getAmount());

        if (newAmountPaid.compareTo(invoice.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Payment exceeds remaining balance");
        }

        // 4️⃣ Create Payment
        Payment payment = new Payment();
        payment.setBusinessId(invoice.getBusinessId());
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(request.getAmount());
        payment.setPaymentReference(request.getPaymentReference());

        // ✅ Always inherit invoice currency
        payment.setCurrency(invoice.getCurrency());

        payment.setStatus(PaymentStatus.RECEIVED);

        paymentRepository.save(payment);

        auditService.log(
                businessId,
                "PAYMENT",
                payment.getId(),
                "CREATED",
                null,
                payment
        );

        // 5️⃣ Update invoice
        invoice.setAmountPaid(newAmountPaid);

        if (newAmountPaid.compareTo(invoice.getTotalAmount()) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        InvoiceResponse response = InvoiceMapper.toResponse(invoice);

        // 6️⃣ Store idempotency record
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
        auditService.log(
                businessId,
                "REFUND",
                refund.getId(),
                "CREATED",
                null,
                refund
        );

        // Update invoice
        Invoice invoice = getInvoice(payment.getInvoiceId());
        Invoice oldInvoiceState = cloneInvoice(invoice);
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
        auditService.log(
                businessId,
                "INVOICE",
                invoice.getId(),
                "REFUND_APPLIED",
                oldInvoiceState,
                invoice
        );
        return InvoiceMapper.toResponse(invoice);


    }

    private Invoice cloneInvoice(Invoice invoice) {

        Invoice copy = new Invoice();

        copy.setId(invoice.getId());
        copy.setStatus(invoice.getStatus());
        copy.setAmountPaid(invoice.getAmountPaid());
        copy.setTotalAmount(invoice.getTotalAmount());

        return copy;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        return invoiceRepository
                .findByBusinessId(businessId)
                .stream()
                .map(InvoiceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(UUID invoiceId) {

        UUID businessId = UUID.fromString(BusinessContext.getBusinessId());

        // Ensure invoice belongs to business
        invoiceRepository
                .findByIdAndBusinessId(invoiceId, businessId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return paymentRepository
                .findByInvoiceIdAndBusinessId(invoiceId, businessId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }


}
