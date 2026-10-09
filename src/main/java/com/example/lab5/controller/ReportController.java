package com.example.lab5.controller;

import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import com.example.lab5.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Báo cáo/thống kê (SQL thuần)
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
}
