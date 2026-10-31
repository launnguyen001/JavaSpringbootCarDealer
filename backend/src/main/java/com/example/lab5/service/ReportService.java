package com.example.lab5.service;

import com.example.lab5.dto.CarInventoryItem;
import com.example.lab5.dto.CarSalesOrderLine;
import com.example.lab5.dto.DashboardSummary;
import com.example.lab5.dto.DashboardData;
import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.InventoryByModel;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.OrderSummary;
import com.example.lab5.dto.RevenueByMonth;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;

import java.util.List;
import java.time.LocalDate;

public interface ReportService {

    List<TopSellingCar> getTopSellingCars(int limit);

    List<InventoryByBrand> getInventoryByBrand();

    RevenueSummary getRevenueSummary();

    List<OrderStatusCount> getOrderStatusCount();

    // ---- Dashboard & drilldown ----

    DashboardSummary getDashboardSummary();
    DashboardData getDashboard(LocalDate from, LocalDate to, Long brandId, Long modelId);
    List<CarInventoryItem> getInventoryCars(String status, LocalDate from, LocalDate to, Long brandId, Long modelId);

    List<InventoryByModel> getModelsByBrand(Long brandId);

    List<CarInventoryItem> getCarsByModel(Long modelId);

    List<CarSalesOrderLine> getOrdersByCar(Long carId);

    List<OrderSummary> getOrdersByStatus(String status);
    List<OrderSummary> getOrdersByStatus(String status, LocalDate from, LocalDate to, Long brandId, Long modelId);

    List<RevenueByMonth> getRevenueByMonth();
}
