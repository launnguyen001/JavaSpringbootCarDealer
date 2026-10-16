package com.example.lab5.repository;

import com.example.lab5.entity.Brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {

    Brand save(Brand brand);

    List<Brand> findAll();

    Optional<Brand> findById(Long id);

    boolean existsById(Long id);

    boolean existsByName(String name);

    void deleteById(Long id);
}
