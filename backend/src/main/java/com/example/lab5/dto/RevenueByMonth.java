package com.example.lab5.dto;

public record RevenueByMonth(
        String period,
        long totalOrders,
        double pendingRevenue,
        double recognizedRevenue,
        double actualRevenue,
        double outstandingAmount
) {
}
