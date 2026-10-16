package com.example.lab5.controller;

import com.example.lab5.dto.AuthRequest;
import com.example.lab5.dto.AuthResponse;
import com.example.lab5.dto.UpdateMeRequest;
import com.example.lab5.dto.UserResponse;
import com.example.lab5.service.AuthService;
import com.example.lab5.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(
                request.username(),
                request.password(),
                request.email()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    // Người dùng tự cập nhật thông tin đăng nhập (email / mật khẩu) của chính mình
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        UserResponse response = userService.updateMe(
                userDetails.getUsername(),
                request.email(),
                request.password()
        );
        return ResponseEntity.ok(response);
    }

    // Đăng ký công khai: KHÔNG nhận `role` từ client (chống leo quyền).
    // Tài khoản tạo ra luôn là USER; chỉ ADMIN mới đổi được vai trò qua /api/admin/users/{id}/role.
    public record RegisterRequest(
            @NotBlank(message = "Tên đăng nhập không được để trống")
            String username,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 4, message = "Mật khẩu cần ít nhất 4 ký tự")
            String password,

            @Email(message = "Email không hợp lệ")
            String email
    ) {
    }
}
