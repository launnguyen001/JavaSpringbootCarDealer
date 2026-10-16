package com.example.lab5.mongorepository;

import com.example.lab5.document.CarSpecification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CarSpecificationRepository extends MongoRepository<CarSpecification, String> {

    Optional<CarSpecification> findByCarId(Long carId);

    boolean existsByCarId(Long carId);

    void deleteByCarId(Long carId);
}
