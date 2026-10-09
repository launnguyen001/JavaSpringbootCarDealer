package com.example.lab5.mapper;

import com.example.lab5.dto.ProductRequest;
import com.example.lab5.dto.ProductResponse;
import com.example.lab5.entity.Category;
import com.example.lab5.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        if (request == null) {
            return null;
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = new Category();
            category.setId(request.categoryId());
        }

        return new Product(
                null,
                request.name(),
                request.description(),
                request.price(),
                request.quantity(),
                category
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory() == null ? null : product.getCategory().getId()
        );
    }
}
