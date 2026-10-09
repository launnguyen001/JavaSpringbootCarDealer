package com.example.lab5.service;

import com.example.lab5.entity.Car;

import java.util.List;

public interface CarService {

    Car create(Car car);

    List<Car> getAllCars();

    Car getCarById(Long id);

    List<Car> searchByName(String name);

    List<Car> searchByPrice(Double minPrice, Double maxPrice);

    List<Car> searchByMinQuantity(Integer minQuantity);

    List<Car> getByBrand(Long brandId);

    List<Car> getByModel(Long modelId);

    List<Car> searchByModelAndPriceAndQuantity(
            String modelName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    );

    List<Car> searchByBrandAndPriceAndQuantity(
            String brandName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    );
}
