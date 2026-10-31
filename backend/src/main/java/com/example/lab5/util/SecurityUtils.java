package com.example.lab5.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Tiện ích đọc thông tin người dùng đang đăng nhập từ SecurityContext. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Username hiện tại, hoặc {@code "system"} khi chạy nền/seed/test. */
    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> authority.equals("ROLE_" + role));
    }

    public static boolean isStaff() {
        return hasRole("ADMIN") || hasRole("EMPLOYEE");
    }
}
