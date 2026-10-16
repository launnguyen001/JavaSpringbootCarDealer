package com.example.lab5.repository;

import com.example.lab5.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByContractNumber(String contractNumber);

    Optional<Contract> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);
}
