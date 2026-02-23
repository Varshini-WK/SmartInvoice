package com.smartinvoice.backend.dto;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private UUID customerId;
    private String currency;
    private String status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Integer gst ;

    private BigDecimal subtotal;
    private BigDecimal taxTotal;
    private BigDecimal discountTotal;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;

    private List<LineItemResponse> lineItems;
}
