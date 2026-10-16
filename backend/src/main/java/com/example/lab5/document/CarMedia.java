package com.example.lab5.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "car_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarMedia {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long carId;

    private String thumbnail;

    private List<String> images = new ArrayList<>();

    private List<String> videos = new ArrayList<>();

    private LocalDateTime updatedAt;
}
