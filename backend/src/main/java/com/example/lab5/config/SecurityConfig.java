package com.example.lab5.config;

import com.example.lab5.access.JwtAccessDeniedHandler;
import com.example.lab5.entrypoint.JwtAuthenticationEntryPoint;
import com.example.lab5.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Cấu hình bảo mật Spring Security: JWT, stateless, phân quyền RBAC theo HTTP method.
// Thứ tự requestMatchers rất quan trọng (first-match-wins).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Quy tắc phân quyền:
    //  PUBLIC   : xem catalog (GET cars/brands/car-models/car-reviews/car-specs/car-media), auth login/register/logout, swagger
    //  USER     : gửi đánh giá, đặt hàng, đăng ký lái thử, tạo hồ sơ khách, giỏ hàng, xem đơn/lịch của mình
    //  EMPLOYEE : xử lý nghiệp vụ (khách, đơn, lịch, thanh toán, hợp đồng, hóa đơn, media/specs, kiểm duyệt đánh giá, log)
    //  ADMIN    : toàn quyền + CRUD hãng/dòng/xe/nhân viên + báo cáo/dashboard + /api/admin/**
    private static final String ADMIN = "ADMIN";
    private static final String EMPLOYEE = "EMPLOYEE";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---- Trang lỗi mặc định của Spring phải công khai ----
                        // Nếu không, mọi lỗi 500 (ví dụ NPE) sẽ bị chuyển sang /error rồi bị chặn
                        // và bị mask thành 401 "Chưa xác thực hoặc phiên làm việc đã hết hạn".
                        .requestMatchers("/error").permitAll()

                        // ---- Tài liệu API ----
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // ---- Auth công khai ----
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()

                        // ---- Đọc catalog công khai (GET) ----
                        .requestMatchers(HttpMethod.GET,
                                "/api/cars", "/api/cars/**",
                                "/api/car-items", "/api/car-items/**",
                                "/api/brands", "/api/brands/**",
                                "/api/car-models", "/api/car-models/**",
                                "/api/car-reviews", "/api/car-reviews/**",
                                "/api/car-media", "/api/car-media/**",
                                "/api/car-specs", "/api/car-specs/**"
                        ).permitAll()

                        // ---- Ghi catalog: ADMIN ----
                        .requestMatchers(HttpMethod.POST, "/api/cars", "/api/cars/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/cars", "/api/cars/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/cars", "/api/cars/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/cars", "/api/cars/**").hasRole(ADMIN)
                        // Từng chiếc xe vật lý: đọc công khai, ghi chỉ ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/car-items", "/api/car-items/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/car-items", "/api/car-items/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/car-items", "/api/car-items/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/car-items", "/api/car-items/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/brands", "/api/brands/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/brands", "/api/brands/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/brands", "/api/brands/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/car-models", "/api/car-models/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/car-models", "/api/car-models/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/car-models", "/api/car-models/**").hasRole(ADMIN)

                        // ---- Nhân viên: xem = staff, CRUD = ADMIN ----
                        .requestMatchers(HttpMethod.POST, "/api/employees", "/api/employees/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/employees", "/api/employees/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/employees", "/api/employees/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/**").hasAnyRole(EMPLOYEE, ADMIN)

                        // ---- Báo cáo & Dashboard: ADMIN ----
                        .requestMatchers("/api/reports/**").hasRole(ADMIN)
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)

                        // ---- Người dùng tự phục vụ (mọi role đã đăng nhập) ----
                        // Phải đặt TRƯỚC rule staff của "/api/customers/**" bên dưới
                        .requestMatchers(HttpMethod.GET, "/api/customers/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/customers/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/customers").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/car-reviews").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/car-reviews/*/replies").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/test-drives").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        // Xem đơn / lịch lái thử của chính mình (theo customerId)
                        .requestMatchers(HttpMethod.GET, "/api/orders/customer/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/test-drives/customer/**").authenticated()

                        // ---- Kiểm duyệt/nội dung: staff ----
                        .requestMatchers(HttpMethod.PUT, "/api/car-reviews/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/car-reviews/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/car-media/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/car-media/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/car-specs/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/car-specs/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/activity-logs/**").hasAnyRole(EMPLOYEE, ADMIN)

                        // ---- Nghiệp vụ còn lại (khách, đơn, lịch, thanh toán, hợp đồng, hóa đơn): staff ----
                        .requestMatchers("/api/customers/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/orders/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/test-drives/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/payments/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/contracts/**").hasAnyRole(EMPLOYEE, ADMIN)
                        .requestMatchers("/api/invoices/**").hasAnyRole(EMPLOYEE, ADMIN)

                        // ---- Mặc định: phải đăng nhập ----
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
