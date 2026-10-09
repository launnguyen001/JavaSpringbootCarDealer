package com.example.lab5.dto;

import com.example.lab5.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderResponse(
        Long id,
        String orderCode,
        LocalDateTime orderDate,
        OrderStatus status,
        Double totalAmount,
        Long customerId,
        String customerName,
        Long employeeId,
        String employeeName,
        List<OrderItemResponse> items
) {
}
