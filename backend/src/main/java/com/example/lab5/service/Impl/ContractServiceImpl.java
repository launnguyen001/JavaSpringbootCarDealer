package com.example.lab5.service.Impl;

import com.example.lab5.dto.ContractRequest;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.ContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final NotificationEventFactory notificationEvents;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            SalesOrderRepository salesOrderRepository,
            NotificationEventFactory notificationEvents
    ) {
        this.contractRepository = contractRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.notificationEvents = notificationEvents;
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
        if (contractRepository.findByOrder_Id(request.orderId()).isPresent()) {
            throw new BadRequestException("Đơn hàng đã có hợp đồng");
        }

        SalesOrder order = salesOrderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng có id = " + request.orderId()));

        Contract contract = new Contract();
        contract.setOrder(order);
        contract.setContractNumber(request.contractNumber().trim());
        contract.setContent(request.content());
        contract.setSignedAt(request.signedAt() == null ? LocalDateTime.now() : request.signedAt());
        Contract saved = contractRepository.save(contract);
        notificationEvents.contractCreated(saved);
        return saved;
    }

    @Override
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
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
        contractRepository.deleteById(id);
    }
}
