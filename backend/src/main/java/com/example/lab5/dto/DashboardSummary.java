package com.example.lab5.dto;

/**
 * Chỉ số tổng hợp cho dashboard.
 *
 * <p>Các chỉ số doanh thu được tách theo cùng định nghĩa nghiệp vụ:
 * đơn PENDING, đơn CONFIRMED/COMPLETED và tiền đã thu từ Payments.
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
        double totalRevenue,
        double pendingRevenue,
        double recognizedRevenue,
        double actualRevenue
) {
}
