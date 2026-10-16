package com.example.lab5.mongorepository;

import com.example.lab5.document.CarMedia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CarMediaRepository extends MongoRepository<CarMedia, String> {

    Optional<CarMedia> findByCarId(Long carId);

    boolean existsByCarId(Long carId);
}
