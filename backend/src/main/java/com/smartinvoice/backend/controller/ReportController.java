package com.smartinvoice.backend.controller;

import com.smartinvoice.backend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.smartinvoice.backend.service.ReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public RevenueResponse revenue(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return reportService.revenueByDateRange(startDate, endDate);
    }

    @GetMapping("/outstanding")
    public List<OutstandingInvoiceResponse> outstanding() {
        return reportService.outstandingInvoices();
    }

    @GetMapping("/overdue")
    public List<OutstandingInvoiceResponse> overdue() {
        return reportService.overdueInvoices();
    }

    @GetMapping("/revenue-by-customer")
    public List<RevenueByCustomerResponse> revenueByCustomer() {
        return reportService.revenueByCustomer();
    }

    @GetMapping("/monthly")
    public List<MonthlyRevenueResponse> monthly() {
        return reportService.monthlyRevenue();
    }
}