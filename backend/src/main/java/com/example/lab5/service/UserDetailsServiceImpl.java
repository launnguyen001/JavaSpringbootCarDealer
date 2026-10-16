package com.example.lab5.service;

import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

// Nạp user từ DB và chuyển thành UserDetails của Spring Security
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản đã bị khóa: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                mapAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(Role role) {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                .stream()
                .collect(Collectors.toList());
    }
}
