package com.example.lab5.service;

import com.example.lab5.entity.Customer;

import java.util.List;

public interface CustomerService {

    Customer create(Customer customer);

    List<Customer> getAllCustomers();

    Customer getCustomerById(Long id);

    List<Customer> searchByName(String name);

    List<Customer> searchByPhone(String phone);
}
