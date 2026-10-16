package com.example.lab5.service.Impl;

import com.example.lab5.entity.Customer;
import com.example.lab5.entity.User;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final TestDriveAppointmentRepository testDriveAppointmentRepository;
    private final UserRepository userRepository;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            SalesOrderRepository salesOrderRepository,
            TestDriveAppointmentRepository testDriveAppointmentRepository,
            UserRepository userRepository
    ) {
        this.customerRepository = customerRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.testDriveAppointmentRepository = testDriveAppointmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Customer create(Customer customer) {
        validateCustomer(customer);
        customer.setId(null);
        customer.setUser(null);
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
        applyData(existing, customer);
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

    // ---------- Hồ sơ "của tôi" (user tự cập nhật) ----------

    @Override
    public Customer getMyProfile(String username) {
        User user = findUser(username);
        return customerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tài khoản chưa có hồ sơ khách hàng. Hãy cập nhật để tạo hồ sơ."));
    }

    @Override
    @Transactional
    public Customer updateMyProfile(String username, Customer data) {
        validateCustomer(data);
        User user = findUser(username);

        // Chưa có hồ sơ thì tạo mới và gắn luôn vào tài khoản đang đăng nhập
        Customer customer = customerRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Customer created = new Customer();
                    created.setUser(user);
                    return created;
                });

        // Email khách phải không trùng với hồ sơ khách khác
        String newEmail = normalize(data.getEmail());
        if (newEmail != null) {
            customerRepository.findByEmail(newEmail).ifPresent(other -> {
                if (!other.getId().equals(customer.getId())) {
                    throw new BadRequestException("Email đã được khách hàng khác sử dụng");
                }
            });
        }

        applyData(customer, data);
        return customerRepository.save(customer);
    }

    // ---------- Helpers ----------

    private void applyData(Customer target, Customer source) {
        target.setFullName(source.getFullName());
        target.setEmail(normalize(source.getEmail()));
        target.setPhone(normalize(source.getPhone()));
        target.setAddress(normalize(source.getAddress()));
        target.setNationalId(normalize(source.getNationalId()));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
