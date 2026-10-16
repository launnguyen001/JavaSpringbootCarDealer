package com.example.lab5.mapper;

import com.example.lab5.dto.UserResponse;
import com.example.lab5.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
