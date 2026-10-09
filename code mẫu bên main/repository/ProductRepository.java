package com.example.lab5.repository;

import com.example.lab5.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Product> findByPriceBetweenOrderByPriceAscIdAsc(Double minPrice, Double maxPrice);

    List<Product> findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(Integer minQuantity);

    @Query("""
            SELECT p FROM Product p JOIN p.category c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :categoryName, '%'))
              AND p.price BETWEEN :minPrice AND :maxPrice
              AND p.quantity >= :minQuantity
            ORDER BY p.id
            """)
    List<Product> searchByCategoryAndPriceAndQuantity(
            @Param("categoryName") String categoryName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minQuantity") Integer minQuantity
    );
}
