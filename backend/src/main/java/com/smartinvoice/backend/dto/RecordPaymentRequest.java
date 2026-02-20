package com.smartinvoice.backend.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecordPaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String currency;

    private String paymentReference;
}
