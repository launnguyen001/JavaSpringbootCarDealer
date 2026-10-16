package com.example.lab5.controller;

import com.example.lab5.dto.CarItemRequest;
import com.example.lab5.dto.CarItemResponse;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.mapper.CarItemMapper;
import com.example.lab5.service.CarItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API quản lý từng chiếc xe vật lý (car_items).
 * Đọc công khai (GET), ghi chỉ ADMIN (xem SecurityConfig).
 */
@RestController
@RequestMapping("/api/car-items")
public class CarItemController {

    private final CarItemService carItemService;
    private final CarItemMapper carItemMapper;

    public CarItemController(CarItemService carItemService, CarItemMapper carItemMapper) {
        this.carItemService = carItemService;
        this.carItemMapper = carItemMapper;
    }

    @GetMapping
    public List<CarItemResponse> getByCar(@RequestParam Long carId) {
        return carItemService.getByCar(carId).stream().map(carItemMapper::toResponse).toList();
    }

    @GetMapping("/available")
    public List<CarItemResponse> getAvailableByCar(@RequestParam Long carId) {
        return carItemService.getAvailableByCar(carId).stream().map(carItemMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CarItemResponse getById(@PathVariable Long id) {
        return carItemMapper.toResponse(carItemService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarItemResponse create(
            @RequestParam Long carId,
            @Valid @RequestBody CarItemRequest request
    ) {
        return carItemMapper.toResponse(carItemService.create(carId, carItemMapper.toEntity(request)));
    }

    @PutMapping("/{id}")
    public CarItemResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CarItemRequest request
    ) {
        return carItemMapper.toResponse(carItemService.update(id, carItemMapper.toEntity(request)));
    }

    @PatchMapping("/{id}/status")
    public CarItemResponse updateStatus(
            @PathVariable Long id,
            @RequestParam CarStatus status
    ) {
        return carItemMapper.toResponse(carItemService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        carItemService.delete(id);
    }
}
