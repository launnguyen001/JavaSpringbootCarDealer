package com.example.lab5.service;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SalesOrderServiceTests {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private EntityManager entityManager;

    private Long customerId;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        customer.setEmail("a@example.com");
        entityManager.persist(customer);
        entityManager.flush();
        customerId = customer.getId();
    }

    @Test
    void createReducesStockAndKeepsAvailableWhenStillInStock() {
        Long carId = persistCar(5);

        salesOrderService.create(orderOf(carId, 2));

        Car car = entityManager.find(Car.class, carId);
        assertThat(car.getQuantity()).isEqualTo(3);
        assertThat(car.getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    void createMarksCarAsSoldWhenStockReachesZero() {
        Long carId = persistCar(5);

        salesOrderService.create(orderOf(carId, 5));

        Car car = entityManager.find(Car.class, carId);
        assertThat(car.getQuantity()).isZero();
        assertThat(car.getStatus()).isEqualTo(CarStatus.SOLD);
    }

    @Test
    void createThrowsWhenNotEnoughStock() {
        Long carId = persistCar(3);

        assertThatThrownBy(() -> salesOrderService.create(orderOf(carId, 4)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createThrowsWhenCustomerNotFound() {
        Long carId = persistCar(5);
        SalesOrderRequest request = new SalesOrderRequest(
                99999L, null, List.of(new OrderItemRequest(carId, 1)));

        assertThatThrownBy(() -> salesOrderService.create(request))
                .isInstanceOf(com.example.lab5.exception.ResourceNotFoundException.class);
    }

    @Test
    void cancelRestoresStockAndAvailability() {
        Long carId = persistCar(5);
        SalesOrder order = salesOrderService.create(orderOf(carId, 5));
        assertThat(entityManager.find(Car.class, carId).getStatus()).isEqualTo(CarStatus.SOLD);

        salesOrderService.cancel(order.getId());

        Car car = entityManager.find(Car.class, carId);
        assertThat(car.getQuantity()).isEqualTo(5);
        assertThat(car.getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    private SalesOrderRequest orderOf(Long carId, int quantity) {
        return new SalesOrderRequest(customerId, null, List.of(new OrderItemRequest(carId, quantity)));
    }

    private Long persistCar(int quantity) {
        Brand brand = new Brand();
        brand.setName("Toyota-" + System.nanoTime());
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBrand(brand);
        entityManager.persist(model);

        Car car = new Car();
        car.setName("Camry 2.0");
        car.setPrice(100.0);
        car.setQuantity(quantity);
        car.setModel(model);
        entityManager.persist(car);

        entityManager.flush();
        return car.getId();
    }
}
