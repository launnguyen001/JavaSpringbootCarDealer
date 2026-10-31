package com.example.lab5.controller;

import com.example.lab5.dto.CarItemDetailResponse;
import com.example.lab5.service.CarItemDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chi tiết một chiếc xe vật lý theo car_items.id, kèm thông số và chứng từ liên quan. */
@RestController
@RequestMapping("/api/car-items")
public class CarItemDetailController {

    private final CarItemDetailService detailService;

    public CarItemDetailController(CarItemDetailService detailService) {
        this.detailService = detailService;
    }

    @GetMapping("/{id}/detail")
    public CarItemDetailResponse getDetail(@PathVariable Long id) {
        return detailService.getDetail(id);
    }
}
