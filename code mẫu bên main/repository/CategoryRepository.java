package com.example.lab5.repository;

import com.example.lab5.entity.Category;

import java.util.List;

public interface CategoryRepository {

    Category save(Category category);

    List<Category> findAll();

    boolean existsById(Long id);
}
