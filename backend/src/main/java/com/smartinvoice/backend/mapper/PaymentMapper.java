package com.smartinvoice.backend.mapper;

import com.smartinvoice.backend.domain.Payment;
import com.smartinvoice.backend.dto.PaymentResponse;

public class PaymentMapper {

    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentReference(payment.getPaymentReference())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}