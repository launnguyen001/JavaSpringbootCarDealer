package com.example.lab5.service.Impl;

import com.example.lab5.dto.AuthResponse;
import com.example.lab5.dto.UserResponse;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.config.JwtUtils;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.AuthService;
import com.example.lab5.service.UserDetailsServiceImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Xử lý nghiệp vụ đăng nhập/đăng xuất/đăng ký bằng JWT
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService,
            JwtUtils jwtUtils,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse login(String username, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new UsernameNotFoundException("Sai tên đăng nhập hoặc mật khẩu");
        }

        String token = jwtUtils.generateToken(userDetails);

        return new AuthResponse(
                token,
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                86400000L
        );
    }

    @Override
    public void logout() {
        // JWT stateless: client xóa token côté client. Ở đây chỉ xóa SecurityContext nếu có.
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public UserResponse register(String username, String password, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: " + role);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.valueOf(role));
        user.setEnabled(true);
        user.setCreatedAt(java.time.LocalDateTime.now());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public UserResponse getCurrentUser() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new IllegalStateException("Không có người dùng đăng nhập");
        }
        String username = ((UserDetails) principal).getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    private boolean isValidRole(String role) {
        try {
            Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
