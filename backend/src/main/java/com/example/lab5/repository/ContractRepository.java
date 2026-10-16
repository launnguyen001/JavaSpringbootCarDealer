package com.example.lab5.repository;

import com.example.lab5.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByContractNumber(String contractNumber);

    Optional<Contract> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);

    /** Id các đơn hàng đã có hợp đồng. */
    @Query("SELECT c.order.id FROM Contract c")
    List<Long> findAllOrderIds();
}
