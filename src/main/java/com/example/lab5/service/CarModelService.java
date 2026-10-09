package com.example.lab5.service;

import com.example.lab5.entity.CarModel;

import java.util.List;

public interface CarModelService {

    CarModel create(CarModel model);

    List<CarModel> getAllModels();

    CarModel getModelById(Long id);

    List<CarModel> searchByName(String name);

    List<CarModel> getByBrand(Long brandId);
}
