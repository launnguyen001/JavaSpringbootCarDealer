package com.example.lab5.dto;

import java.time.LocalDateTime;

public record CarSalesOrderLine(
        Long orderId,
        String orderCode,
        LocalDateTime orderDate,
        String status,
        String customerName,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {
}
