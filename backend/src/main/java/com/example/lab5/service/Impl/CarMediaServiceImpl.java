package com.example.lab5.service.Impl;

import com.example.lab5.document.CarMedia;
import com.example.lab5.dto.CarMediaRequest;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarMediaMapper;
import com.example.lab5.mongorepository.CarMediaRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.service.CarMediaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarMediaServiceImpl implements CarMediaService {

    private final CarMediaRepository repository;
    private final CarMediaMapper mapper;
    private final CarRepository carRepository;

    public CarMediaServiceImpl(CarMediaRepository repository, CarMediaMapper mapper, CarRepository carRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.carRepository = carRepository;
    }

    /**
     * Lấy tên xe từ SQL (carRepository). Nếu xe không tồn tại hoặc không đọc được thì trả null.
     */
    public String getCarName(Long carId) {
        try {
            return carRepository.findById(carId).map(car -> car.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
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
    public List<CarMedia> getAll() {
        return repository.findAll();
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
