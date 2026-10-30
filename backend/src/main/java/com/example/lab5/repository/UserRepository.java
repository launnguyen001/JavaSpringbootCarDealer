package com.example.lab5.repository;

import com.example.lab5.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /** Tìm tối đa 20 tài khoản đang hoạt động, không bao gồm người đang đăng nhập. */
    java.util.List<User> findTop20ByEnabledTrueAndIdNotAndUsernameContainingIgnoreCaseOrderByUsernameAsc(
            Long excludedUserId, String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
