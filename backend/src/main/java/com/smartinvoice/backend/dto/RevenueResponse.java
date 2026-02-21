package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RevenueResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
}