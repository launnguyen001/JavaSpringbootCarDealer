package com.example.lab5.service;

import com.example.lab5.entity.Category;

import java.util.List;

public interface CategoryService {
    Category create(Category category);

    List<Category> getAllCategories();
}
