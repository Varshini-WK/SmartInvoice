package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OutstandingInvoiceResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private String customerName;
    private BigDecimal remainingAmount;
}