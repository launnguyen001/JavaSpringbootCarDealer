package com.example.lab5.service.Impl;

import com.example.lab5.dto.ContractRequest;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.repository.CarItemRepository;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.ContractService;
import com.example.lab5.service.CarCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final NotificationEventFactory notificationEvents;
    private final CarItemRepository carItemRepository;
    private final CarCatalogService carCatalogService;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            SalesOrderRepository salesOrderRepository,
            NotificationEventFactory notificationEvents,
            CarItemRepository carItemRepository,
            CarCatalogService carCatalogService
    ) {
        this.contractRepository = contractRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.notificationEvents = notificationEvents;
        this.carItemRepository = carItemRepository;
        this.carCatalogService = carCatalogService;
    }

    @Override
    @Transactional
    public Contract create(ContractRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
        if (request.contractNumber() == null || request.contractNumber().isBlank()) {
            throw new BadRequestException("Số hợp đồng không được để trống");
        }
        SalesOrder order = salesOrderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng có id = " + request.orderId()));

        if (contractRepository.findByOrder_Id(request.orderId()).isPresent()) {
            throw new BadRequestException("Đơn hàng đã có hợp đồng");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ được tạo hợp đồng sau khi đơn hàng đã CONFIRMED");
        }
        if (order.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng chưa có xe để lập hợp đồng");
        }
        for (OrderItem item : order.getItems()) {
            if (item.getCarItem() == null || item.getCarItem().getStatus() != CarStatus.RESERVED) {
                throw new BadRequestException("Xe trong đơn không còn ở trạng thái RESERVED; không thể ký hợp đồng");
            }
        }

        Contract contract = new Contract();
        contract.setOrder(order);
        contract.setContractNumber(request.contractNumber().trim());
        contract.setContent(request.content());
        contract.setSignedAt(request.signedAt() == null ? LocalDateTime.now() : request.signedAt());
        Contract saved = contractRepository.save(contract);
        for (OrderItem item : order.getItems()) {
            item.getCarItem().setStatus(CarStatus.SOLD);
            carItemRepository.save(item.getCarItem());
        }
        carCatalogService.evictCatalog();
        notificationEvents.contractCreated(saved);
        return saved;
    }

    @Override
    public List<Contract> getAllContracts() {
        return contractRepository.findAllByOrderBySignedAtDescIdDesc();
    }

    @Override
    public Contract getContractById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng có id = " + id));
    }

    @Override
    public Contract getByOrder(Long orderId) {
        return contractRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hợp đồng của đơn hàng id = " + orderId));
    }

    @Override
    @Transactional
    public Contract update(Long id, ContractRequest request) {
        if (request == null || request.contractNumber() == null || request.contractNumber().isBlank()) {
            throw new BadRequestException("Số hợp đồng không được để trống");
        }
        Contract contract = getContractById(id);
        String contractNumber = request.contractNumber().trim();
        contractRepository.findByContractNumber(contractNumber)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Số hợp đồng đã tồn tại");
                });
        contract.setContractNumber(contractNumber);
        contract.setContent(request.content());
        if (request.signedAt() != null) {
            contract.setSignedAt(request.signedAt());
        }
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getContractById(id);
        throw new BadRequestException("Không thể xóa hợp đồng đã phát hành vì hợp đồng là căn cứ chuyển xe sang SOLD. Hãy dùng quy trình điều chỉnh hợp đồng.");
    }
}
