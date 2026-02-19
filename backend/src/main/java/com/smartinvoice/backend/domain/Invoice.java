package com.smartinvoice.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"business_id", "invoice_number"}
                )
        })
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "invoice_status", nullable = false)
    private InvoiceStatus status;


    private LocalDate issueDate;
    private LocalDate dueDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 4)
    private BigDecimal taxTotal;

    @Column(precision = 19, scale = 4)
    private BigDecimal discountTotal;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @OneToMany(mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

}
