package com.smartinvoice.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateInvoiceRequest {

    @NotBlank
    private String invoiceNumber;

    @NotNull
    private UUID customerId;

    @NotBlank
    private String currency;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    private LocalDate dueDate;
}
