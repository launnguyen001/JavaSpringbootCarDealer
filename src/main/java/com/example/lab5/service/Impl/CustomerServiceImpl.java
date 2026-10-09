package com.example.lab5.service.Impl;

import com.example.lab5.entity.Customer;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer create(Customer customer) {
        validateCustomer(customer);
        customer.setId(null);
        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng có id = " + id));
    }

    @Override
    public List<Customer> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Từ khóa tên khách hàng không được để trống");
        }
        return customerRepository.findByFullNameContainingIgnoreCaseOrderByIdAsc(name.trim());
    }

    @Override
    public List<Customer> searchByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        return customerRepository.findByPhoneContainingOrderByIdAsc(phone.trim());
    }

    private void validateCustomer(Customer customer) {
        if (customer == null || customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new BadRequestException("Họ tên khách hàng không được để trống");
        }
        if (customer.getFullName().length() > 150) {
            throw new BadRequestException("Họ tên khách hàng không được vượt quá 150 ký tự");
        }
    }
}
