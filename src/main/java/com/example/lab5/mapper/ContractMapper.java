package com.example.lab5.mapper;

import com.example.lab5.dto.ContractResponse;
import com.example.lab5.entity.Contract;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getOrder() == null ? null : contract.getOrder().getId(),
                contract.getOrder() == null ? null : contract.getOrder().getOrderCode(),
                contract.getContractNumber(),
                contract.getSignedAt(),
                contract.getContent()
        );
    }
}
