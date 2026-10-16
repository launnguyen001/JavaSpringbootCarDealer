package com.example.lab5.controller;

import com.example.lab5.dto.EmployeeRequest;
import com.example.lab5.dto.EmployeeResponse;
import com.example.lab5.entity.Employee;
import com.example.lab5.mapper.EmployeeMapper;
import com.example.lab5.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    public EmployeeController(EmployeeService employeeService, EmployeeMapper employeeMapper) {
        this.employeeService = employeeService;
        this.employeeMapper = employeeMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse addEmployee(@Valid @RequestBody EmployeeRequest request) {
        return employeeMapper.toResponse(employeeService.create(employeeMapper.toEntity(request)));
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return toResponses(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.getEmployeeById(id));
    }

    @GetMapping("/search/name")
    public List<EmployeeResponse> searchByName(@RequestParam String name) {
        return toResponses(employeeService.searchByName(name));
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request
    ) {
        return employeeMapper.toResponse(employeeService.update(id, employeeMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
    }

    private List<EmployeeResponse> toResponses(List<Employee> employees) {
        return employees.stream().map(employeeMapper::toResponse).toList();
    }
}
