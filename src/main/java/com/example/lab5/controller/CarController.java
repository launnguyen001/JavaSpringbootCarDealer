package com.example.lab5.controller;

import com.example.lab5.dto.CarRequest;
import com.example.lab5.dto.CarResponse;
import com.example.lab5.entity.Car;
import com.example.lab5.mapper.CarMapper;
import com.example.lab5.service.CarCatalogService;
import com.example.lab5.service.CarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;
    private final CarCatalogService carCatalogService;
    private final CarMapper carMapper;

    public CarController(
            CarService carService,
            CarCatalogService carCatalogService,
            CarMapper carMapper
    ) {
        this.carService = carService;
        this.carCatalogService = carCatalogService;
        this.carMapper = carMapper;
    }

    // Chức năng 1: Xem danh sách xe
    @GetMapping
    public List<CarResponse> getAllCars() {
        return toResponses(carService.getAllCars());
    }

    // Chức năng 2: Thêm xe
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse addCar(@Valid @RequestBody CarRequest request) {
        return carMapper.toResponse(carService.create(carMapper.toEntity(request)));
    }

    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carCatalogService.getCarDetail(id);
    }

    // Chức năng: Cập nhật xe
    @PutMapping("/{id}")
    public CarResponse updateCar(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        return carMapper.toResponse(carService.update(id, carMapper.toEntity(request)));
    }

    // Chức năng: Xoá xe
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCar(@PathVariable Long id) {
        carService.delete(id);
    }

    @GetMapping("/search/name")
    public List<CarResponse> searchByName(@RequestParam String name) {
        return carCatalogService.searchByName(name);
    }

    @GetMapping("/search/price")
    public List<CarResponse> searchByPrice(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice
    ) {
        return toResponses(carService.searchByPrice(minPrice, maxPrice));
    }

    @GetMapping("/search/stock")
    public List<CarResponse> searchByMinQuantity(@RequestParam Integer minQuantity) {
        return toResponses(carService.searchByMinQuantity(minQuantity));
    }

    @GetMapping("/search/advanced")
    public List<CarResponse> searchAdvanced(
            @RequestParam String modelName,
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice,
            @RequestParam Integer minQuantity
    ) {
        return toResponses(carService.searchByModelAndPriceAndQuantity(
                modelName, minPrice, maxPrice, minQuantity
        ));
    }

    @GetMapping("/search/advanced/brand")
    public List<CarResponse> searchAdvancedByBrand(
            @RequestParam String brandName,
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice,
            @RequestParam Integer minQuantity
    ) {
        return toResponses(carService.searchByBrandAndPriceAndQuantity(
                brandName, minPrice, maxPrice, minQuantity
        ));
    }

    @GetMapping("/brand/{brandId}")
    public List<CarResponse> getByBrand(@PathVariable Long brandId) {
        return toResponses(carService.getByBrand(brandId));
    }

    @GetMapping("/model/{modelId}")
    public List<CarResponse> getByModel(@PathVariable Long modelId) {
        return toResponses(carService.getByModel(modelId));
    }

    private List<CarResponse> toResponses(List<Car> cars) {
        return cars.stream().map(carMapper::toResponse).toList();
    }
}
