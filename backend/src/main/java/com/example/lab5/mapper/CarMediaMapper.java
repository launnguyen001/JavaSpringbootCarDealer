package com.example.lab5.mapper;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;
import com.example.lab5.dto.CarMediaResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class CarMediaMapper {

    public CarMedia toEntity(CarMediaRequest request) {
        if (request == null) {
            return null;
        }
        CarMedia media = new CarMedia();
        media.setCarId(request.carId());
        media.setThumbnail(request.thumbnail());
        media.setImages(request.images() == null ? new ArrayList<>() : request.images());
        media.setVideos(request.videos() == null ? new ArrayList<>() : request.videos());
        media.setUpdatedAt(LocalDateTime.now());
        return media;
    }

    public CarMediaResponse toResponse(CarMedia media, String carName) {
        return new CarMediaResponse(
                media.getId(),
                media.getCarId(),
                carName,
                media.getThumbnail(),
                media.getImages(),
                media.getVideos(),
                media.getUpdatedAt()
        );
    }
}
