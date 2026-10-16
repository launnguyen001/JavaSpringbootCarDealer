package com.example.lab5.controller;

import com.example.lab5.dto.CarInventoryItem;
import com.example.lab5.dto.CarSalesOrderLine;
import com.example.lab5.dto.DashboardSummary;
import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.InventoryByModel;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.OrderSummary;
import com.example.lab5.dto.RevenueByMonth;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import com.example.lab5.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Báo cáo/thống kê (SQL thuần) - chỉ ADMIN truy cập (xem SecurityConfig)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/top-selling-cars")
    public List<TopSellingCar> getTopSellingCars(@RequestParam(defaultValue = "5") int limit) {
        return reportService.getTopSellingCars(limit);
    }

    @GetMapping("/inventory-by-brand")
    public List<InventoryByBrand> getInventoryByBrand() {
        return reportService.getInventoryByBrand();
    }

    @GetMapping("/revenue-summary")
    public RevenueSummary getRevenueSummary() {
        return reportService.getRevenueSummary();
    }

    @GetMapping("/orders-by-status")
    public List<OrderStatusCount> getOrdersByStatus() {
        return reportService.getOrderStatusCount();
    }

    // ------------------- Dashboard & drilldown -------------------

    @GetMapping("/dashboard-summary")
    public DashboardSummary getDashboardSummary() {
        return reportService.getDashboardSummary();
    }

    @GetMapping("/inventory-by-brand/{brandId}/models")
    public List<InventoryByModel> getModelsByBrand(@PathVariable Long brandId) {
        return reportService.getModelsByBrand(brandId);
    }

    @GetMapping("/inventory-by-model/{modelId}/cars")
    public List<CarInventoryItem> getCarsByModel(@PathVariable Long modelId) {
        return reportService.getCarsByModel(modelId);
    }

    @GetMapping("/top-selling-cars/{carId}/orders")
    public List<CarSalesOrderLine> getOrdersByCar(@PathVariable Long carId) {
        return reportService.getOrdersByCar(carId);
    }

    @GetMapping("/orders-by-status/{status}")
    public List<OrderSummary> getOrdersByStatus(@PathVariable String status) {
        return reportService.getOrdersByStatus(status);
    }

    @GetMapping("/revenue-by-month")
    public List<RevenueByMonth> getRevenueByMonth() {
        return reportService.getRevenueByMonth();
    }
}
