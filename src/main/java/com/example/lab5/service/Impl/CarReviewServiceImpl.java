package com.example.lab5.service.Impl;

import com.example.lab5.document.CarReview;
import com.example.lab5.dto.CarReviewRequest;
import com.example.lab5.dto.CarReviewSummary;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarReviewMapper;
import com.example.lab5.mongorepository.CarReviewRepository;
import com.example.lab5.service.CarReviewService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarReviewServiceImpl implements CarReviewService {

    private final CarReviewRepository repository;
    private final CarReviewMapper mapper;

    public CarReviewServiceImpl(CarReviewRepository repository, CarReviewMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CarReview create(CarReviewRequest request) {
        if (request == null || request.carId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new BadRequestException("Điểm đánh giá phải từ 1 đến 5");
        }
        return repository.save(mapper.toEntity(request));
    }

    @Override
    public List<CarReview> getByCar(Long carId) {
        if (carId == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        return repository.findByCarIdOrderByCreatedAtDesc(carId);
    }

    @Override
    public CarReview addReply(String reviewId, String author, String content) {
        if (author == null || author.isBlank()) {
            throw new BadRequestException("Người phản hồi không được để trống");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Nội dung phản hồi không được để trống");
        }
        CarReview review = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đánh giá có id = " + reviewId));
        review.getReplies().add(new CarReview.ReviewReply(author, content, LocalDateTime.now()));
        return repository.save(review);
    }

    @Override
    public CarReviewSummary getSummary(Long carId) {
        List<CarReview> reviews = getByCar(carId);
        long total = reviews.size();
        double average = reviews.stream()
                .mapToInt(CarReview::getRating)
                .average()
                .orElse(0.0);
        double rounded = Math.round(average * 100.0) / 100.0;
        return new CarReviewSummary(carId, total, rounded);
    }
}
