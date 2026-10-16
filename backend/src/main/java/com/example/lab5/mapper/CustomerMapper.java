package com.example.lab5.mapper;

import com.example.lab5.dto.CustomerRequest;
import com.example.lab5.dto.CustomerResponse;
import com.example.lab5.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer.setNationalId(request.nationalId());
        return customer;
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getNationalId()
        );
    }
}
