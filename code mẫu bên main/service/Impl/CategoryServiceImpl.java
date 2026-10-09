package com.example.lab5.service.Impl;

import com.example.lab5.entity.Category;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.repository.CategoryRepository;
import com.example.lab5.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category create(Category category) {
        validateCategory(category);

        category.setId(null);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    private void validateCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        if (category.getName().length() > 100) {
            throw new BadRequestException("Tên danh mục không được vượt quá 100 ký tự");
        }
        if (category.getDescription() != null && category.getDescription().length() > 500) {
            throw new BadRequestException("Mô tả danh mục không được vượt quá 500 ký tự");
        }
    }

}
