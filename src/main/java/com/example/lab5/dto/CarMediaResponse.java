package com.example.lab5.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CarMediaResponse(
        String id,
        Long carId,
        String thumbnail,
        List<String> images,
        List<String> videos,
        LocalDateTime updatedAt
) {
}
