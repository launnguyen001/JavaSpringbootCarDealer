package com.example.lab5.mongorepository;

import com.example.lab5.document.CarReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CarReviewRepository extends MongoRepository<CarReview, String> {

    List<CarReview> findByCarIdOrderByCreatedAtDesc(Long carId);

    List<CarReview> findByCarIdAndRatingGreaterThanEqualOrderByCreatedAtDesc(Long carId, Integer rating);

    long countByCarId(Long carId);
}
