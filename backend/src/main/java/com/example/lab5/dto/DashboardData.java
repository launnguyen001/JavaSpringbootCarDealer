package com.example.lab5.dto;

import java.util.List;

public record DashboardData(
        long totalCars,
        long availableCars,
        long reservedCars,
        long soldCars,
        long maintenanceCars,
        long newItems,
        long usedItems,
        long soldInPeriod,
        long previousSoldInPeriod,
        double revenue,
        double previousRevenue,
        List<OrderStatusCount> orderStatuses,
        List<RevenueByMonth> revenueByMonth,
        List<OrderSummary> recentOrders,
        List<TopSellingCar> topSellingCars
) {}
