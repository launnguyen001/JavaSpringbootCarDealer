package com.example.lab5.service.Impl;

import com.example.lab5.document.CarSpecification;
import com.example.lab5.dto.CarSpecificationRequest;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarSpecificationMapper;
import com.example.lab5.mongorepository.CarSpecificationRepository;
import com.example.lab5.service.CarSpecificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CarSpecificationServiceImpl implements CarSpecificationService {

    private final CarSpecificationRepository repository;
    private final CarSpecificationMapper mapper;

    public CarSpecificationServiceImpl(
            CarSpecificationRepository repository,
            CarSpecificationMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CarSpecification save(CarSpecificationRequest request) {
        if (request == null || request.carId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        CarSpecification spec = mapper.toEntity(request);
        repository.findByCarId(request.carId()).ifPresent(existing -> spec.setId(existing.getId()));
        spec.setUpdatedAt(LocalDateTime.now());
        return repository.save(spec);
    }

    @Override
    public CarSpecification getByCarId(Long carId) {
        return repository.findByCarId(carId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông số kỹ thuật của xe id = " + carId));
    }

    @Override
    public void deleteByCarId(Long carId) {
        repository.deleteByCarId(carId);
    }
}
