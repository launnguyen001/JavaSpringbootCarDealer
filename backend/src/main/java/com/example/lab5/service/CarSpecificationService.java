package com.example.lab5.service;

import com.example.lab5.document.CarSpecification;
import com.example.lab5.dto.CarSpecificationRequest;

import java.util.List;

public interface CarSpecificationService {

    CarSpecification save(CarSpecificationRequest request);

    List<CarSpecification> getAll();

    CarSpecification getByCarId(Long carId);

    void deleteByCarId(Long carId);

    /** Lấy tên xe từ SQL theo id (dùng hiển thị ở giao diện quản trị). */
    String getCarName(Long carId);
}
