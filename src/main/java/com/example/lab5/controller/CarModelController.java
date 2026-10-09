package com.example.lab5.controller;

import com.example.lab5.dto.CarModelRequest;
import com.example.lab5.dto.CarModelResponse;
import com.example.lab5.mapper.CarModelMapper;
import com.example.lab5.service.CarModelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/car-models")
public class CarModelController {

    private final CarModelService carModelService;
    private final CarModelMapper carModelMapper;

    public CarModelController(CarModelService carModelService, CarModelMapper carModelMapper) {
        this.carModelService = carModelService;
        this.carModelMapper = carModelMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarModelResponse addModel(@Valid @RequestBody CarModelRequest request) {
        return carModelMapper.toResponse(carModelService.create(carModelMapper.toEntity(request)));
    }

    @GetMapping
    public List<CarModelResponse> getAllModels() {
        return toResponses(carModelService.getAllModels());
    }

    @GetMapping("/{id}")
    public CarModelResponse getModelById(@PathVariable Long id) {
        return carModelMapper.toResponse(carModelService.getModelById(id));
    }

    @GetMapping("/search/name")
    public List<CarModelResponse> searchByName(@RequestParam String name) {
        return toResponses(carModelService.searchByName(name));
    }

    @GetMapping("/brand/{brandId}")
    public List<CarModelResponse> getByBrand(@PathVariable Long brandId) {
        return toResponses(carModelService.getByBrand(brandId));
    }

    private List<CarModelResponse> toResponses(List<com.example.lab5.entity.CarModel> models) {
        return models.stream().map(carModelMapper::toResponse).toList();
    }
}
