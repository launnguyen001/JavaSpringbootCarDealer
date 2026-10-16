package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CarModelControllerMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long brandId;
    private Long modelId;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setName("Honda");
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Civic");
        model.setBodyType("Sedan");
        model.setBrand(brand);
        entityManager.persist(model);
        entityManager.flush();

        brandId = brand.getId();
        modelId = model.getId();
    }

    @Test
    void updateModelReturnsUpdatedModel() throws Exception {
        String json = """
                {
                  "name": "Civic Type R",
                  "bodyType": "Hatchback",
                  "description": "Bản hiệu suất cao",
                  "brandId": %d
                }
                """.formatted(brandId);

        mockMvc.perform(asAdmin(put("/api/car-models/{id}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelId))
                .andExpect(jsonPath("$.name").value("Civic Type R"))
                .andExpect(jsonPath("$.bodyType").value("Hatchback"));
    }

    @Test
    void updateModelReturnsNotFound() throws Exception {
        String json = """
                {"name": "Không tồn tại", "brandId": %d}
                """.formatted(brandId);

        mockMvc.perform(asAdmin(put("/api/car-models/{id}", 9999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteModelWithoutCarsReturnsNoContent() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/car-models/{id}", modelId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteModelReturnsNotFound() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/car-models/{id}", 9999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteModelBlockedWhenStillHasCars() throws Exception {
        Car car = new Car();
        car.setName("Civic 1.5");
        car.setPrice(100.0);
        car.setQuantity(1);
        car.setModel(entityManager.find(CarModel.class, modelId));
        entityManager.persist(car);
        entityManager.flush();

        mockMvc.perform(asAdmin(delete("/api/car-models/{id}", modelId)))
                .andExpect(status().isBadRequest());
    }
}
