package com.example.lab5.dto;

public record RevenueSummary(
        Long totalOrders,
        Double totalRevenue,
        Double pendingRevenue,
        Double recognizedRevenue,
        Double actualRevenue
) {
}
