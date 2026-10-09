package com.example.lab5.mapper;

import com.example.lab5.document.CarSpecification;
import com.example.lab5.dto.CarSpecificationRequest;
import com.example.lab5.dto.CarSpecificationResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class CarSpecificationMapper {

    public CarSpecification toEntity(CarSpecificationRequest request) {
        if (request == null) {
            return null;
        }
        CarSpecification spec = new CarSpecification();
        spec.setCarId(request.carId());
        spec.setEngine(request.engine());
        spec.setTransmission(request.transmission());
        spec.setFuelType(request.fuelType());
        spec.setSeats(request.seats());
        spec.setDimensions(request.dimensions());
        spec.setWeightKg(request.weightKg());
        spec.setSafetyFeatures(request.safetyFeatures() == null ? new ArrayList<>() : request.safetyFeatures());
        spec.setExtra(request.extra() == null ? new HashMap<>() : request.extra());
        spec.setUpdatedAt(LocalDateTime.now());
        return spec;
    }

    public CarSpecificationResponse toResponse(CarSpecification spec) {
        return new CarSpecificationResponse(
                spec.getId(),
                spec.getCarId(),
                spec.getEngine(),
                spec.getTransmission(),
                spec.getFuelType(),
                spec.getSeats(),
                spec.getDimensions(),
                spec.getWeightKg(),
                spec.getSafetyFeatures(),
                spec.getExtra(),
                spec.getUpdatedAt()
        );
    }
}
