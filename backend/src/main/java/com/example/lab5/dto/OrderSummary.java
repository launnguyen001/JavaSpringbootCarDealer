package com.example.lab5.dto;

import java.time.LocalDateTime;

public record OrderSummary(
        Long orderId,
        String orderCode,
        LocalDateTime orderDate,
        String status,
        Double totalAmount,
        String customerName,
        String employeeName
) {
}
