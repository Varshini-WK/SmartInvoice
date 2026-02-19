package com.smartinvoice.backend.controller;


import com.smartinvoice.backend.domain.Invoice;
import com.smartinvoice.backend.dto.CreateInvoiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartinvoice.backend.service.InvoiceService;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<Invoice> create(
            @RequestBody @Valid CreateInvoiceRequest request
    ) {
        return ResponseEntity.ok(
                invoiceService.createInvoice(request)
        );
    }
}
