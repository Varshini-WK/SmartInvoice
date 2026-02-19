package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class LineItemResponse {

    private UUID id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxPercent;
    private BigDecimal discountPercent;
    private BigDecimal lineTotal;
}
