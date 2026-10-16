package com.example.lab5.repository;

import com.example.lab5.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullNameContainingIgnoreCaseOrderByIdAsc(String fullName);

    List<Customer> findByPhoneContainingOrderByIdAsc(String phone);

    boolean existsByEmail(String email);

    /** Hồ sơ khách gắn với một tài khoản đăng nhập (dùng cho /api/customers/me). */
    Optional<Customer> findByUser_Id(Long userId);

    /** Tìm khách theo email — dùng để kiểm tra trùng khi user tự cập nhật hồ sơ. */
    Optional<Customer> findByEmail(String email);
}
