package com.example.lab5.service;

import com.example.lab5.document.CarSpecification;
import com.example.lab5.dto.CarSpecificationRequest;

public interface CarSpecificationService {

    CarSpecification save(CarSpecificationRequest request);

    CarSpecification getByCarId(Long carId);

    void deleteByCarId(Long carId);
}
