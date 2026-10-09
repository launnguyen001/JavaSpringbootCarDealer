package com.example.lab5.service;

import com.example.lab5.dto.CarResponse;
import com.example.lab5.entity.Car;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarMapper;
import com.example.lab5.repository.CarRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

// Cache kết quả tìm kiếm/chi tiết xe nóng (Redis ở production, cache in-memory khi test)
@Service
public class CarCatalogService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    public CarCatalogService(CarRepository carRepository, CarMapper carMapper) {
        this.carRepository = carRepository;
        this.carMapper = carMapper;
    }

    @Cacheable(cacheNames = "carDetail", key = "#id")
    public CarResponse getCarDetail(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe có id = " + id));
        return carMapper.toResponse(car);
    }

    @Cacheable(cacheNames = "carSearch", key = "#name.toLowerCase()")
    public List<CarResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new com.example.lab5.exception.BadRequestException(
                    "Từ khóa tên xe không được để trống");
        }
        return carRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name.trim())
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @CacheEvict(cacheNames = {"carDetail", "carSearch"}, allEntries = true)
    public void evictCatalog() {
        // xoá cache khi dữ liệu xe thay đổi
    }
}
