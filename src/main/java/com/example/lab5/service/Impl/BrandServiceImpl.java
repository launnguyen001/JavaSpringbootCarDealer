package com.example.lab5.service.Impl;

import com.example.lab5.entity.Brand;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.service.BrandService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    public BrandServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
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
