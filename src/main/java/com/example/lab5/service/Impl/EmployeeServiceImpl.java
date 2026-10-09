package com.example.lab5.service.Impl;

import com.example.lab5.entity.Employee;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Employee create(Employee employee) {
        validateEmployee(employee);
        employee.setId(null);
        return employeeRepository.save(employee);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có id = " + id));
    }

    @Override
    public List<Employee> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Từ khóa tên nhân viên không được để trống");
        }
        return employeeRepository.findByFullNameContainingIgnoreCaseOrderByIdAsc(name.trim());
    }

    private void validateEmployee(Employee employee) {
        if (employee == null || employee.getFullName() == null || employee.getFullName().isBlank()) {
            throw new BadRequestException("Họ tên nhân viên không được để trống");
        }
        if (employee.getFullName().length() > 150) {
            throw new BadRequestException("Họ tên nhân viên không được vượt quá 150 ký tự");
        }
    }
}
