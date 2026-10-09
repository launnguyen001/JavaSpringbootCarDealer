package com.example.lab5.service.Impl;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarMediaMapper;
import com.example.lab5.mongorepository.CarMediaRepository;
import com.example.lab5.service.CarMediaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CarMediaServiceImpl implements CarMediaService {

    private final CarMediaRepository repository;
    private final CarMediaMapper mapper;

    public CarMediaServiceImpl(CarMediaRepository repository, CarMediaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CarMedia save(CarMediaRequest request) {
        if (request == null || request.carId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        CarMedia media = mapper.toEntity(request);
        repository.findByCarId(request.carId()).ifPresent(existing -> media.setId(existing.getId()));
        media.setUpdatedAt(LocalDateTime.now());
        return repository.save(media);
    }

    @Override
    public CarMedia getByCarId(Long carId) {
        return repository.findByCarId(carId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hình ảnh của xe id = " + carId));
    }

    @Override
    public void deleteByCarId(Long carId) {
        repository.findByCarId(carId).ifPresent(repository::delete);
    }
}
