package com.example.lab5.repository;

import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Car> findByModel_Brand_IdOrderByIdAsc(Long brandId);

    List<Car> findByModel_IdOrderByIdAsc(Long modelId);

    boolean existsByModel_Id(Long modelId);

    // ---- Tìm theo giá / tồn kho: nay xét trên từng chiếc (car_items) ----

    @Query("""
            SELECT c FROM Car c
            WHERE EXISTS (SELECT i.id FROM CarItem i
                          WHERE i.car = c AND i.price BETWEEN :minPrice AND :maxPrice)
            ORDER BY c.id
            """)
    List<Car> findByItemPriceBetween(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    @Query("""
            SELECT c FROM Car c
            WHERE (SELECT COUNT(i.id) FROM CarItem i
                   WHERE i.car = c AND i.status = :status) >= :minQuantity
            ORDER BY c.id
            """)
    List<Car> findWithItemsStatusAtLeast(
            @Param("status") CarStatus status,
            @Param("minQuantity") Integer minQuantity
    );

    @Query("""
            SELECT c FROM Car c JOIN c.model m
            WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :modelName, '%'))
              AND EXISTS (SELECT i.id FROM CarItem i
                          WHERE i.car = c AND i.price BETWEEN :minPrice AND :maxPrice)
              AND (SELECT COUNT(i2.id) FROM CarItem i2
                   WHERE i2.car = c AND i2.status = :status) >= :minQuantity
            ORDER BY c.id
            """)
    List<Car> searchByModelAndPriceAndQuantity(
            @Param("modelName") String modelName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minQuantity") Integer minQuantity,
            @Param("status") CarStatus status
    );

    @Query("""
            SELECT c FROM Car c JOIN c.model m JOIN m.brand b
            WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :brandName, '%'))
              AND EXISTS (SELECT i.id FROM CarItem i
                          WHERE i.car = c AND i.price BETWEEN :minPrice AND :maxPrice)
              AND (SELECT COUNT(i2.id) FROM CarItem i2
                   WHERE i2.car = c AND i2.status = :status) >= :minQuantity
            ORDER BY c.id
            """)
    List<Car> searchByBrandAndPriceAndQuantity(
            @Param("brandName") String brandName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minQuantity") Integer minQuantity,
            @Param("status") CarStatus status
    );
}
