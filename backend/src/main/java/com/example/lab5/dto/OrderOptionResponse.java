package com.example.lab5.dto;

import com.example.lab5.entity.OrderStatus;

import java.time.LocalDateTime;

/**
 * Một đơn hàng "đủ điều kiện" để gắn chứng từ (hợp đồng / hóa đơn / thanh toán).
 * Dùng cho dropdown chọn đơn ở form admin — thay vì bắt người dùng gõ id.
 */
public record OrderOptionResponse(
        Long orderId,
        String orderCode,
        LocalDateTime orderDate,
        OrderStatus status,
        String customerName,
        Double totalAmount,
        Double paidAmount,
        Double remainingAmount,
        boolean hasContract,
        boolean hasInvoice
) {
}
