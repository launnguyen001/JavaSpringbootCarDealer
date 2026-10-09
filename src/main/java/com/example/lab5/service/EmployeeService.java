package com.example.lab5.service;

import com.example.lab5.entity.Employee;

import java.util.List;

public interface EmployeeService {

    Employee create(Employee employee);

    List<Employee> getAllEmployees();

    Employee getEmployeeById(Long id);

    List<Employee> searchByName(String name);
}
