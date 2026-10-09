package com.example.lab5.mapper;

import com.example.lab5.document.CarReview;
import com.example.lab5.dto.CarReviewRequest;
import com.example.lab5.dto.CarReviewResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class CarReviewMapper {

    public CarReview toEntity(CarReviewRequest request) {
        if (request == null) {
            return null;
        }
        CarReview review = new CarReview();
        review.setCarId(request.carId());
        review.setCustomerId(request.customerId());
        review.setCustomerName(request.customerName());
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setContent(request.content());
        review.setCreatedAt(LocalDateTime.now());
        review.setReplies(new ArrayList<>());
        return review;
    }

    public CarReviewResponse toResponse(CarReview review) {
        return new CarReviewResponse(
                review.getId(),
                review.getCarId(),
                review.getCustomerId(),
                review.getCustomerName(),
                review.getRating(),
                review.getTitle(),
                review.getContent(),
                review.getCreatedAt(),
                review.getReplies() == null
                        ? java.util.List.of()
                        : review.getReplies().stream()
                                .map(reply -> new CarReviewResponse.Reply(
                                        reply.getAuthor(),
                                        reply.getContent(),
                                        reply.getCreatedAt()))
                                .toList()
        );
    }
}
