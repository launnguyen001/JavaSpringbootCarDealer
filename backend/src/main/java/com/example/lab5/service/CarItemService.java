package com.example.lab5.service;

import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarStatus;

import java.util.List;

/** Quản lý từng chiếc xe vật lý (car_items). */
public interface CarItemService {

    List<CarItem> getByCar(Long carId);

    List<CarItem> getAvailableByCar(Long carId);

    CarItem getById(Long id);

    CarItem create(Long carId, CarItem item);

    CarItem update(Long id, CarItem item);

    /** Chỉ đổi trạng thái một chiếc — không xoá mất VIN/màu/giá như update toàn bộ. */
    CarItem updateStatus(Long id, CarStatus status);

    void delete(Long id);
}
