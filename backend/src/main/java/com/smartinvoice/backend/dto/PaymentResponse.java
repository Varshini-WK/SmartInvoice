package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID id;
    private BigDecimal amount;
    private String paymentReference;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}