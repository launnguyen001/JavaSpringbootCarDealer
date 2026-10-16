package com.example.lab5.service;

import com.example.lab5.entity.Customer;

import java.util.List;

public interface CustomerService {

    Customer create(Customer customer);

    List<Customer> getAllCustomers();

    Customer getCustomerById(Long id);

    List<Customer> searchByName(String name);

    List<Customer> searchByPhone(String phone);

    Customer update(Long id, Customer customer);

    void delete(Long id);

    /** Hồ sơ khách của tài khoản đang đăng nhập. */
    Customer getMyProfile(String username);

    /** Cập nhật hồ sơ khách của tài khoản đang đăng nhập (tạo mới nếu chưa có). */
    Customer updateMyProfile(String username, Customer data);
}
