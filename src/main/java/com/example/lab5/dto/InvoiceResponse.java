package com.example.lab5.dto;

import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        Long orderId,
        String orderCode,
        String invoiceNumber,
        LocalDateTime issuedAt,
        Double total
) {
}
