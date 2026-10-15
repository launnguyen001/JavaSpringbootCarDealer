package com.example.lab5.service;

import com.example.lab5.entity.Brand;

import java.util.List;

public interface BrandService {

    Brand create(Brand brand);

    List<Brand> getAllBrands();

    Brand getById(Long id);

    Brand update(Long id, Brand brand);

    void delete(Long id);
}
