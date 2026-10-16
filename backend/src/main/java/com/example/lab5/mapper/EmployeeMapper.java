package com.example.lab5.mapper;

import com.example.lab5.dto.EmployeeRequest;
import com.example.lab5.dto.EmployeeResponse;
import com.example.lab5.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        if (request == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setPosition(request.position());
        return employee;
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPosition()
        );
    }
}
