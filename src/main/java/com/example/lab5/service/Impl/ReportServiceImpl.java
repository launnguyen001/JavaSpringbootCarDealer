package com.example.lab5.service.Impl;

import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.repository.JdbcReportRepository;
import com.example.lab5.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final JdbcReportRepository jdbcReportRepository;

    public ReportServiceImpl(JdbcReportRepository jdbcReportRepository) {
        this.jdbcReportRepository = jdbcReportRepository;
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
}
