package com.smartinvoice.backend.service;

import com.smartinvoice.backend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smartinvoice.backend.repository.ReportRepository;
import com.smartinvoice.backend.tenant.BusinessContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    private UUID getBusinessId() {
        return UUID.fromString(BusinessContext.getBusinessId());
    }

    public RevenueResponse revenueByDateRange(LocalDate start,
                                              LocalDate end) {

        BigDecimal revenue = reportRepository.revenueByDateRange(
                getBusinessId(),
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );

        return RevenueResponse.builder()
                .startDate(start)
                .endDate(end)
                .totalRevenue(revenue)
                .build();
    }

    public List<OutstandingInvoiceResponse> outstandingInvoices() {
        return reportRepository.outstandingInvoices(getBusinessId())
                .stream()
                .map(row -> OutstandingInvoiceResponse.builder()
                        .invoiceId((UUID) row[0])
                        .invoiceNumber((String) row[1])
                        .customerName((String) row[2])
                        .remainingAmount((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    public List<OutstandingInvoiceResponse> overdueInvoices() {
        return reportRepository.overdueInvoices(getBusinessId())
                .stream()
                .map(row -> OutstandingInvoiceResponse.builder()
                        .invoiceId((UUID) row[0])
                        .invoiceNumber((String) row[1])
                        .customerName((String) row[2])
                        .remainingAmount((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    public List<RevenueByCustomerResponse> revenueByCustomer() {
        return reportRepository.revenueByCustomer(getBusinessId())
                .stream()
                .map(row -> RevenueByCustomerResponse.builder()
                        .customerId((UUID) row[0])
                        .customerName((String) row[1])
                        .totalRevenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    public List<MonthlyRevenueResponse> monthlyRevenue() {
        return reportRepository.monthlyRevenue(getBusinessId())
                .stream()
                .map(row -> MonthlyRevenueResponse.builder()
                        .month(((java.sql.Timestamp) row[0])
                                .toLocalDateTime()
                                .toLocalDate())
                        .revenue((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }
}
