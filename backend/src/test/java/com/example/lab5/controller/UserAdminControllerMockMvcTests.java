package com.example.lab5.controller;

import com.example.lab5.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test API quản trị tài khoản người dùng (/api/admin/users)
 * và API người dùng tự cập nhật (/api/auth/me, /api/customers/me).
 */
class UserAdminControllerMockMvcTests extends AbstractAdminMockMvcTest {

    // ---------- GET /api/admin/users ----------

    @Test
    void listUsersReturnsUsersWithoutPassword() throws Exception {
        ensureUser("t5_list", "password123", Role.USER);

        mockMvc.perform(asAdmin(get("/api/admin/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].enabled").exists())
                // không bao giờ trả mật khẩu ra ngoài
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    // ---------- PUT /api/admin/users/{id}/status ----------

    @Test
    void lockThenUnlockUser() throws Exception {
        Long id = ensureUserAndGetId("t5_lock", Role.USER);

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void lockedUserCannotLogin() throws Exception {
        ensureUser("t5_locked_login", "password123", Role.USER);
        Long id = userRepository.findByUsername("t5_locked_login").orElseThrow().getId();

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        // Tài khoản bị khoá -> đăng nhập phải bị từ chối 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"t5_locked_login\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotLockSelf() throws Exception {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();

        mockMvc.perform(asAdmin(put("/api/admin/users/" + adminId + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingEnabledReturnsBadRequest() throws Exception {
        Long id = ensureUserAndGetId("t5_nostatus", Role.USER);

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT /api/admin/users/{id}/role ----------

    @Test
    void updateRoleToEmployee() throws Exception {
        Long id = ensureUserAndGetId("t5_role", Role.USER);

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/role"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void cannotDemoteRootAdmin() throws Exception {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();

        mockMvc.perform(asAdmin(put("/api/admin/users/" + adminId + "/role"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRoleReturnsBadRequest() throws Exception {
        Long id = ensureUserAndGetId("t5_badrole", Role.USER);

        mockMvc.perform(asAdmin(put("/api/admin/users/" + id + "/role"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Phân quyền ----------

    @Test
    void userCannotAccessAdminUsers() throws Exception {
        ensureUser("t5_plain", "password123", Role.USER);
        String token = loginToken("t5_plain", "password123");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PUT /api/auth/me ----------

    @Test
    void updateMeChangesEmailAndPassword() throws Exception {
        ensureUser("t5_me", "password123", Role.USER);
        String token = loginToken("t5_me", "password123");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "t5_me_updated@example.com",
                              "password": "newpassword456"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("t5_me_updated@example.com"))
                .andExpect(jsonPath("$.username").value("t5_me"));

        // mật khẩu mới phải đăng nhập được
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"t5_me\",\"password\":\"newpassword456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMeShortPasswordReturnsBadRequest() throws Exception {
        ensureUser("t5_me_short", "password123", Role.USER);
        String token = loginToken("t5_me_short", "password123");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET/PUT /api/customers/me ----------

    @Test
    void customerMeIsCreatedOnFirstUpdate() throws Exception {
        ensureUser("t5_profile", "password123", Role.USER);
        String token = loginToken("t5_profile", "password123");

        // Chưa có hồ sơ -> PUT tạo mới
        mockMvc.perform(put("/api/customers/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "fullName": "Khách Test Me",
                              "email": "t5_profile@example.com",
                              "phone": "0900000001",
                              "address": "1 Đường Test, TP.HCM",
                              "nationalId": "TEST-CCCD-0001"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Khách Test Me"));

        // Đọc lại được
        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Khách Test Me"))
                .andExpect(jsonPath("$.phone").value("0900000001"));
    }

    @Test
    void customerMeWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerMeRequiresFullName() throws Exception {
        ensureUser("t5_profile_bad", "password123", Role.USER);
        String token = loginToken("t5_profile_bad", "password123");

        mockMvc.perform(put("/api/customers/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private Long ensureUserAndGetId(String username, Role role) {
        ensureUser(username, "password123", role);
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    private String loginToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractToken(body);
    }
}
