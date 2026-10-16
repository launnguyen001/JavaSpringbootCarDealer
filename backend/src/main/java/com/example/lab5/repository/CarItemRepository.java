package com.example.lab5.repository;

import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarItemRepository extends JpaRepository<CarItem, Long> {

    List<CarItem> findByCar_IdOrderByIdAsc(Long carId);

    List<CarItem> findByCar_IdAndStatusOrderByIdAsc(Long carId, CarStatus status);

    long countByCar_IdAndStatus(Long carId, CarStatus status);

    boolean existsByCar_Id(Long carId);

    boolean existsByVin(String vin);

    Optional<CarItem> findByVin(String vin);

    void deleteByCar_Id(Long carId);
}
