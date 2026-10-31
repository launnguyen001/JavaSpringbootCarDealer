package com.example.lab5.service.Impl;

import com.example.lab5.dto.CarInventoryItem;
import com.example.lab5.dto.CarSalesOrderLine;
import com.example.lab5.dto.DashboardSummary;
import com.example.lab5.dto.DashboardData;
import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.InventoryByModel;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.OrderSummary;
import com.example.lab5.dto.RevenueByMonth;
import com.example.lab5.dto.RevenueOrderSummary;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.JdbcReportRepository;
import com.example.lab5.repository.JdbcDashboardRepository;
import com.example.lab5.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class ReportServiceImpl implements ReportService {

    private final JdbcReportRepository jdbcReportRepository;
    private final JdbcDashboardRepository jdbcDashboardRepository;
    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CarRepository carRepository;

    public ReportServiceImpl(
            JdbcReportRepository jdbcReportRepository,
            JdbcDashboardRepository jdbcDashboardRepository,
            BrandRepository brandRepository,
            CarModelRepository carModelRepository,
            CarRepository carRepository
    ) {
        this.jdbcReportRepository = jdbcReportRepository;
        this.jdbcDashboardRepository = jdbcDashboardRepository;
        this.brandRepository = brandRepository;
        this.carModelRepository = carModelRepository;
        this.carRepository = carRepository;
    }

    @Override
    public List<TopSellingCar> getTopSellingCars(int limit) {
        if (limit <= 0) {
            throw new BadRequestException("Số lượng bản ghi phải lớn hơn 0");
        }
        return jdbcReportRepository.findTopSellingCars(limit);
    }

    @Override
    public List<InventoryByBrand> getInventoryByBrand() {
        return jdbcReportRepository.findInventoryByBrand();
    }

    @Override
    public RevenueSummary getRevenueSummary() {
        return jdbcReportRepository.findRevenueSummary();
    }

    @Override
    public List<OrderStatusCount> getOrderStatusCount() {
        return jdbcReportRepository.countOrdersByStatus();
    }

    // ---- Dashboard & drilldown ----

    @Override
    public DashboardSummary getDashboardSummary() {
        return jdbcReportRepository.findDashboardSummary();
    }

    @Override
    public DashboardData getDashboard(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Khoảng ngày báo cáo không hợp lệ");
        }
        if (brandId != null && !brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Không tìm thấy hãng xe id = " + brandId);
        }
        if (modelId != null && !carModelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Không tìm thấy dòng xe id = " + modelId);
        }
        return jdbcDashboardRepository.findDashboard(from, to, brandId, modelId);
    }

    @Override
    public List<CarInventoryItem> getInventoryCars(String status, LocalDate from, LocalDate to, Long brandId, Long modelId) {
        if (status != null && !List.of("IN_STOCK", "AVAILABLE", "RESERVED", "SOLD", "SOLD_ALL", "MAINTENANCE").contains(status)) {
            throw new BadRequestException("Trạng thái xe không hợp lệ: " + status);
        }
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Khoảng ngày báo cáo không hợp lệ");
        }
        return jdbcDashboardRepository.findInventoryCars(status, from, to, brandId, modelId);
    }

    @Override
    public List<InventoryByModel> getModelsByBrand(Long brandId) {
        if (brandId == null || !brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Không tìm thấy hãng xe id = " + brandId);
        }
        return jdbcReportRepository.findModelsByBrand(brandId);
    }

    @Override
    public List<CarInventoryItem> getCarsByModel(Long modelId) {
        if (modelId == null || !carModelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Không tìm thấy dòng xe id = " + modelId);
        }
        return jdbcReportRepository.findCarsByModel(modelId);
    }

    @Override
    public List<CarSalesOrderLine> getOrdersByCar(Long carId) {
        if (carId == null || !carRepository.existsById(carId)) {
            throw new ResourceNotFoundException("Không tìm thấy xe id = " + carId);
        }
        return jdbcReportRepository.findOrdersByCar(carId);
    }

    @Override
    public List<OrderSummary> getOrdersByStatus(String status) {
        return getOrdersByStatus(status, null, null, null, null);
    }

    @Override
    public List<OrderSummary> getOrdersByStatus(String status, LocalDate from, LocalDate to, Long brandId, Long modelId) {
        if (status == null) throw new BadRequestException("Trạng thái đơn hàng không được để trống");
        String normalized = status.trim().toUpperCase();
        try {
            OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
        if ((from == null) != (to == null) || (from != null && to.isBefore(from)))
            throw new BadRequestException("Khoảng ngày báo cáo không hợp lệ");
        if (brandId != null && !brandRepository.existsById(brandId))
            throw new ResourceNotFoundException("Không tìm thấy hãng xe id = " + brandId);
        if (modelId != null && !carModelRepository.existsById(modelId))
            throw new ResourceNotFoundException("Không tìm thấy dòng xe id = " + modelId);
        if (from == null) return jdbcReportRepository.findOrdersByStatus(normalized);
        return jdbcReportRepository.findOrdersByStatus(normalized, from, to, brandId, modelId);
    }

    @Override
    public List<RevenueOrderSummary> getOrdersByRevenue(YearMonth period, String category, Long brandId, Long modelId) {
        if (period == null) throw new BadRequestException("Tháng doanh thu không hợp lệ");
        if (category == null || !List.of("PENDING", "RECOGNIZED", "ACTUAL").contains(category.trim().toUpperCase())) {
            throw new BadRequestException("Nhóm doanh thu không hợp lệ");
        }
        if (brandId != null && !brandRepository.existsById(brandId))
            throw new ResourceNotFoundException("Không tìm thấy hãng xe id = " + brandId);
        if (modelId != null && !carModelRepository.existsById(modelId))
            throw new ResourceNotFoundException("Không tìm thấy dòng xe id = " + modelId);
        return jdbcDashboardRepository.findOrdersByRevenue(period, category.trim().toUpperCase(), brandId, modelId);
    }

    @Override
    public List<RevenueByMonth> getRevenueByMonth() {
        return jdbcReportRepository.findRevenueByMonth();
    }
}
