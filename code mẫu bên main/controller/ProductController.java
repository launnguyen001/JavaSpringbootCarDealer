package com.example.lab5.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lab5.dto.ProductRequest;
import com.example.lab5.dto.ProductResponse;
import com.example.lab5.entity.Product;
import com.example.lab5.mapper.ProductMapper;
import com.example.lab5.service.ProductService;

import java.util.List;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    // Chức năng 1: Xem danh sách sản phẩm
    // GET: http://localhost:8080/api/products
    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    // Chức năng 2: Thêm sản phẩm
    // POST: http://localhost:8080/api/products
    @PostMapping
    public ProductResponse addProduct(@RequestBody ProductRequest request) {
        return productMapper.toResponse(productService.create(productMapper.toEntity(request)));
    }

    @GetMapping("/search/name")
    public List<ProductResponse> searchByName(@RequestParam String name) {
        return toResponses(productService.searchByName(name));
    }

    @GetMapping("/search/price")
    public List<ProductResponse> searchByPrice(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice
    ) {
        return toResponses(productService.searchByPrice(minPrice, maxPrice));
    }

    @GetMapping("/search/stock")
    public List<ProductResponse> searchByMinQuantity(@RequestParam Integer minQuantity) {
        return toResponses(productService.searchByMinQuantity(minQuantity));
    }

    @GetMapping("/search/advanced")
    public List<ProductResponse> searchAdvanced(
            @RequestParam String categoryName,
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice,
            @RequestParam Integer minQuantity
    ) {
        return toResponses(productService.searchAdvanced(
                categoryName, minPrice, maxPrice, minQuantity
        ));
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(productMapper::toResponse).toList();
    }
}
