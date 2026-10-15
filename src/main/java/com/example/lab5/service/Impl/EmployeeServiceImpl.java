package com.example.lab5.service.Impl;

import com.example.lab5.entity.Employee;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final TestDriveAppointmentRepository testDriveAppointmentRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            SalesOrderRepository salesOrderRepository,
            TestDriveAppointmentRepository testDriveAppointmentRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.testDriveAppointmentRepository = testDriveAppointmentRepository;
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

    @Override
    @Transactional
    public Employee update(Long id, Employee employee) {
        validateEmployee(employee);
        Employee existing = getEmployeeById(id);
        existing.setFullName(employee.getFullName());
        existing.setEmail(employee.getEmail());
        existing.setPhone(employee.getPhone());
        existing.setPosition(employee.getPosition());
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getEmployeeById(id);
        if (salesOrderRepository.existsByEmployee_Id(id)
                || testDriveAppointmentRepository.existsByEmployee_Id(id)) {
            throw new BadRequestException("Không thể xoá nhân viên vì đang phụ trách");
        }
        employeeRepository.deleteById(id);
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
