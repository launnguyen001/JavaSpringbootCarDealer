package com.example.lab5.repository;

import com.example.lab5.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    List<CarModel> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<CarModel> findByBrand_IdOrderByIdAsc(Long brandId);

    boolean existsByNameAndBrand_Id(String name, Long brandId);
}
