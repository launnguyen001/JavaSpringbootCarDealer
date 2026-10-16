package com.example.lab5.controller;

import com.example.lab5.dto.CustomerRequest;
import com.example.lab5.dto.CustomerResponse;
import com.example.lab5.entity.Customer;
import com.example.lab5.mapper.CustomerMapper;
import com.example.lab5.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse addCustomer(@Valid @RequestBody CustomerRequest request) {
        return customerMapper.toResponse(customerService.create(customerMapper.toEntity(request)));
    }

    // ---------- Hồ sơ của chính người dùng đang đăng nhập ----------
    // (đặt trước "/{id}" để "/me" không bị hiểu là id)

    @GetMapping("/me")
    public CustomerResponse getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return customerMapper.toResponse(customerService.getMyProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public CustomerResponse updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CustomerRequest request
    ) {
        return customerMapper.toResponse(
                customerService.updateMyProfile(userDetails.getUsername(), customerMapper.toEntity(request))
        );
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return toResponses(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerMapper.toResponse(customerService.getCustomerById(id));
    }

    @GetMapping("/search/name")
    public List<CustomerResponse> searchByName(@RequestParam String name) {
        return toResponses(customerService.searchByName(name));
    }

    @GetMapping("/search/phone")
    public List<CustomerResponse> searchByPhone(@RequestParam String phone) {
        return toResponses(customerService.searchByPhone(phone));
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        return customerMapper.toResponse(customerService.update(id, customerMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
    }

    private List<CustomerResponse> toResponses(List<Customer> customers) {
        return customers.stream().map(customerMapper::toResponse).toList();
    }
}
