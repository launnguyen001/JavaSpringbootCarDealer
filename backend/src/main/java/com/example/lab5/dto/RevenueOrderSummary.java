package com.example.lab5.dto;

import java.time.LocalDateTime;

/** Đơn hàng đóng góp vào một nhóm doanh thu của tháng; revenueAmount là phần được phân bổ vào nhóm. */
public record RevenueOrderSummary(
        Long orderId,
        String orderCode,
        LocalDateTime orderDate,
        String status,
        Double totalAmount,
        Double revenueAmount,
        String customerName,
        String employeeName
) {
}
