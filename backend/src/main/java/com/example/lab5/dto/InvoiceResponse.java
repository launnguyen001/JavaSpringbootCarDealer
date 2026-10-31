package com.example.lab5.dto;

import com.example.lab5.entity.InvoicePaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        Long orderId,
        String orderCode,
        String invoiceNumber,
        LocalDateTime issuedAt,
        Double total,
        Double paidAmount,
        Double remainingAmount,
        InvoicePaymentStatus paymentStatus,
        LocalDate dueDate,
        String customerName
) {
}
