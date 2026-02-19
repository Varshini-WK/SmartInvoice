package com.smartinvoice.backend.domain;

public enum InvoiceStatus {

    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    REFUNDED
}
