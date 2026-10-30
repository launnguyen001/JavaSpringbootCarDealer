package com.example.lab5.dto;

import com.example.lab5.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        Long id,
        Long orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String changedBy,
        String note,
        LocalDateTime changedAt
) {
}
