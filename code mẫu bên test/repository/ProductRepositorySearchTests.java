package com.example.lab5.repository;

import com.example.lab5.entity.Category;
import com.example.lab5.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositorySearchTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Category laptops = category("Laptops");
        Category phones = category("Phones");

        product("Laptop Acer", 100.0, 5, laptops);
        product("Laptop Asus", 200.0, 0, laptops);
        product("Notebook Dell", 300.0, 10, laptops);
        product("Phone Acer", 100.0, 9, phones);
        product("Phone Samsung", 400.0, 5, phones);
        product("Laptop Pro", 200.0, 10, laptops);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchByNameIgnoresCaseAndMatchesPartOfName() {
        assertThat(productRepository.findByNameContainingIgnoreCaseOrderByIdAsc("LAPTOP"))
                .extracting(Product::getName)
                .containsExactly("Laptop Acer", "Laptop Asus", "Laptop Pro");
        assertThat(productRepository.findByNameContainingIgnoreCaseOrderByIdAsc("camera"))
                .isEmpty();
    }

    @Test
    void searchByPriceIncludesBothBoundariesAndOrdersByPriceThenId() {
        assertThat(productRepository.findByPriceBetweenOrderByPriceAscIdAsc(100.0, 200.0))
                .extracting(Product::getName)
                .containsExactly("Laptop Acer", "Phone Acer", "Laptop Asus", "Laptop Pro");
        assertThat(productRepository.findByPriceBetweenOrderByPriceAscIdAsc(300.0, 300.0))
                .extracting(Product::getName)
                .containsExactly("Notebook Dell");
        assertThat(productRepository.findByPriceBetweenOrderByPriceAscIdAsc(500.0, 600.0))
                .isEmpty();
    }

    @Test
    void searchByMinimumQuantityIncludesBoundaryAndOrdersByStock() {
        assertThat(productRepository.findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(5))
                .extracting(Product::getName)
                .containsExactly(
                        "Notebook Dell", "Laptop Pro", "Phone Acer",
                        "Laptop Acer", "Phone Samsung"
                );
        assertThat(productRepository.findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(11))
                .isEmpty();
    }

    @Test
    void advancedSearchJoinsCategoryAndAppliesEveryFilter() {
        assertThat(productRepository.searchByCategoryAndPriceAndQuantity("LAP", 100.0, 200.0, 5))
                .extracting(Product::getName)
                .containsExactly("Laptop Acer", "Laptop Pro");
        assertThat(productRepository.searchByCategoryAndPriceAndQuantity("phones", 100.0, 200.0, 5))
                .extracting(Product::getName)
                .containsExactly("Phone Acer");
        assertThat(productRepository.searchByCategoryAndPriceAndQuantity("laptops", 200.0, 200.0, 10))
                .extracting(Product::getName)
                .containsExactly("Laptop Pro");
        assertThat(productRepository.searchByCategoryAndPriceAndQuantity("Laptops", 100.0, 200.0, 11))
                .isEmpty();
        assertThat(productRepository.searchByCategoryAndPriceAndQuantity("Cameras", 0.0, 500.0, 0))
                .isEmpty();
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        entityManager.persist(category);
        return category;
    }

    private void product(String name, Double price, Integer quantity, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(category);
        entityManager.persist(product);
    }
}
