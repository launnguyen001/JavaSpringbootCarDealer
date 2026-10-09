package com.example.lab5.service.Impl;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.SalesOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CarRepository carRepository;

    public SalesOrderServiceImpl(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            CarRepository carRepository
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.carRepository = carRepository;
    }

    @Override
    @Transactional
    public SalesOrder create(SalesOrderRequest request) {
        validateRequest(request);

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khách hàng có id = " + request.customerId()));

        Employee employee = null;
        if (request.employeeId() != null) {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy nhân viên có id = " + request.employeeId()));
        }

        SalesOrder order = new SalesOrder();
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCustomer(customer);
        order.setEmployee(employee);

        double total = 0.0;
        for (OrderItemRequest itemRequest : request.items()) {
            Car car = carRepository.findById(itemRequest.carId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy xe có id = " + itemRequest.carId()));

            if (car.getQuantity() == null || car.getQuantity() < itemRequest.quantity()) {
                throw new BadRequestException(
                        "Xe '" + car.getName() + "' không đủ số lượng trong kho");
            }

            car.setQuantity(car.getQuantity() - itemRequest.quantity());
            if (car.getQuantity() == 0) {
                car.setStatus(CarStatus.SOLD);
            }
            carRepository.save(car);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setCar(car);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(car.getPrice());
            order.getItems().add(item);

            total += car.getPrice() * itemRequest.quantity();
        }

        order.setTotalAmount(total);
        return salesOrderRepository.save(order);
    }

    @Override
    public List<SalesOrder> getAllOrders() {
        return salesOrderRepository.findAllByOrderByOrderDateDesc();
    }

    @Override
    public SalesOrder getOrderById(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng có id = " + id));
    }

    @Override
    public List<SalesOrder> getByCustomer(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Khách hàng không được để trống");
        }
        return salesOrderRepository.findByCustomer_IdOrderByOrderDateDesc(customerId);
    }

    @Override
    @Transactional
    public SalesOrder updateStatus(Long id, OrderStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái đơn hàng không được để trống");
        }
        SalesOrder order = getOrderById(id);
        order.setStatus(status);
        return salesOrderRepository.save(order);
    }

    @Override
    @Transactional
    public SalesOrder cancel(Long id) {
        SalesOrder order = getOrderById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị huỷ trước đó");
        }
        for (OrderItem item : order.getItems()) {
            Car car = item.getCar();
            car.setQuantity(car.getQuantity() + item.getQuantity());
            if (car.getStatus() == CarStatus.SOLD) {
                car.setStatus(CarStatus.AVAILABLE);
            }
            carRepository.save(car);
        }
        order.setStatus(OrderStatus.CANCELLED);
        return salesOrderRepository.save(order);
    }

    private String generateOrderCode() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateRequest(SalesOrderRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu đơn hàng không được để trống");
        }
        if (request.customerId() == null) {
            throw new BadRequestException("Khách hàng không được để trống");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất một xe");
        }
        for (OrderItemRequest item : request.items()) {
            if (item.carId() == null) {
                throw new BadRequestException("Xe không được để trống");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Số lượng xe phải lớn hơn 0");
            }
        }
    }
}
