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
import com.example.lab5.service.ReportExportService;
import com.example.lab5.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.LocalDate;
import com.example.lab5.dto.DashboardData;

// Báo cáo/thống kê (SQL thuần) - chỉ ADMIN truy cập (xem SecurityConfig)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
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

    @GetMapping("/dashboard")
    public DashboardData getDashboard(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long modelId
    ) {
        return reportService.getDashboard(from, to, brandId, modelId);
    }

    @GetMapping("/inventory-cars")
    public List<CarInventoryItem> getInventoryCars(
            @RequestParam(required = false) String status,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long modelId
    ) {
        return reportService.getInventoryCars(status, from, to, brandId, modelId);
    }

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
    public List<OrderSummary> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long modelId
    ) {
        return reportService.getOrdersByStatus(status, from, to, brandId, modelId);
    }

    @GetMapping("/revenue-by-month")
    public List<RevenueByMonth> getRevenueByMonth() {
        return reportService.getRevenueByMonth();
    }

    /**
     * Xuất báo cáo ra Excel/CSV.
     *
     * <p>Ví dụ: {@code GET /api/reports/export?type=dashboard-summary&format=xlsx}
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String type,
            @RequestParam(defaultValue = "xlsx") String format
    ) {
        byte[] content = reportExportService.export(type, format);
        String filename = reportExportService.suggestedFilename(type, format);
        boolean csv = "csv".equalsIgnoreCase(format);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(csv
                ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
