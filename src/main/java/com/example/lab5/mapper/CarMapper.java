package com.example.lab5.mapper;

import com.example.lab5.dto.CarRequest;
import com.example.lab5.dto.CarResponse;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    public Car toEntity(CarRequest request) {
        if (request == null) {
            return null;
        }
        CarModel model = null;
        if (request.modelId() != null) {
            model = new CarModel();
            model.setId(request.modelId());
        }
        Car car = new Car();
        car.setName(request.name());
        car.setDescription(request.description());
        car.setPrice(request.price());
        car.setQuantity(request.quantity());
        car.setVin(request.vin());
        car.setYear(request.year());
        car.setColor(request.color());
        car.setStatus(request.status() == null ? CarStatus.AVAILABLE : request.status());
        car.setModel(model);
        return car;
    }

    public CarResponse toResponse(Car car) {
        Long modelId = null;
        String modelName = null;
        String brandName = null;
        if (car.getModel() != null) {
            modelId = car.getModel().getId();
            modelName = car.getModel().getName();
            if (car.getModel().getBrand() != null) {
                brandName = car.getModel().getBrand().getName();
            }
        }
        return new CarResponse(
                car.getId(),
                car.getName(),
                car.getDescription(),
                car.getPrice(),
                car.getQuantity(),
                car.getVin(),
                car.getYear(),
                car.getColor(),
                car.getStatus(),
                modelId,
                modelName,
                brandName
        );
    }
}
