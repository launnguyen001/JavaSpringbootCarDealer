package com.example.lab5.service.Impl;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.CarModel;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.service.CarModelService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarModelServiceImpl implements CarModelService {

    private final CarModelRepository carModelRepository;
    private final BrandRepository brandRepository;
    private final CarRepository carRepository;
    private final EntityManager entityManager;

    public CarModelServiceImpl(
            CarModelRepository carModelRepository,
            BrandRepository brandRepository,
            CarRepository carRepository,
            EntityManager entityManager
    ) {
        this.carModelRepository = carModelRepository;
        this.brandRepository = brandRepository;
        this.carRepository = carRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public CarModel create(CarModel model) {
        validateModel(model);
        Long brandId = model.getBrand().getId();
        if (!brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Không tìm thấy hãng xe có id = " + brandId);
        }
        model.setBrand(entityManager.getReference(Brand.class, brandId));
        model.setId(null);
        return carModelRepository.save(model);
    }

    @Override
    public List<CarModel> getAllModels() {
        return carModelRepository.findAll();
    }

    @Override
    public CarModel getModelById(Long id) {
        return carModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dòng xe có id = " + id));
    }

    @Override
    public List<CarModel> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Từ khóa tên dòng xe không được để trống");
        }
        return carModelRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name.trim());
    }

    @Override
    public List<CarModel> getByBrand(Long brandId) {
        if (brandId == null) {
            throw new BadRequestException("Hãng xe không được để trống");
        }
        return carModelRepository.findByBrand_IdOrderByIdAsc(brandId);
    }

    @Override
    @Transactional
    public CarModel update(Long id, CarModel model) {
        validateModel(model);
        CarModel existing = getModelById(id);
        Long brandId = model.getBrand().getId();
        if (!brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Không tìm thấy hãng xe có id = " + brandId);
        }
        existing.setName(model.getName());
        existing.setBodyType(model.getBodyType());
        existing.setDescription(model.getDescription());
        existing.setBrand(entityManager.getReference(Brand.class, brandId));
        return carModelRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getModelById(id);
        if (carRepository.existsByModel_Id(id)) {
            throw new BadRequestException("Không thể xoá dòng xe vì còn xe");
        }
        carModelRepository.deleteById(id);
    }

    private void validateModel(CarModel model) {
        if (model == null || model.getName() == null || model.getName().isBlank()) {
            throw new BadRequestException("Tên dòng xe không được để trống");
        }
        if (model.getName().length() > 100) {
            throw new BadRequestException("Tên dòng xe không được vượt quá 100 ký tự");
        }
        if (model.getBrand() == null || model.getBrand().getId() == null) {
            throw new BadRequestException("Hãng xe không được để trống");
        }
    }
}
