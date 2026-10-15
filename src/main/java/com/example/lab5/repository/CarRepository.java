package com.example.lab5.repository;

import com.example.lab5.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Car> findByPriceBetweenOrderByPriceAscIdAsc(Double minPrice, Double maxPrice);

    List<Car> findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(Integer minQuantity);

    List<Car> findByModel_Brand_IdOrderByIdAsc(Long brandId);

    List<Car> findByModel_IdOrderByIdAsc(Long modelId);

    boolean existsByModel_Id(Long modelId);

    @Query("""
            SELECT c FROM Car c JOIN c.model m JOIN m.brand b
            WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :modelName, '%'))
              AND c.price BETWEEN :minPrice AND :maxPrice
              AND c.quantity >= :minQuantity
            ORDER BY c.id
            """)
    List<Car> searchByModelAndPriceAndQuantity(
            @Param("modelName") String modelName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minQuantity") Integer minQuantity
    );

    @Query("""
            SELECT c FROM Car c JOIN c.model m JOIN m.brand b
            WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :brandName, '%'))
              AND c.price BETWEEN :minPrice AND :maxPrice
              AND c.quantity >= :minQuantity
            ORDER BY c.id
            """)
    List<Car> searchByBrandAndPriceAndQuantity(
            @Param("brandName") String brandName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minQuantity") Integer minQuantity
    );
}
