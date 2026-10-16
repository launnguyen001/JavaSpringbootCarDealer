package com.example.lab5.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "car_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarReview {

    @Id
    private String id;

    @Indexed
    private Long carId;

    private Long customerId;

    private String customerName;

    private Integer rating;

    private String title;

    @TextIndexed
    private String content;

    private LocalDateTime createdAt;

    private List<ReviewReply> replies = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewReply {
        private String author;
        private String content;
        private LocalDateTime createdAt;
    }
}
