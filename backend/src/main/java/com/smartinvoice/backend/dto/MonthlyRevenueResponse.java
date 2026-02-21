package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MonthlyRevenueResponse {
    private LocalDate month;
    private BigDecimal revenue;
}