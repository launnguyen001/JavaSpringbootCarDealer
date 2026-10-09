package com.example.lab5.service.Impl;

import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.service.CarCatalogService;
import com.example.lab5.service.CarService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CarModelRepository carModelRepository;
    private final CarCatalogService carCatalogService;
    private final EntityManager entityManager;

    public CarServiceImpl(
            CarRepository carRepository,
            CarModelRepository carModelRepository,
            CarCatalogService carCatalogService,
            EntityManager entityManager
    ) {
        this.carRepository = carRepository;
        this.carModelRepository = carModelRepository;
        this.carCatalogService = carCatalogService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Car create(Car car) {
        validateCar(car);
        Long modelId = car.getModel().getId();
        if (!carModelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Không tìm thấy dòng xe có id = " + modelId);
        }
        car.setModel(entityManager.getReference(CarModel.class, modelId));
        car.setId(null);
        Car saved = carRepository.save(car);
        carCatalogService.evictCatalog();
        return saved;
    }

    @Override
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Override
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe có id = " + id));
    }

    @Override
    public List<Car> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Từ khóa tên xe không được để trống");
        }
        return carRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name.trim());
    }

    @Override
    public List<Car> searchByPrice(Double minPrice, Double maxPrice) {
        validatePriceRange(minPrice, maxPrice);
        return carRepository.findByPriceBetweenOrderByPriceAscIdAsc(minPrice, maxPrice);
    }

    @Override
    public List<Car> searchByMinQuantity(Integer minQuantity) {
        validateMinQuantity(minQuantity);
        return carRepository.findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(minQuantity);
    }

    @Override
    public List<Car> getByBrand(Long brandId) {
        if (brandId == null) {
            throw new BadRequestException("Hãng xe không được để trống");
        }
        return carRepository.findByModel_Brand_IdOrderByIdAsc(brandId);
    }

    @Override
    public List<Car> getByModel(Long modelId) {
        if (modelId == null) {
            throw new BadRequestException("Dòng xe không được để trống");
        }
        return carRepository.findByModel_IdOrderByIdAsc(modelId);
    }

    @Override
    public List<Car> searchByModelAndPriceAndQuantity(
            String modelName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    ) {
        if (modelName == null || modelName.isBlank()) {
            throw new BadRequestException("Tên dòng xe không được để trống");
        }
        validatePriceRange(minPrice, maxPrice);
        validateMinQuantity(minQuantity);
        return carRepository.searchByModelAndPriceAndQuantity(
                modelName.trim(), minPrice, maxPrice, minQuantity
        );
    }

    @Override
    public List<Car> searchByBrandAndPriceAndQuantity(
            String brandName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    ) {
        if (brandName == null || brandName.isBlank()) {
            throw new BadRequestException("Tên hãng xe không được để trống");
        }
        validatePriceRange(minPrice, maxPrice);
        validateMinQuantity(minQuantity);
        return carRepository.searchByBrandAndPriceAndQuantity(
                brandName.trim(), minPrice, maxPrice, minQuantity
        );
    }

    private void validatePriceRange(Double minPrice, Double maxPrice) {
        if (minPrice == null || maxPrice == null
                || !Double.isFinite(minPrice) || !Double.isFinite(maxPrice)
                || minPrice < 0 || maxPrice < minPrice) {
            throw new BadRequestException("Khoảng giá không hợp lệ");
        }
    }

    private void validateMinQuantity(Integer minQuantity) {
        if (minQuantity == null || minQuantity < 0) {
            throw new BadRequestException("Số lượng tối thiểu không hợp lệ");
        }
    }

    private void validateCar(Car car) {
        if (car == null || car.getName() == null || car.getName().isBlank()) {
            throw new BadRequestException("Tên xe không được để trống");
        }
        if (car.getPrice() == null || car.getPrice() < 0) {
            throw new BadRequestException("Giá xe không hợp lệ");
        }
        if (car.getQuantity() == null || car.getQuantity() < 0) {
            throw new BadRequestException("Số lượng xe không hợp lệ");
        }
        if (car.getModel() == null || car.getModel().getId() == null) {
            throw new BadRequestException("Dòng xe không được để trống");
        }
    }
}
