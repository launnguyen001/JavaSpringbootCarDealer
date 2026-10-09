package com.example.lab5.service;

import com.example.lab5.document.CarReview;
import com.example.lab5.dto.CarReviewRequest;
import com.example.lab5.dto.CarReviewSummary;

import java.util.List;

public interface CarReviewService {

    CarReview create(CarReviewRequest request);

    List<CarReview> getByCar(Long carId);

    CarReview addReply(String reviewId, String author, String content);

    CarReviewSummary getSummary(Long carId);
}
