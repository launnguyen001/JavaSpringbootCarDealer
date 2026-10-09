package com.example.lab5.controller;

import com.example.lab5.dto.CategoryRequest;
import com.example.lab5.dto.CategoryResponse;
import com.example.lab5.mapper.CategoryMapper;
import com.example.lab5.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @PostMapping
    public CategoryResponse addCategory(@RequestBody CategoryRequest request) {
        return categoryMapper.toResponse(categoryService.create(categoryMapper.toEntity(request)));
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

}
