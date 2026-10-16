package com.example.lab5.service.Impl;

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
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.JdbcReportRepository;
import com.example.lab5.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final JdbcReportRepository jdbcReportRepository;
    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CarRepository carRepository;

    public ReportServiceImpl(
            JdbcReportRepository jdbcReportRepository,
            BrandRepository brandRepository,
            CarModelRepository carModelRepository,
            CarRepository carRepository
    ) {
        this.jdbcReportRepository = jdbcReportRepository;
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
        if (status == null) {
            throw new BadRequestException("Trạng thái đơn hàng không được để trống");
        }
        try {
            OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
        return jdbcReportRepository.findOrdersByStatus(status.trim().toUpperCase());
    }

    @Override
    public List<RevenueByMonth> getRevenueByMonth() {
        return jdbcReportRepository.findRevenueByMonth();
    }
}
