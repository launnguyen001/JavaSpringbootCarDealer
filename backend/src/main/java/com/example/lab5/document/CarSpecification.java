package com.example.lab5.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "car_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarSpecification {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long carId;

    private String engine;

    private String transmission;

    private String fuelType;

    private Integer seats;

    private String dimensions;

    private Double weightKg;

    private List<String> safetyFeatures = new ArrayList<>();

    private Map<String, Object> extra = new HashMap<>();

    private LocalDateTime updatedAt;
}
