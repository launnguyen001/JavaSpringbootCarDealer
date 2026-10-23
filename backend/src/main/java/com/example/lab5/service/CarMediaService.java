package com.example.lab5.service;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;

import java.util.List;

public interface CarMediaService {

    CarMedia save(CarMediaRequest request);

    List<CarMedia> getAll();

    CarMedia getByCarId(Long carId);

    void deleteByCarId(Long carId);

    /** Lấy tên xe từ SQL theo id (dùng hiển thị ở giao diện quản trị). */
    String getCarName(Long carId);
}
