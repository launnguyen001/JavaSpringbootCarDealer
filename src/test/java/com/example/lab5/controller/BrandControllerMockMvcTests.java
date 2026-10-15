package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.CarModel;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Brand dùng JdbcTemplate nên không bọc @Transactional để tránh lệch kết nối JPA/JDBC
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandControllerMockMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CarModelRepository carModelRepository;

    @Test
    void updateBrandReturnsUpdatedBrand() throws Exception {
        Long id = persistBrand("Brand-update-" + System.nanoTime());

        String json = """
                {
                  "name": "Toyota Updated",
                  "country": "Nhật Bản",
                  "description": "Hãng xe cập nhật"
                }
                """;

        mockMvc.perform(put("/api/brands/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Toyota Updated"))
                .andExpect(jsonPath("$.country").value("Nhật Bản"));
    }

    @Test
    void updateBrandReturnsNotFound() throws Exception {
        String json = """
                {"name": "Không tồn tại"}
                """;

        mockMvc.perform(put("/api/brands/{id}", 9999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBrandWithoutModelsReturnsNoContent() throws Exception {
        Long id = persistBrand("Brand-delete-" + System.nanoTime());

        mockMvc.perform(delete("/api/brands/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBrandReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/brands/{id}", 9999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBrandBlockedWhenStillHasModels() throws Exception {
        Long id = persistBrand("Brand-guard-" + System.nanoTime());

        Brand reference = new Brand();
        reference.setId(id);
        CarModel model = new CarModel();
        model.setName("Model-" + System.nanoTime());
        model.setBrand(reference);
        carModelRepository.save(model);

        mockMvc.perform(delete("/api/brands/{id}", id))
                .andExpect(status().isBadRequest());
    }

    private Long persistBrand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry("Test");
        return brandRepository.save(brand).getId();
    }
}
