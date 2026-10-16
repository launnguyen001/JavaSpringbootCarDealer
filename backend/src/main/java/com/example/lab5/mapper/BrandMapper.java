package com.example.lab5.mapper;

import com.example.lab5.dto.BrandRequest;
import com.example.lab5.dto.BrandResponse;
import com.example.lab5.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toEntity(BrandRequest request) {
        if (request == null) {
            return null;
        }
        Brand brand = new Brand();
        brand.setName(request.name());
        brand.setCountry(request.country());
        brand.setDescription(request.description());
        return brand;
    }

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getCountry(),
                brand.getDescription()
        );
    }
}
