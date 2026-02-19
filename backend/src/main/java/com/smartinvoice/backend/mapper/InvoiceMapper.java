package com.smartinvoice.backend.mapper;


import com.smartinvoice.backend.domain.Invoice;
import com.smartinvoice.backend.domain.InvoiceLineItem;
import com.smartinvoice.backend.dto.InvoiceResponse;
import com.smartinvoice.backend.dto.LineItemResponse;

import java.util.stream.Collectors;

public class InvoiceMapper {

    public static InvoiceResponse toResponse(Invoice invoice) {

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerId(invoice.getCustomerId())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus().name())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .taxTotal(invoice.getTaxTotal())
                .discountTotal(invoice.getDiscountTotal())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .lineItems(
                        invoice.getLineItems()
                                .stream()
                                .map(InvoiceMapper::mapLineItem)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static LineItemResponse mapLineItem(InvoiceLineItem item) {

        return LineItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxPercent(item.getTaxPercent())
                .discountPercent(item.getDiscountPercent())
                .lineTotal(item.getLineTotal())
                .build();
    }
}

