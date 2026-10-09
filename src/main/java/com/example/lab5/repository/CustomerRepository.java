package com.example.lab5.repository;

import com.example.lab5.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullNameContainingIgnoreCaseOrderByIdAsc(String fullName);

    List<Customer> findByPhoneContainingOrderByIdAsc(String phone);

    boolean existsByEmail(String email);
}
