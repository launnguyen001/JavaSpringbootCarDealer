package com.example.lab5.dto;

/**
 * Chỉ số tổng hợp cho dashboard.
 *
 * <p>Sau khi tách bảng, các chỉ số "xe" đếm theo từng chiếc vật lý ({@code car_items}),
 * kèm số xe mới ({@code newItems}) và xe cũ ({@code usedItems}).
 */
public record DashboardSummary(
        long totalCars,
        long availableCars,
        long reservedCars,
        long soldCars,
        long maintenanceCars,
        long newItems,
        long usedItems,
        long totalCustomers,
        long totalEmployees,
        long totalOrders,
        long scheduledTestDrives,
        double totalRevenue
) {
}
