package com.example.lab5.service.Impl;

import com.example.lab5.dto.ActivityLogRequest;
import com.example.lab5.dto.UserResponse;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.UserMapper;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.ActivityLogService;
import com.example.lab5.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    // Tài khoản quản trị gốc: không cho phép khoá hoặc hạ quyền để tránh tự vô hiệu hoá hệ thống
    private static final String ROOT_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            ActivityLogService activityLogService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, boolean enabled) {
        User user = findUser(id);

        if (!enabled) {
            // Chặn tự khoá chính mình và chặn khoá tài khoản quản trị gốc
            if (ROOT_ADMIN_USERNAME.equals(user.getUsername())) {
                throw new BadRequestException("Không thể khoá tài khoản quản trị gốc");
            }
            if (user.getUsername().equals(currentUsername())) {
                throw new BadRequestException("Không thể tự khoá tài khoản đang đăng nhập");
            }
        }

        user.setEnabled(enabled);
        User saved = userRepository.save(user);

        writeLog(enabled ? "MỞ KHOÁ TÀI KHOẢN" : "KHOÁ TÀI KHOẢN", saved,
                "Tài khoản " + saved.getUsername() + " → " + (enabled ? "hoạt động" : "bị khoá"));
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, String roleName) {
        User user = findUser(id);
        Role role = parseRole(roleName);

        // Không cho hạ quyền tài khoản quản trị gốc
        if (ROOT_ADMIN_USERNAME.equals(user.getUsername()) && role != Role.ADMIN) {
            throw new BadRequestException("Không thể hạ quyền tài khoản quản trị gốc");
        }

        user.setRole(role);
        User saved = userRepository.save(user);

        writeLog("ĐỔI VAI TRÒ", saved, "Tài khoản " + saved.getUsername() + " → " + role.name());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateMe(String username, String email, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + username));

        if (email != null && !email.isBlank()) {
            String normalized = email.trim();
            boolean changed = user.getEmail() == null || !normalized.equalsIgnoreCase(user.getEmail());
            if (changed && userRepository.existsByEmail(normalized)) {
                throw new BadRequestException("Email đã được sử dụng");
            }
            user.setEmail(normalized);
        }

        boolean passwordChanged = newPassword != null && !newPassword.isBlank();
        if (passwordChanged) {
            if (newPassword.length() < 4) {
                throw new BadRequestException("Mật khẩu cần ít nhất 4 ký tự");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        User saved = userRepository.save(user);

        writeLog("CẬP NHẬT TÀI KHOẢN", saved,
                passwordChanged ? "Người dùng đổi email/mật khẩu" : "Người dùng đổi email");
        return userMapper.toResponse(saved);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng có id = " + id));
    }

    private Role parseRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BadRequestException("Vai trò không được để trống");
        }
        try {
            return Role.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Vai trò không hợp lệ: " + roleName
                    + ". Chỉ nhận các giá trị " + Arrays.toString(Role.values()));
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    // Ghi vết thao tác quản trị lên MongoDB; lỗi ghi log không được làm hỏng nghiệp vụ chính
    // (môi trường test không có MongoDB nên phải bọc try/catch).
    private void writeLog(String action, User target, String description) {
        try {
            activityLogService.log(new ActivityLogRequest(
                    action,
                    "USER",
                    target.getId(),
                    description,
                    currentUsername(),
                    LocalDateTime.now()
            ));
        } catch (Exception ignored) {
            // bỏ qua
        }
    }
}
