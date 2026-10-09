package com.example.lab5.mapper;

import com.example.lab5.dto.CarModelRequest;
import com.example.lab5.dto.CarModelResponse;
import com.example.lab5.entity.Brand;
import com.example.lab5.entity.CarModel;
import org.springframework.stereotype.Component;

@Component
public class CarModelMapper {

    public CarModel toEntity(CarModelRequest request) {
        if (request == null) {
            return null;
        }
        Brand brand = null;
        if (request.brandId() != null) {
            brand = new Brand();
            brand.setId(request.brandId());
        }
        return new CarModel(
                null,
                request.name(),
                request.bodyType(),
                request.description(),
                brand
        );
    }

    public CarModelResponse toResponse(CarModel model) {
        return new CarModelResponse(
                model.getId(),
                model.getName(),
                model.getBodyType(),
                model.getDescription(),
                model.getBrand() == null ? null : model.getBrand().getId(),
                model.getBrand() == null ? null : model.getBrand().getName()
        );
    }
}
