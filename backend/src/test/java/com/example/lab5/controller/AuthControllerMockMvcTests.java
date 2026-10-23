package com.example.lab5.controller;

import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerMockMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        entityManager.flush();

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEmail("admin@cardealer.vn");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setCreatedAt(java.time.LocalDateTime.now());
        userRepository.save(admin);

        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user"));
        user.setEmail("user@cardealer.vn");
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        entityManager.flush();
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        String json = """
            {
              "username": "%s",
              "password": "%s"
            }
            """.formatted(username, password);

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void loginSuccessReturnsToken() throws Exception {
        loginAndExtractToken("admin", "admin");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "admin",
                              "password": "admin"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "admin",
                              "password": "wrongpassword"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginNonExistentUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "nonexistent",
                              "password": "anything"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMissingFieldsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerUserReturnsCreated() throws Exception {
        // Đăng ký công khai: KHÔNG gửi `role` — backend phải tự gán USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "newuser",
                              "password": "password123",
                              "email": "newuser@cardealer.vn"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void registerIgnoresClientRoleAndStaysUser() throws Exception {
        // Chống leo quyền: client gửi role=ADMIN vẫn chỉ được tạo tài khoản USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "hacker",
                              "password": "password123",
                              "email": "hacker@cardealer.vn",
                              "role": "ADMIN"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerMissingPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "nopassword",
                              "email": "nopassword@cardealer.vn"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerDuplicateUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "admin",
                              "password": "password123",
                              "email": "dup@cardealer.vn"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminDashboardWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDashboardWithUserTokenReturnsForbidden() throws Exception {
        // Đăng nhập user rồi lấy token
        String loginJson = """
            {
              "username": "user",
              "password": "user"
            }
            """;
        String tokenBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Trích xuất token từ JSON (đơn giản)
        String token = extractToken(tokenBody);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboardWithAdminTokenReturnsOk() throws Exception {
        String loginJson = """
            {
              "username": "admin",
              "password": "admin"
            }
            """;
        String tokenBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = extractToken(tokenBody);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void meWithAdminTokenReturnsAdminInfo() throws Exception {
        String loginJson = """
            {
              "username": "admin",
              "password": "admin"
            }
            """;
        String tokenBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = extractToken(tokenBody);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String extractToken(String jsonBody) {
        // Trích xuất đơn giản giá trị của trường "token" từ JSON
        int start = jsonBody.indexOf("\"token\":\"") + 9;
        int end = jsonBody.indexOf('"', start);
        return jsonBody.substring(start, end);
    }
}
