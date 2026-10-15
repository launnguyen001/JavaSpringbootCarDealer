package com.example.lab5.service;

import com.example.lab5.dto.ContractRequest;
import com.example.lab5.entity.Contract;

import java.util.List;

public interface ContractService {

    Contract create(ContractRequest request);

    List<Contract> getAllContracts();

    Contract getContractById(Long id);

    Contract getByOrder(Long orderId);

    Contract update(Long id, ContractRequest request);

    void delete(Long id);
}
