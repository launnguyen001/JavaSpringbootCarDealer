package com.example.lab5.controller;

import com.example.lab5.document.CarReview;
import com.example.lab5.dto.CarReviewRequest;
import com.example.lab5.dto.CarReviewResponse;
import com.example.lab5.dto.CarReviewSummary;
import com.example.lab5.mapper.CarReviewMapper;
import com.example.lab5.service.CarReviewService;
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

// NoSQL (MongoDB): đánh giá / bình luận xe
@RestController
@RequestMapping("/api/car-reviews")
public class CarReviewController {

    private final CarReviewService service;
    private final CarReviewMapper mapper;

    public CarReviewController(CarReviewService service, CarReviewMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarReviewResponse create(@Valid @RequestBody CarReviewRequest request) {
        CarReview saved = service.create(request);
        return mapper.toResponse(saved, service.getCarName(saved.getCarId()));
    }

    @GetMapping
    public List<CarReviewResponse> getAll() {
        return service.getAll().stream()
                .map(r -> mapper.toResponse(r, service.getCarName(r.getCarId())))
                .toList();
    }

    @GetMapping("/car/{carId}")
    public List<CarReviewResponse> getByCar(@PathVariable Long carId) {
        return service.getByCar(carId).stream()
                .map(r -> mapper.toResponse(r, service.getCarName(r.getCarId())))
                .toList();
    }

    @PutMapping("/{id}")
    public CarReviewResponse update(
            @PathVariable String id,
            @Valid @RequestBody CarReviewRequest request
    ) {
        CarReview updated = service.update(id, request);
        return mapper.toResponse(updated, service.getCarName(updated.getCarId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/car/{carId}/summary")
    public CarReviewSummary getSummary(@PathVariable Long carId) {
        return service.getSummary(carId);
    }

    @PostMapping("/{reviewId}/replies")
    public CarReviewResponse addReply(
            @PathVariable String reviewId,
            @RequestParam String author,
            @RequestParam String content
    ) {
        CarReview review = service.addReply(reviewId, author, content);
        return mapper.toResponse(review, service.getCarName(review.getCarId()));
    }
}
