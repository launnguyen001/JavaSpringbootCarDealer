package com.example.lab5.dto;

public record DashboardSummary(
        long totalCars,
        long availableCars,
        long reservedCars,
        long soldCars,
        long maintenanceCars,
        long totalCustomers,
        long totalEmployees,
        long totalOrders,
        long scheduledTestDrives,
        double totalRevenue
) {
}
