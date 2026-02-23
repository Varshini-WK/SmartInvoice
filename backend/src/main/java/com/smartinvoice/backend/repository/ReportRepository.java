package com.smartinvoice.backend.repository;

import com.smartinvoice.backend.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Invoice, UUID> {


    @Query(value = """
        SELECT COALESCE(SUM(amount), 0)
        FROM payments
        WHERE business_id = :businessId
        AND status = 'RECEIVED'
        AND created_at BETWEEN :start AND :end
        """, nativeQuery = true)
    BigDecimal revenueByDateRange(@Param("businessId") UUID businessId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);


    @Query(value = """
        SELECT i.id, i.invoice_number, c.name,
               (i.total_amount - i.amount_paid) AS remaining
        FROM invoices i
        JOIN customers c ON i.customer_id = c.id
        WHERE i.business_id = :businessId
        AND i.amount_paid < i.total_amount
        """, nativeQuery = true)
    List<Object[]> outstandingInvoices(@Param("businessId") UUID businessId);


    @Query(value = """
        SELECT i.id, i.invoice_number, c.name,
               i.total_amount - i.amount_paid
        FROM invoices i
        JOIN customers c ON i.customer_id = c.id
        WHERE i.business_id = :businessId
        AND i.due_date < CURRENT_DATE
        AND i.status != 'PAID'
        """, nativeQuery = true)
    List<Object[]> overdueInvoices(@Param("businessId") UUID businessId);


    @Query(value = """
        SELECT c.id, c.name, COALESCE(SUM(i.amount_paid), 0)
        FROM invoices i
        JOIN customers c ON i.customer_id = c.id
        WHERE i.business_id = :businessId
        GROUP BY c.id, c.name
        """, nativeQuery = true)
    List<Object[]> revenueByCustomer(@Param("businessId") UUID businessId);


    @Query(value = """
        SELECT DATE_TRUNC('month', created_at) AS month,
               SUM(amount)
        FROM payments
        WHERE business_id = :businessId
        AND status = 'RECEIVED'
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> monthlyRevenue(@Param("businessId") UUID businessId);
}
