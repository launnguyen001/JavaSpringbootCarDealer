package com.example.lab5.controller;

import com.example.lab5.dto.UserResponse;
import com.example.lab5.dto.UserRoleRequest;
import com.example.lab5.dto.UserStatusRequest;
import com.example.lab5.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Quản trị tài khoản người dùng — chỉ ADMIN (bảo vệ bằng "/api/admin/**" trong SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listAll();
    }

    @PutMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return userService.updateStatus(id, request.enabled());
    }

    @PutMapping("/{id}/role")
    public UserResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleRequest request
    ) {
        return userService.updateRole(id, request.role());
    }
}
