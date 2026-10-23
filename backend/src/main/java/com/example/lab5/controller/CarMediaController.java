package com.example.lab5.controller;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;
import com.example.lab5.dto.CarMediaResponse;
import com.example.lab5.mapper.CarMediaMapper;
import com.example.lab5.service.CarMediaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// NoSQL (MongoDB): hình ảnh / video của xe
@RestController
@RequestMapping("/api/car-media")
public class CarMediaController {

    private final CarMediaService service;
    private final CarMediaMapper mapper;

    public CarMediaController(CarMediaService service, CarMediaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PutMapping
    public CarMediaResponse save(@Valid @RequestBody CarMediaRequest request) {
        CarMedia saved = service.save(request);
        return mapper.toResponse(saved, service.getCarName(saved.getCarId()));
    }

    @GetMapping
    public List<CarMediaResponse> getAll() {
        return service.getAll().stream()
                .map(m -> mapper.toResponse(m, service.getCarName(m.getCarId())))
                .toList();
    }

    @GetMapping("/{carId}")
    public CarMediaResponse getByCarId(@PathVariable Long carId) {
        return mapper.toResponse(service.getByCarId(carId), service.getCarName(carId));
    }

    @DeleteMapping("/{carId}")
    public void deleteByCarId(@PathVariable Long carId) {
        service.deleteByCarId(carId);
    }
}
