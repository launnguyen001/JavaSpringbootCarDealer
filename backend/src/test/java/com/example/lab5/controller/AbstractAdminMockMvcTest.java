package com.example.lab5.controller;

import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class cho các MockMvc test cần gọi endpoint yêu cầu quyền ADMIN.
 * Tự đảm bảo tài khoản admin tồn tại, đăng nhập lấy JWT và cung cấp asAdmin() để gắn header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractAdminMockMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String adminToken;

    @BeforeEach
    void initAdminToken() throws Exception {
        ensureUser("admin", "admin", Role.ADMIN);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        this.adminToken = extractToken(body);
    }

    /** Gắn header Authorization: Bearer <adminToken> vào request. */
    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + adminToken);
    }

    protected void ensureUser(String username, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(username + "@cardealer.vn");
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    static String extractToken(String jsonBody) {
        int start = jsonBody.indexOf("\"token\":\"") + 9;
        int end = jsonBody.indexOf('"', start);
        return jsonBody.substring(start, end);
    }
}
