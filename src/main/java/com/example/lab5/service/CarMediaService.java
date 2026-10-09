package com.example.lab5.service;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;

public interface CarMediaService {

    CarMedia save(CarMediaRequest request);

    CarMedia getByCarId(Long carId);

    void deleteByCarId(Long carId);
}
