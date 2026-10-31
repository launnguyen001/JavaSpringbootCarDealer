package com.example.lab5.service.Impl;

import com.example.lab5.dto.CarItemDetailResponse;
import com.example.lab5.dto.CarSpecificationResponse;
import com.example.lab5.dto.ContractResponse;
import com.example.lab5.dto.InvoiceResponse;
import com.example.lab5.dto.OrderSummary;
import com.example.lab5.dto.PaymentResponse;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.CarItemMapper;
import com.example.lab5.mapper.CarSpecificationMapper;
import com.example.lab5.mapper.ContractMapper;
import com.example.lab5.mapper.InvoiceMapper;
import com.example.lab5.mapper.PaymentMapper;
import com.example.lab5.repository.OrderItemRepository;
import com.example.lab5.service.CarItemDetailService;
import com.example.lab5.service.CarItemService;
import com.example.lab5.service.CarSpecificationService;
import com.example.lab5.service.ContractService;
import com.example.lab5.service.InvoiceService;
import com.example.lab5.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarItemDetailServiceImpl implements CarItemDetailService {

    private final CarItemService carItemService;
    private final CarItemMapper carItemMapper;
    private final CarSpecificationService carSpecificationService;
    private final CarSpecificationMapper carSpecificationMapper;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;
    private final ContractService contractService;
    private final ContractMapper contractMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public CarItemDetailServiceImpl(
            CarItemService carItemService,
            CarItemMapper carItemMapper,
            CarSpecificationService carSpecificationService,
            CarSpecificationMapper carSpecificationMapper,
            OrderItemRepository orderItemRepository,
            InvoiceService invoiceService,
            InvoiceMapper invoiceMapper,
            ContractService contractService,
            ContractMapper contractMapper,
            PaymentService paymentService,
            PaymentMapper paymentMapper
    ) {
        this.carItemService = carItemService;
        this.carItemMapper = carItemMapper;
        this.carSpecificationService = carSpecificationService;
        this.carSpecificationMapper = carSpecificationMapper;
        this.orderItemRepository = orderItemRepository;
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
        this.contractService = contractService;
        this.contractMapper = contractMapper;
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CarItemDetailResponse getDetail(Long carItemId) {
        CarItem item = carItemService.getById(carItemId);
        CarSpecificationResponse specifications = null;
        try {
            var spec = carSpecificationService.getByCarId(item.getCar().getId());
            specifications = carSpecificationMapper.toResponse(spec, item.getCar().getName());
        } catch (ResourceNotFoundException ignored) {
            // Một dòng xe có thể chưa được khai báo thông số kỹ thuật trên MongoDB.
        }

        OrderItem orderItem = orderItemRepository.findFirstByCarItem_IdOrderByIdDesc(carItemId).orElse(null);
        if (orderItem == null) {
            return new CarItemDetailResponse(
                    carItemMapper.toResponse(item), specifications, null, null, null, List.of());
        }

        SalesOrder order = orderItem.getOrder();
        OrderSummary relatedOrder = new OrderSummary(
                order.getId(),
                order.getOrderCode(),
                order.getOrderDate(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getTotalAmount(),
                order.getCustomer() == null ? null : order.getCustomer().getFullName(),
                order.getEmployee() == null ? null : order.getEmployee().getFullName()
        );

        InvoiceResponse invoice = null;
        try {
            invoice = invoiceMapper.toResponse(invoiceService.getByOrder(order.getId()));
        } catch (ResourceNotFoundException ignored) {
            // Đơn có thể chưa được lập hóa đơn.
        }

        ContractResponse contract = null;
        try {
            contract = contractMapper.toResponse(contractService.getByOrder(order.getId()));
        } catch (ResourceNotFoundException ignored) {
            // Đơn có thể chưa được lập hợp đồng.
        }

        List<PaymentResponse> payments = paymentService.getByOrder(order.getId())
                .stream().map(paymentMapper::toResponse).toList();

        return new CarItemDetailResponse(
                carItemMapper.toResponse(item), specifications, relatedOrder, invoice, contract, payments);
    }
}
