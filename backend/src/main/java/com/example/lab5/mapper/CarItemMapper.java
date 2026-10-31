package com.example.lab5.mapper;

import com.example.lab5.dto.CarItemRequest;
import com.example.lab5.dto.CarItemResponse;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarStatus;
import org.springframework.stereotype.Component;

@Component
public class CarItemMapper {

    /** Map một chiếc xe; tự lấy quan hệ car -> model -> brand (lazy, nhờ OSIV). */
    public CarItemResponse toResponse(CarItem item) {
        return toResponse(item, item == null ? null : item.getCar());
    }

    /** Map một chiếc xe khi đã có sẵn dòng xe (tránh truy vấn thêm). */
    public CarItemResponse toResponse(CarItem item, Car car) {
        if (item == null) {
            return null;
        }
        Long modelId = null;
        String modelName = null;
        String brandName = null;
        if (car != null && car.getModel() != null) {
            modelId = car.getModel().getId();
            modelName = car.getModel().getName();
            if (car.getModel().getBrand() != null) {
                brandName = car.getModel().getBrand().getName();
            }
        }
        return new CarItemResponse(
                item.getId(),
                car == null ? null : car.getId(),
                car == null ? null : car.getName(),
                item.getVin(),
                item.getColor(),
                item.getYear(),
                item.getCondition(),
                item.getPrice(),
                item.getStatus(),
                item.getMileageKm(),
                item.getNote(),
                modelId,
                modelName,
                brandName,
                car == null ? null : car.getDescription(),
                car == null ? null : car.getBasePrice(),
                car == null || car.getModel() == null ? null : car.getModel().getBodyType()
        );
    }

    public CarItem toEntity(CarItemRequest request) {
        if (request == null) {
            return null;
        }
        CarItem item = new CarItem();
        apply(request, item);
        return item;
    }

    /** Gán dữ liệu form vào entity; giữ mặc định NEW / AVAILABLE khi client bỏ trống. */
    public void apply(CarItemRequest request, CarItem item) {
        item.setVin(normalize(request.vin()));
        item.setColor(normalize(request.color()));
        item.setYear(request.year());
        item.setCondition(request.condition() == null ? CarCondition.NEW : request.condition());
        item.setPrice(request.price());
        item.setStatus(request.status() == null ? CarStatus.AVAILABLE : request.status());
        item.setMileageKm(request.mileageKm());
        item.setNote(normalize(request.note()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
