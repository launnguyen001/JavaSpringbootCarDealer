package com.example.lab5.service.Impl;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.OrderOptionResponse;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CarItemRepository;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.OrderItemRepository;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.OrderStateService;
import com.example.lab5.service.CarCatalogService;
import com.example.lab5.service.SalesOrderService;
import com.example.lab5.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CarItemRepository carItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderStateService orderStateService;
    private final CarCatalogService carCatalogService;
    private final NotificationEventFactory notificationEvents;

    public SalesOrderServiceImpl(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            CarItemRepository carItemRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            OrderStateService orderStateService,
            CarCatalogService carCatalogService,
            NotificationEventFactory notificationEvents
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.carItemRepository = carItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.orderStateService = orderStateService;
        this.carCatalogService = carCatalogService;
        this.notificationEvents = notificationEvents;
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
        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer);
        order.setEmployee(resolveEmployee(request.employeeId()));

        double total = 0.0;
        for (OrderItemRequest itemRequest : request.items()) {
            total += addItemToOrder(order, itemRequest);
        }

        order.setTotalAmount(total);

        SalesOrder saved = salesOrderRepository.save(order);
        orderStateService.recordInitial(saved, SecurityUtils.currentUsername());
        carCatalogService.evictCatalog();
        notificationEvents.orderCreated(saved);
        return saved;
    }

    @Override
    public List<SalesOrder> getAllOrders() {
        return salesOrderRepository.findAllByOrderByOrderDateDescIdDesc();
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
        return salesOrderRepository.findByCustomer_IdOrderByOrderDateDescIdDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderOptionResponse> getOrderOptions(String target) {
        String normalized = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);

        List<SalesOrder> orders = switch (normalized) {
            case "invoice" -> salesOrderRepository.findOptionsForInvoice();
            case "contract" -> salesOrderRepository.findOptionsForContract();
            case "payment" -> salesOrderRepository.findActiveOrderOptions();
            default -> throw new BadRequestException(
                    "Loại danh sách đơn hàng không hợp lệ: " + target
                            + " (chỉ nhận invoice, contract hoặc payment)");
        };

        Set<Long> invoicedOrderIds = new HashSet<>(invoiceRepository.findAllOrderIds());
        Set<Long> contractedOrderIds = new HashSet<>(contractRepository.findAllOrderIds());

        Map<Long, Double> paidByOrder = new HashMap<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (payment.getOrder() == null || (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.PARTIAL)) {
                continue;
            }
            double amount = payment.getAmount() == null ? 0.0 : payment.getAmount();
            paidByOrder.merge(payment.getOrder().getId(), amount, (paid, extra) -> paid + extra);
        }

        return orders.stream().map(order -> {
            double total = order.getTotalAmount() == null ? 0.0 : order.getTotalAmount();
            double paid = paidByOrder.getOrDefault(order.getId(), 0.0);
            return new OrderOptionResponse(
                    order.getId(),
                    order.getOrderCode(),
                    order.getOrderDate(),
                    order.getStatus(),
                    order.getCustomer() == null ? null : order.getCustomer().getFullName(),
                    total,
                    paid,
                    Math.max(total - paid, 0.0),
                    contractedOrderIds.contains(order.getId()),
                    invoicedOrderIds.contains(order.getId())
            );
        }).filter(option -> !normalized.equals("payment") || option.remainingAmount() > 0.0001d)
          .toList();
    }

    @Override
    @Transactional
    public SalesOrder updateStatus(Long id, OrderStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái đơn hàng không được để trống");
        }
        // Hủy đơn phải đi qua cancel() để hoàn lại trạng thái tồn kho của từng xe.
        if (status == OrderStatus.CANCELLED) {
            return cancel(id);
        }
        SalesOrder order = getOrderById(id);
        if (status == OrderStatus.COMPLETED) {
            double paid = paymentRepository.sumPaidAmountByOrderId(id);
            if (paid + 0.0001d < (order.getTotalAmount() == null ? 0.0 : order.getTotalAmount())) {
                throw new BadRequestException("Chỉ hoàn tất đơn hàng sau khi đã thu đủ tiền");
            }
            if (!contractRepository.existsByOrder_Id(id) || !invoiceRepository.existsByOrder_Id(id)) {
                throw new BadRequestException("Cần có cả hợp đồng và hóa đơn trước khi hoàn tất đơn hàng");
            }
        }
        return orderStateService.transition(order, status, SecurityUtils.currentUsername(),
                "Cập nhật trạng thái qua API");
    }

    @Override
    @Transactional
    public SalesOrder cancel(Long id) {
        SalesOrder order = getOrderById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị huỷ trước đó");
        }
        if (contractRepository.existsByOrder_Id(id) || invoiceRepository.existsByOrder_Id(id)
                || paymentRepository.existsByOrder_Id(id)) {
            throw new BadRequestException("Đơn đã phát sinh hợp đồng, hóa đơn hoặc thanh toán. Cần xử lý điều chỉnh chứng từ trước khi hủy");
        }
        restoreStock(order);
        SalesOrder cancelled = orderStateService.transition(order, OrderStatus.CANCELLED, SecurityUtils.currentUsername(),
                "Hủy đơn hàng");
        carCatalogService.evictCatalog();
        return cancelled;
    }

    @Override
    @Transactional
    public SalesOrder update(Long id, SalesOrderRequest request) {
        validateRequest(request);
        SalesOrder order = getOrderById(id);
        assertOrderEditable(order);

        order.setEmployee(resolveEmployee(request.employeeId()));

        // Trả lại trạng thái của các xe cũ trước khi thay bằng danh sách mới
        restoreStock(order);
        order.getItems().clear();

        double total = 0.0;
        for (OrderItemRequest itemRequest : request.items()) {
            total += addItemToOrder(order, itemRequest);
        }
        order.setTotalAmount(total);

        SalesOrder saved = salesOrderRepository.save(order);
        salesOrderRepository.flush();
        carCatalogService.evictCatalog();
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

        CarItem carItem = findAvailableItem(request.carItemId());
        markItemReserved(carItem);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setCarItem(carItem);
        item.setQuantity(1);
        item.setUnitPrice(carItem.getPrice());
        order.getItems().add(item);
        recalculateTotal(order);

        salesOrderRepository.save(order);
        salesOrderRepository.flush();
        carCatalogService.evictCatalog();
        return item;
    }

    @Override
    @Transactional
    public OrderItem updateItem(Long orderId, Long itemId, OrderItemRequest request) {
        validateItem(request);
        SalesOrder order = getOrderById(orderId);
        assertOrderEditable(order);

        OrderItem item = findItem(order, itemId);

        // Trả lại trạng thái xe cũ rồi đánh dấu chiếc xe mới
        restoreItemStatus(item.getCarItem());
        CarItem carItem = findAvailableItem(request.carItemId());
        markItemReserved(carItem);

        item.setCarItem(carItem);
        item.setQuantity(1);
        item.setUnitPrice(carItem.getPrice());
        recalculateTotal(order);

        salesOrderRepository.save(order);
        salesOrderRepository.flush();
        carCatalogService.evictCatalog();
        return item;
    }

    @Override
    @Transactional
    public void deleteItem(Long orderId, Long itemId) {
        SalesOrder order = getOrderById(orderId);
        assertOrderEditable(order);

        OrderItem item = findItem(order, itemId);
        restoreItemStatus(item.getCarItem());
        order.getItems().remove(item);
        recalculateTotal(order);
        salesOrderRepository.save(order);
        carCatalogService.evictCatalog();
    }

    private OrderItem findItem(SalesOrder order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId() != null && item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dòng xe có id = " + itemId + " trong đơn hàng"));
    }

    // Thêm một chiếc xe vào đơn: giữ chỗ tồn kho, chưa ghi nhận đã bán
    private double addItemToOrder(SalesOrder order, OrderItemRequest itemRequest) {
        CarItem carItem = findAvailableItem(itemRequest.carItemId());
        markItemReserved(carItem);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setCarItem(carItem);
        item.setQuantity(1);
        item.setUnitPrice(carItem.getPrice());
        order.getItems().add(item);

        return carItem.getPrice();
    }

    private CarItem findAvailableItem(Long carItemId) {
        CarItem carItem = carItemRepository.findByIdForUpdate(carItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe có id = " + carItemId));
        if (carItem.getStatus() != CarStatus.AVAILABLE) {
            throw new BadRequestException("Xe '" + describe(carItem)
                    + "' không còn ở trạng thái có thể bán (hiện tại: " + carItem.getStatus() + ")");
        }
        return carItem;
    }

    // PENDING/CONFIRMED giữ xe ở trạng thái RESERVED; chỉ tạo hợp đồng mới chuyển SOLD.
    private void markItemReserved(CarItem carItem) {
        carItem.setStatus(CarStatus.RESERVED);
        carItemRepository.save(carItem);
    }

    private void restoreStock(SalesOrder order) {
        for (OrderItem item : order.getItems()) {
            restoreItemStatus(item.getCarItem());
        }
    }

    private void restoreItemStatus(CarItem carItem) {
        if (carItem == null) {
            return;
        }
        if (carItem.getStatus() == CarStatus.RESERVED) {
            carItem.setStatus(CarStatus.AVAILABLE);
            carItemRepository.save(carItem);
        }
    }

    private String describe(CarItem carItem) {
        String carName = carItem.getCar() == null ? null : carItem.getCar().getName();
        if (carName == null) {
            return carItem.getVin() == null ? ("#" + carItem.getId()) : carItem.getVin();
        }
        return carItem.getVin() == null ? carName : (carName + " - " + carItem.getVin());
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
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ được chỉnh sửa nội dung đơn hàng khi đơn còn ở trạng thái PENDING");
        }
        if (paymentRepository.existsByOrder_Id(order.getId())) {
            throw new BadRequestException(
                    "Không thể sửa nội dung đơn hàng sau khi đã ghi nhận thanh toán. Hãy xử lý điều chỉnh công nợ riêng.");
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
        if (item == null || item.carItemId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        // Mỗi dòng là một chiếc xe: số lượng bỏ trống hoặc bằng 1
        if (item.quantity() != null && item.quantity() != 1) {
            throw new BadRequestException("Mỗi dòng đơn là một chiếc xe nên số lượng phải bằng 1");
        }
    }
}
