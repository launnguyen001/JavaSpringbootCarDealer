package com.example.lab5.service;

import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;

import java.util.List;

public interface ReportService {

    List<TopSellingCar> getTopSellingCars(int limit);

    List<InventoryByBrand> getInventoryByBrand();

    RevenueSummary getRevenueSummary();

    List<OrderStatusCount> getOrderStatusCount();
}
