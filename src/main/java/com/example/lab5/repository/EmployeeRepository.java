package com.example.lab5.repository;

import com.example.lab5.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByFullNameContainingIgnoreCaseOrderByIdAsc(String fullName);
}
