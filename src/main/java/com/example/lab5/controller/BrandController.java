package com.example.lab5.controller;

import com.example.lab5.dto.BrandRequest;
import com.example.lab5.dto.BrandResponse;
import com.example.lab5.mapper.BrandMapper;
import com.example.lab5.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;
    private final BrandMapper brandMapper;

    public BrandController(BrandService brandService, BrandMapper brandMapper) {
        this.brandService = brandService;
        this.brandMapper = brandMapper;
    }

    // Chức năng: Thêm hãng xe
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse addBrand(@Valid @RequestBody BrandRequest request) {
        return brandMapper.toResponse(brandService.create(brandMapper.toEntity(request)));
    }

    // Chức năng: Xem danh sách hãng xe
    @GetMapping
    public List<BrandResponse> getAllBrands() {
        return brandService.getAllBrands()
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    // Chức năng: Xem chi tiết hãng xe
    @GetMapping("/{id}")
    public BrandResponse getBrandById(@PathVariable Long id) {
        return brandMapper.toResponse(brandService.getById(id));
    }

    // Chức năng: Cập nhật hãng xe
    @PutMapping("/{id}")
    public BrandResponse updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return brandMapper.toResponse(brandService.update(id, brandMapper.toEntity(request)));
    }

    // Chức năng: Xoá hãng xe
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable Long id) {
        brandService.delete(id);
    }
}
