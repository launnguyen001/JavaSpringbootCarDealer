package com.example.lab5.service;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.OrderOptionResponse;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentMethod;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    void createMarksChosenItemSoldAndKeepsOthersAvailable() {
        Long carId = persistCarWithItems(3);
        Long itemId = firstItemId(carId);

        salesOrderService.create(orderOf(itemId));

        assertThat(entityManager.find(CarItem.class, itemId).getStatus()).isEqualTo(CarStatus.SOLD);
        long available = availableCount(carId);
        assertThat(available).isEqualTo(2);
    }

    @Test
    void createThrowsWhenItemAlreadySold() {
        Long carId = persistCarWithItems(1);
        Long itemId = firstItemId(carId);
        salesOrderService.create(orderOf(itemId));

        assertThatThrownBy(() -> salesOrderService.create(orderOf(itemId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createThrowsWhenQuantityIsNotOne() {
        Long carId = persistCarWithItems(3);
        Long itemId = firstItemId(carId);
        SalesOrderRequest request = new SalesOrderRequest(
                customerId, null, List.of(new OrderItemRequest(itemId, 2)));

        assertThatThrownBy(() -> salesOrderService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createThrowsWhenCustomerNotFound() {
        Long carId = persistCarWithItems(1);
        SalesOrderRequest request = new SalesOrderRequest(
                99999L, null, List.of(new OrderItemRequest(firstItemId(carId), 1)));

        assertThatThrownBy(() -> salesOrderService.create(request))
                .isInstanceOf(com.example.lab5.exception.ResourceNotFoundException.class);
    }

    @Test
    void cancelRestoresItemAvailability() {
        Long carId = persistCarWithItems(1);
        Long itemId = firstItemId(carId);
        SalesOrder order = salesOrderService.create(orderOf(itemId));
        assertThat(entityManager.find(CarItem.class, itemId).getStatus()).isEqualTo(CarStatus.SOLD);

        salesOrderService.cancel(order.getId());

        assertThat(entityManager.find(CarItem.class, itemId).getStatus()).isEqualTo(CarStatus.AVAILABLE);
        assertThat(availableCount(carId)).isEqualTo(1);
    }

    @Test
    void getOrderOptionsForInvoiceExcludesCancelledAndAlreadyInvoiced() {
        Long invoicedCarId = persistCarWithItems(1);
        Long cancelledCarId = persistCarWithItems(1);
        Long eligibleCarId = persistCarWithItems(1);

        SalesOrder invoiced = salesOrderService.create(orderOf(firstItemId(invoicedCarId)));
        SalesOrder cancelled = salesOrderService.create(orderOf(firstItemId(cancelledCarId)));
        SalesOrder eligible = salesOrderService.create(orderOf(firstItemId(eligibleCarId)));

        persistInvoice(invoiced);
        salesOrderService.cancel(cancelled.getId());

        List<OrderOptionResponse> options = salesOrderService.getOrderOptions("invoice");

        assertThat(options).extracting(option -> option.orderId()).containsExactly(eligible.getId());
        OrderOptionResponse option = options.getFirst();
        assertThat(option.customerName()).isEqualTo("Nguyễn Văn A");
        assertThat(option.hasInvoice()).isFalse();
        assertThat(option.hasContract()).isFalse();
        assertThat(option.remainingAmount()).isEqualTo(option.totalAmount());
    }

    @Test
    void getOrderOptionsForContractExcludesOrderWithContract() {
        Long contractedCarId = persistCarWithItems(1);
        Long eligibleCarId = persistCarWithItems(1);

        SalesOrder contracted = salesOrderService.create(orderOf(firstItemId(contractedCarId)));
        SalesOrder eligible = salesOrderService.create(orderOf(firstItemId(eligibleCarId)));

        persistContract(contracted);

        List<OrderOptionResponse> options = salesOrderService.getOrderOptions("contract");

        assertThat(options).extracting(option -> option.orderId()).containsExactly(eligible.getId());
        assertThat(options.getFirst().hasContract()).isFalse();
    }

    @Test
    void getOrderOptionsForPaymentKeepsActiveOrdersAndComputesRemaining() {
        Long paidCarId = persistCarWithItems(1);
        Long cancelledCarId = persistCarWithItems(1);

        SalesOrder order = salesOrderService.create(orderOf(firstItemId(paidCarId)));
        SalesOrder cancelled = salesOrderService.create(orderOf(firstItemId(cancelledCarId)));
        salesOrderService.cancel(cancelled.getId());

        persistPayment(order, 40.0);
        persistPayment(order, 10.0);

        List<OrderOptionResponse> options = salesOrderService.getOrderOptions("payment");

        assertThat(options).extracting(option -> option.orderId()).containsExactly(order.getId());
        OrderOptionResponse option = options.getFirst();
        assertThat(option.paidAmount()).isEqualTo(50.0);
        assertThat(option.remainingAmount()).isEqualTo(option.totalAmount() - 50.0);
    }

    @Test
    void getOrderOptionsRejectsUnknownTarget() {
        assertThatThrownBy(() -> salesOrderService.getOrderOptions("bogus"))
                .isInstanceOf(BadRequestException.class);
    }

    private SalesOrderRequest orderOf(Long carItemId) {
        return new SalesOrderRequest(customerId, null, List.of(new OrderItemRequest(carItemId, 1)));
    }

    private void persistInvoice(SalesOrder order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-TEST-" + System.nanoTime());
        invoice.setTotal(order.getTotalAmount());
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setOrder(order);
        entityManager.persist(invoice);
        entityManager.flush();
    }

    private void persistContract(SalesOrder order) {
        Contract contract = new Contract();
        contract.setContractNumber("CON-TEST-" + System.nanoTime());
        contract.setSignedAt(LocalDateTime.now());
        contract.setContent("Nội dung thử nghiệm");
        contract.setOrder(order);
        entityManager.persist(contract);
        entityManager.flush();
    }

    private void persistPayment(SalesOrder order, double amount) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setOrder(order);
        entityManager.persist(payment);
        entityManager.flush();
    }

    private long availableCount(Long carId) {
        return entityManager
                .createQuery("SELECT COUNT(i) FROM CarItem i WHERE i.car.id = :carId AND i.status = :st",
                        Long.class)
                .setParameter("carId", carId)
                .setParameter("st", CarStatus.AVAILABLE)
                .getSingleResult();
    }

    private Long firstItemId(Long carId) {
        return entityManager
                .createQuery("SELECT i.id FROM CarItem i WHERE i.car.id = :carId ORDER BY i.id", Long.class)
                .setParameter("carId", carId)
                .setMaxResults(1)
                .getSingleResult();
    }

    // Tạo dòng xe + `count` chiếc AVAILABLE
    private Long persistCarWithItems(int count) {
        Brand brand = new Brand();
        brand.setName("Toyota-" + System.nanoTime());
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBrand(brand);
        entityManager.persist(model);

        Car car = new Car();
        car.setName("Camry 2.0");
        car.setBasePrice(100.0);
        car.setModel(model);
        entityManager.persist(car);

        for (int i = 1; i <= count; i++) {
            CarItem item = new CarItem();
            item.setCar(car);
            item.setVin("VIN-" + System.nanoTime() + "-" + i);
            item.setCondition(CarCondition.NEW);
            item.setPrice(100.0);
            item.setStatus(CarStatus.AVAILABLE);
            entityManager.persist(item);
        }
        entityManager.flush();
        return car.getId();
    }
}
