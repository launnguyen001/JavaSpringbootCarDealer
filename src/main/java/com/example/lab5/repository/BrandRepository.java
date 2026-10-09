package com.example.lab5.repository;

import com.example.lab5.entity.Brand;

import java.util.List;

public interface BrandRepository {

    Brand save(Brand brand);

    List<Brand> findAll();

    boolean existsById(Long id);
}
