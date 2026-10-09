package com.example.lab5.mapper;

import com.example.lab5.dto.CategoryRequest;
import com.example.lab5.dto.CategoryResponse;
import com.example.lab5.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) {
            return null;
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
