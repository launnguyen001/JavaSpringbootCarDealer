package com.example.lab5.service;

import java.util.List;

import com.example.lab5.entity.Product;

public interface ProductService {


    Product create(Product product);


    List<Product> getAllProducts();


    Product getProductById(Long id);

    List<Product> searchByName(String name);

    List<Product> searchByPrice(Double minPrice, Double maxPrice);

    List<Product> searchByMinQuantity(Integer minQuantity);

    List<Product> searchAdvanced(
            String categoryName,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity
    );
}
