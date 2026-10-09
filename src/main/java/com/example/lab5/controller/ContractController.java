package com.example.lab5.controller;

import com.example.lab5.dto.ContractRequest;
import com.example.lab5.dto.ContractResponse;
import com.example.lab5.entity.Contract;
import com.example.lab5.mapper.ContractMapper;
import com.example.lab5.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractMapper contractMapper;

    public ContractController(ContractService contractService, ContractMapper contractMapper) {
        this.contractService = contractService;
        this.contractMapper = contractMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse createContract(@Valid @RequestBody ContractRequest request) {
        return contractMapper.toResponse(contractService.create(request));
    }

    @GetMapping
    public List<ContractResponse> getAllContracts() {
        return toResponses(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ContractResponse getContractById(@PathVariable Long id) {
        return contractMapper.toResponse(contractService.getContractById(id));
    }

    @GetMapping("/order/{orderId}")
    public ContractResponse getByOrder(@PathVariable Long orderId) {
        return contractMapper.toResponse(contractService.getByOrder(orderId));
    }

    private List<ContractResponse> toResponses(List<Contract> contracts) {
        return contracts.stream().map(contractMapper::toResponse).toList();
    }
}
