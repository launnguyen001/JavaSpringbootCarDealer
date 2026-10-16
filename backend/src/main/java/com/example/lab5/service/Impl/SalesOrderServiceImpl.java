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
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.OrderItemRepository;
import com.example.lab5.repository.PaymentRepository;
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
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;

    public SalesOrderServiceImpl(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            CarRepository carRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.carRepository = carRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public SalesOrder create(SalesOrderRequest request) {
        validateRequest(request);

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khách hàng có id = " + request.customerId()));

        SalesOrder order = new SalesOrder();
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCustomer(customer);
        order.setEmployee(resolveEmployee(request.employeeId()));

        double total = 0.0;
        for (OrderItemRequest itemRequest : request.items()) {
            total += addItemToOrder(order, itemRequest);
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
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        return salesOrderRepository.save(order);
    }

    @Override
    @Transactional
    public SalesOrder update(Long id, SalesOrderRequest request) {
        validateRequest(request);
        SalesOrder order = getOrderById(id);
        assertOrderEditable(order);

        order.setEmployee(resolveEmployee(request.employeeId()));

        // Trả lại tồn kho của các dòng cũ trước khi thay bằng danh sách mới
        restoreStock(order);
        order.getItems().clear();

        double total = 0.0;
        for (OrderItemRequest itemRequest : request.items()) {
            total += addItemToOrder(order, itemRequest);
        }
        order.setTotalAmount(total);

        SalesOrder saved = salesOrderRepository.save(order);
        salesOrderRepository.flush();
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SalesOrder order = getOrderById(id);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new BadRequestException("Chỉ xoá được đơn hàng đã huỷ");
        }
        if (paymentRepository.existsByOrder_Id(id)
                || contractRepository.existsByOrder_Id(id)
                || invoiceRepository.existsByOrder_Id(id)) {
            throw new BadRequestException("Không thể xoá đơn đã có thanh toán/hợp đồng/hóa đơn");
        }
        salesOrderRepository.delete(order);
    }

    @Override
    public List<OrderItem> getItems(Long orderId) {
        getOrderById(orderId);
        return orderItemRepository.findByOrder_IdOrderByIdAsc(orderId);
    }

    @Override
    @Transactional
    public OrderItem addItem(Long orderId, OrderItemRequest request) {
        validateItem(request);
        SalesOrder order = getOrderById(orderId);
        assertOrderEditable(order);

        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe có id = " + request.carId()));
        deductStock(car, request.quantity());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setCar(car);
        item.setQuantity(request.quantity());
        item.setUnitPrice(car.getPrice());
        order.getItems().add(item);
        recalculateTotal(order);

        salesOrderRepository.save(order);
        salesOrderRepository.flush();
        return item;
    }

    @Override
    @Transactional
    public OrderItem updateItem(Long orderId, Long itemId, OrderItemRequest request) {
        validateItem(request);
        SalesOrder order = getOrderById(orderId);
        assertOrderEditable(order);

        OrderItem item = findItem(order, itemId);

        // Trả lại tồn kho cũ rồi trừ theo số lượng/xe mới
        restoreCarStock(item.getCar(), item.getQuantity());
        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe có id = " + request.carId()));
        deductStock(car, request.quantity());

        item.setCar(car);
        item.setQuantity(request.quantity());
        item.setUnitPrice(car.getPrice());
        recalculateTotal(order);

        salesOrderRepository.save(order);
        salesOrderRepository.flush();
        return item;
    }

    @Override
    @Transactional
    public void deleteItem(Long orderId, Long itemId) {
        SalesOrder order = getOrderById(orderId);
        assertOrderEditable(order);

        OrderItem item = findItem(order, itemId);
        restoreCarStock(item.getCar(), item.getQuantity());
        order.getItems().remove(item);
        recalculateTotal(order);
        salesOrderRepository.save(order);
    }

    private OrderItem findItem(SalesOrder order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId() != null && item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dòng xe có id = " + itemId + " trong đơn hàng"));
    }

    // Thêm một dòng xe vào đơn: kiểm tra + trừ tồn kho, trả về thành tiền của dòng
    private double addItemToOrder(SalesOrder order, OrderItemRequest itemRequest) {
        Car car = carRepository.findById(itemRequest.carId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe có id = " + itemRequest.carId()));
        deductStock(car, itemRequest.quantity());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setCar(car);
        item.setQuantity(itemRequest.quantity());
        item.setUnitPrice(car.getPrice());
        order.getItems().add(item);

        return car.getPrice() * itemRequest.quantity();
    }

    private void deductStock(Car car, int quantity) {
        if (car.getQuantity() == null || car.getQuantity() < quantity) {
            throw new BadRequestException("Xe '" + car.getName() + "' không đủ số lượng trong kho");
        }
        car.setQuantity(car.getQuantity() - quantity);
        if (car.getQuantity() == 0) {
            car.setStatus(CarStatus.SOLD);
        }
        carRepository.save(car);
    }

    private void restoreStock(SalesOrder order) {
        for (OrderItem item : order.getItems()) {
            restoreCarStock(item.getCar(), item.getQuantity());
        }
    }

    private void restoreCarStock(Car car, int quantity) {
        car.setQuantity(car.getQuantity() + quantity);
        if (car.getStatus() == CarStatus.SOLD) {
            car.setStatus(CarStatus.AVAILABLE);
        }
        carRepository.save(car);
    }

    private void recalculateTotal(SalesOrder order) {
        double total = order.getItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        order.setTotalAmount(total);
    }

    private Employee resolveEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên có id = " + employeeId));
    }

    private void assertOrderEditable(SalesOrder order) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Chỉ sửa được đơn hàng ở trạng thái PENDING hoặc CONFIRMED");
        }
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
            validateItem(item);
        }
    }

    private void validateItem(OrderItemRequest item) {
        if (item == null || item.carId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new BadRequestException("Số lượng xe phải lớn hơn 0");
        }
    }
}
