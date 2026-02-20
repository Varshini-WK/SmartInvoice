package com.smartinvoice.backend.controller;

import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import com.smartinvoice.backend.dto.InvoiceResponse;

import com.smartinvoice.backend.dto.RecordPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartinvoice.backend.service.InvoiceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @RequestBody @Valid CreateInvoiceRequest request
    ) {
        return ResponseEntity.ok(
                invoiceService.createInvoice(request)
        );
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceResponse> sendInvoice(
            @PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.sendInvoice(id));
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<InvoiceResponse> addLineItem(
            @PathVariable UUID id,
            @RequestBody CreateInvoiceRequest.LineItemRequest request) {
        return ResponseEntity.ok(invoiceService.addLineItem(id, request));
    }

    @PutMapping("/{id}/line-items/{itemId}")
    public ResponseEntity<InvoiceResponse> updateLineItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody CreateInvoiceRequest.LineItemRequest request) {
        return ResponseEntity.ok(invoiceService.updateLineItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/line-items/{itemId}")
    public ResponseEntity<InvoiceResponse> deleteLineItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(invoiceService.deleteLineItem(id, itemId));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<InvoiceResponse> recordPayment(
            @PathVariable UUID id,
            @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(invoiceService.recordPayment(id, request));
    }

}
