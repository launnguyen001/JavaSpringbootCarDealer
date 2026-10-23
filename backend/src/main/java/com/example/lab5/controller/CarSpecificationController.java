package com.example.lab5.controller;

import com.example.lab5.document.CarSpecification;
import com.example.lab5.dto.CarSpecificationRequest;
import com.example.lab5.dto.CarSpecificationResponse;
import com.example.lab5.mapper.CarSpecificationMapper;
import com.example.lab5.service.CarSpecificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// NoSQL (MongoDB): thông số kỹ thuật linh hoạt theo từng xe
@RestController
@RequestMapping("/api/car-specs")
public class CarSpecificationController {

    private final CarSpecificationService service;
    private final CarSpecificationMapper mapper;

    public CarSpecificationController(CarSpecificationService service, CarSpecificationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PutMapping
    public CarSpecificationResponse save(@Valid @RequestBody CarSpecificationRequest request) {
        CarSpecification saved = service.save(request);
        return mapper.toResponse(saved, service.getCarName(saved.getCarId()));
    }

    @GetMapping
    public List<CarSpecificationResponse> getAll() {
        return service.getAll().stream()
                .map(s -> mapper.toResponse(s, service.getCarName(s.getCarId())))
                .toList();
    }

    @GetMapping("/{carId}")
    public CarSpecificationResponse getByCarId(@PathVariable Long carId) {
        return mapper.toResponse(service.getByCarId(carId), service.getCarName(carId));
    }

    @DeleteMapping("/{carId}")
    public void deleteByCarId(@PathVariable Long carId) {
        service.deleteByCarId(carId);
    }
}
