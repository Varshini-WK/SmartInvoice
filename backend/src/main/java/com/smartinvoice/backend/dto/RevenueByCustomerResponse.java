package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class RevenueByCustomerResponse {
    private UUID customerId;
    private String customerName;
    private BigDecimal totalRevenue;
}