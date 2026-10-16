package com.example.lab5.repository;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CarRepositorySearchTests {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Brand toyota = brand("Toyota", "Nhật Bản");
        Brand honda = brand("Honda", "Nhật Bản");

        CarModel camry = model("Camry", toyota);
        CarModel corolla = model("Corolla", toyota);
        CarModel civic = model("Civic", honda);

        car("Camry 2.0", 100.0, 5, camry);
        car("Camry 2.5", 200.0, 0, camry);
        car("Corolla Cross", 300.0, 10, corolla);
        car("Civic 1.5", 100.0, 9, civic);
        car("Civic Type R", 400.0, 5, civic);
        car("Corolla Altis", 200.0, 10, corolla);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchByNameIgnoresCaseAndMatchesPartOfName() {
        assertThat(carRepository.findByNameContainingIgnoreCaseOrderByIdAsc("CAMRY"))
                .extracting(Car::getName)
                .containsExactly("Camry 2.0", "Camry 2.5");
        assertThat(carRepository.findByNameContainingIgnoreCaseOrderByIdAsc("lexus"))
                .isEmpty();
    }

    @Test
    void searchByItemPriceIncludesBothBoundaries() {
        // Xe có ít nhất một chiếc trong khoảng giá [100, 200]
        assertThat(carRepository.findByItemPriceBetween(100.0, 200.0))
                .extracting(Car::getName)
                .containsExactly("Camry 2.0", "Civic 1.5", "Corolla Altis");
        assertThat(carRepository.findByItemPriceBetween(300.0, 300.0))
                .extracting(Car::getName)
                .containsExactly("Corolla Cross");
        assertThat(carRepository.findByItemPriceBetween(500.0, 600.0))
                .isEmpty();
    }

    @Test
    void searchByMinimumAvailableItemsIncludesBoundary() {
        assertThat(carRepository.findWithItemsStatusAtLeast(CarStatus.AVAILABLE, 5))
                .extracting(Car::getName)
                .containsExactly(
                        "Camry 2.0", "Corolla Cross", "Civic 1.5", "Civic Type R", "Corolla Altis"
                );
        assertThat(carRepository.findWithItemsStatusAtLeast(CarStatus.AVAILABLE, 11))
                .isEmpty();
    }

    @Test
    void advancedSearchByModelJoinsAndAppliesEveryFilter() {
        assertThat(carRepository.searchByModelAndPriceAndQuantity(
                "camry", 100.0, 200.0, 5, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Camry 2.0");
        assertThat(carRepository.searchByModelAndPriceAndQuantity(
                "corolla", 100.0, 200.0, 5, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Corolla Altis");
        assertThat(carRepository.searchByModelAndPriceAndQuantity(
                "corolla", 200.0, 200.0, 10, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Corolla Altis");
        assertThat(carRepository.searchByModelAndPriceAndQuantity(
                "camry", 100.0, 200.0, 11, CarStatus.AVAILABLE))
                .isEmpty();
        assertThat(carRepository.searchByModelAndPriceAndQuantity(
                "lexus", 0.0, 500.0, 0, CarStatus.AVAILABLE))
                .isEmpty();
    }

    @Test
    void advancedSearchByBrandJoinsAndAppliesEveryFilter() {
        assertThat(carRepository.searchByBrandAndPriceAndQuantity(
                "toyota", 0.0, 500.0, 0, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Camry 2.0", "Corolla Cross", "Corolla Altis");
        assertThat(carRepository.searchByBrandAndPriceAndQuantity(
                "honda", 100.0, 200.0, 5, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Civic 1.5");
        assertThat(carRepository.searchByBrandAndPriceAndQuantity(
                "toyota", 100.0, 200.0, 10, CarStatus.AVAILABLE))
                .extracting(Car::getName)
                .containsExactly("Corolla Altis");
    }

    private Brand brand(String name, String country) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry(country);
        entityManager.persist(brand);
        return brand;
    }

    private CarModel model(String name, Brand brand) {
        CarModel model = new CarModel();
        model.setName(name);
        model.setBrand(brand);
        entityManager.persist(model);
        return model;
    }

    // Tạo dòng xe với `quantity` chiếc AVAILABLE, mỗi chiếc giá `price`.
    // quantity = 0 -> dòng xe chưa có chiếc nào trong kho.
    private void car(String name, Double price, Integer quantity, CarModel model) {
        Car car = new Car();
        car.setName(name);
        car.setBasePrice(price);
        car.setModel(model);
        entityManager.persist(car);

        for (int i = 1; i <= quantity; i++) {
            CarItem item = new CarItem();
            item.setCar(car);
            item.setVin("VIN-" + System.nanoTime() + "-" + i);
            item.setCondition(CarCondition.NEW);
            item.setPrice(price);
            item.setStatus(CarStatus.AVAILABLE);
            entityManager.persist(item);
        }
    }
}
