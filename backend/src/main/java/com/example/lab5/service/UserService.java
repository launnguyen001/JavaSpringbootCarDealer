package com.example.lab5.service;

import com.example.lab5.dto.UserResponse;

import java.util.List;

/** Quản trị tài khoản người dùng (dành cho ADMIN) + người dùng tự cập nhật. */
public interface UserService {

    /** Danh sách toàn bộ tài khoản (không trả mật khẩu). */
    List<UserResponse> listAll();

    /** Khoá / mở khoá tài khoản. */
    UserResponse updateStatus(Long id, boolean enabled);

    /** Đổi vai trò tài khoản. Nhận tên vai trò dạng chuỗi (ADMIN/EMPLOYEE/USER). */
    UserResponse updateRole(Long id, String roleName);

    /** Người dùng đang đăng nhập tự cập nhật email / mật khẩu của chính mình. */
    UserResponse updateMe(String username, String email, String newPassword);
}
