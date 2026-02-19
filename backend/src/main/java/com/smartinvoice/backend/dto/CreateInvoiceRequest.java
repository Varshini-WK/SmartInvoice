package com.smartinvoice.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @NotEmpty
    private List<LineItemRequest> lineItems;
    @Getter
    @Setter
    public static class LineItemRequest {

        @NotBlank
        private String description;

        @NotNull
        private BigDecimal quantity;

        @NotNull
        private BigDecimal unitPrice;

        private BigDecimal taxPercent;
        private BigDecimal discountPercent;
    }


}
