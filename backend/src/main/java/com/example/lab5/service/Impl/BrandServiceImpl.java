package com.example.lab5.service.Impl;

import com.example.lab5.entity.Brand;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.service.BrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;

    public BrandServiceImpl(BrandRepository brandRepository, CarModelRepository carModelRepository) {
        this.brandRepository = brandRepository;
        this.carModelRepository = carModelRepository;
    }

    @Override
    public Brand create(Brand brand) {
        validateBrand(brand);
        brand.setId(null);
        return brandRepository.save(brand);
    }

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public Brand getById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hãng xe có id = " + id));
    }

    @Override
    @Transactional
    public Brand update(Long id, Brand brand) {
        validateBrand(brand);
        Brand existing = getById(id);
        existing.setName(brand.getName());
        existing.setCountry(brand.getCountry());
        existing.setDescription(brand.getDescription());
        return brandRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        if (carModelRepository.existsByBrand_Id(id)) {
            throw new BadRequestException("Không thể xoá hãng vì còn dòng xe");
        }
        brandRepository.deleteById(id);
    }

    private void validateBrand(Brand brand) {
        if (brand == null || brand.getName() == null || brand.getName().isBlank()) {
            throw new BadRequestException("Tên hãng xe không được để trống");
        }
        if (brand.getName().length() > 100) {
            throw new BadRequestException("Tên hãng xe không được vượt quá 100 ký tự");
        }
        if (brand.getDescription() != null && brand.getDescription().length() > 500) {
            throw new BadRequestException("Mô tả hãng xe không được vượt quá 500 ký tự");
        }
    }
}
