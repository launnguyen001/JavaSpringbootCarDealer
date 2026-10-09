package com.example.lab5.service.Impl;

import com.example.lab5.entity.Category;
import com.example.lab5.entity.Product;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CategoryRepository;
import com.example.lab5.repository.ProductRepository;
import com.example.lab5.service.ProductService;
import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {


    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;


    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            EntityManager entityManager
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }


    @Override
    @Transactional
    public Product create(Product product) {
        validateProduct(product);
        Long categoryId = product.getCategory().getId();
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy danh mục có id = " + categoryId
            );
        }

        product.setCategory(entityManager.getReference(Category.class, categoryId));
        product.setId(null);
        return productRepository.save(product);
    }


    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có id = " + id));
    }

    @Override
    public List<Product> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Từ khóa tên sản phẩm không được để trống");
        }
        return productRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name.trim());
    }

    @Override
    public List<Product> searchByPrice(Double minPrice, Double maxPrice) {
        validatePriceRange(minPrice, maxPrice);
        return productRepository.findByPriceBetweenOrderByPriceAscIdAsc(minPrice, maxPrice);
    }

    @Override
    public List<Product> searchByMinQuantity(Integer minQuantity) {
        validateMinQuantity(minQuantity);
        return productRepository.findByQuantityGreaterThanEqualOrderByQuantityDescIdAsc(minQuantity);
    }

    @Override
    public List<Product> searchAdvanced(
            String categoryName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    ) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        validatePriceRange(minPrice, maxPrice);
        validateMinQuantity(minQuantity);
        return productRepository.searchByCategoryAndPriceAndQuantity(
                categoryName.trim(), minPrice, maxPrice, minQuantity
        );
    }

    private void validatePriceRange(Double minPrice, Double maxPrice) {
        if (minPrice == null || maxPrice == null
                || !Double.isFinite(minPrice) || !Double.isFinite(maxPrice)
                || minPrice < 0 || maxPrice < minPrice) {
            throw new BadRequestException("Khoảng giá không hợp lệ");
        }
    }

    private void validateMinQuantity(Integer minQuantity) {
        if (minQuantity == null || minQuantity < 0) {
            throw new BadRequestException("Số lượng tối thiểu không hợp lệ");
        }
    }

    private void validateProduct(Product product) {
        if (product == null || product.getName() == null || product.getName().isBlank()) {
            throw new BadRequestException("Tên sản phẩm không được để trống");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new BadRequestException("Giá sản phẩm không hợp lệ");
        }
        if (product.getQuantity() == null || product.getQuantity() < 0) {
            throw new BadRequestException("Số lượng sản phẩm không hợp lệ");
        }
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new BadRequestException("Danh mục sản phẩm không được để trống");
        }
    }
}
