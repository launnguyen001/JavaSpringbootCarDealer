package com.example.lab5.dto;

public record RevenueByMonth(
        String period,
        long totalOrders,
        double totalRevenue,
        double totalCollected,
        double outstandingAmount
) {
    public RevenueByMonth(String period, long totalOrders, double totalRevenue) {
        this(period, totalOrders, totalRevenue, 0.0, totalRevenue);
    }
}
