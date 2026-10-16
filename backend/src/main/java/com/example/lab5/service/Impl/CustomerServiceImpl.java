package com.example.lab5.service.Impl;

import com.example.lab5.entity.Customer;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final TestDriveAppointmentRepository testDriveAppointmentRepository;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            SalesOrderRepository salesOrderRepository,
            TestDriveAppointmentRepository testDriveAppointmentRepository
    ) {
        this.customerRepository = customerRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.testDriveAppointmentRepository = testDriveAppointmentRepository;
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

    @Override
    @Transactional
    public Customer update(Long id, Customer customer) {
        validateCustomer(customer);
        Customer existing = getCustomerById(id);
        existing.setFullName(customer.getFullName());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        existing.setNationalId(customer.getNationalId());
        return customerRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getCustomerById(id);
        if (salesOrderRepository.existsByCustomer_Id(id)
                || testDriveAppointmentRepository.existsByCustomer_Id(id)) {
            throw new BadRequestException("Không thể xoá khách vì đã có đơn/lịch");
        }
        customerRepository.deleteById(id);
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
