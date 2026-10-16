# Task — Buổi 3 (HÔM NAY): Hoàn thiện backend, xác thực & phân quyền JWT (user + admin)

> Mục tiêu: **hoàn thiện backend** Car Dealer — bổ sung tài khoản **user** và phân quyền **admin** bằng **JWT**:
> đăng nhập (`/api/auth/login`), đăng xuất (`/api/auth/logout`), đăng ký user (tùy chọn), bảo vệ endpoint admin
> (chỉ `ADMIN`), cho phép `USER`/`EMPLOYEE` truy cập endpoint khách hàng. Bám pattern hiện có
> (`entity → dto → mapper → repository → service/Impl → controller`) và dùng lại `GlobalExceptionHandler`.
>
> Tham chiếu: `report.md` (báo cáo hiện trạng), `plan.md` Buổi 3.
>
> **Trạng thái: CHƯA BẮT ĐẦU** — tài liệu này là kế hoạch chốt trước khi viết code. Không phá mã mẫu,
> không sửa test backend hiện có; chỉ thêm mã mới và cấu hình bảo mật.
>
> **Quan trọng: Buổi này CHỈ làm backend. Không động vào mã nguồn frontend.** Frontend sẽ consume các
> endpoint JWT này ở Buổi sau (Buổi 4).
>
> Sau khi xong: `.\mvnw.cmd test` phải xanh (giữ nguyên test hiện có + test auth mới), `.\mvnw.cmd clean package`
> build thành công, Swagger hiển thị endpoint auth.

---

## 0. Quyết định thiết kế (chốt sẵn — có thể đổi)

1. **Entity `User` (SQL/JPA):** `id`, `username` (unique), `password` (mã hóa BCrypt), `role` (`ADMIN` | `USER` | `EMPLOYEE`), `enabled` (boolean). Table `users`.
2. **Role:** `ROLE_ADMIN` (quản trị toàn hệ thống), `ROLE_USER` (khách hàng), `ROLE_EMPLOYEE` (nhân viên bán hàng).
3. **JWT:** `JwtUtils` sinh JWT chứa claims `sub` (= username), `roles`, `exp`; secretKey & expiration đọc từ `application.properties` (`app.jwt.secret`, `app.jwt.expiration-ms`). Không lưu JWT trong Redis (stateless); đăng xuất = xóa session Redis tương ứng + instruk client xóa token (hoặc giữ blacklist ngắn hạn nếu muốn huỷ ngay).
4. **Spring Security:** `WebSecurityFilterChain` — bật BCrypt `PasswordEncoder`, `UserDetailsService` từ `UserRepository`, `JwtAuthenticationFilter` đặt trước `UsernamePasswordAuthenticationFilter`.
5. **Đường dẫn bảo vệ:**
   - `POST /api/auth/**`: public (không cần auth).
   - `GET /api/brands`, `GET /api/cars`, `GET /api/car-models`, `GET /api/car-reviews/…`…: public (xem sản phẩm), hoặc yêu cầu `USER` tùy quyết định.
   - `/api/admin/**` (admin quản lý hãng/xe/khách/đơn…): chỉ `ADMIN`.
   - Endpoint liên quan khách hàng (`/api/customers/me`, đơn hàng của mình…): `USER`/`EMPLOYEE`.
   - Các endpoint còn lại: bảo vệ tuỳ granularity sau khi chốt.
6. **Thay thế session Redis bằng JWT (hôm nay):** `/api/sessions` (session UUID lưu Redis) được thay thế bằng JWT stateless (`/api/auth/login` trả token, client gửi `Authorization: Bearer <jwt>`). Có thể giữ `/api/sessions` để tương thích cũ hoặc đánh dấu sẽ loại bỏ ở Buổi 5 — tùy quyết định. `/api/auth/logout` instruct client xóa JWT (hoặc thêm blacklist ngắn hạn nếu muốn huỷ ngay).
7. **Mã hóa mật khẩu:** BCrypt qua `PasswordEncoder` bean; không lưu mật khẩu rõ.
8. **Không phá mã mẫu:** cấu trúc package/tên hàm/kiểu trả về hiện tại giữ nguyên; thêm file mới vào package hiện có.

---

## 1. Ma trận endpoint MỤC TIÊU

| Tính năng | Method | Path | Quyền | Mô tả |
|---|---|---|---|---|
| Đăng nhập | `POST` | `/api/auth/login` | Public | Nhận `username`, `password` → trả JWT (+ user, role). |
| Đăng xuất | `POST` | `/api/auth/logout` | Authenticated | Hủy session/token của người dùng hiện tại. |
| Đăng ký user | `POST` | `/api/auth/register` | Public | Tạo tài khoản `USER` (tùy chọn, có thể bật/tắt). |
| Xem hồ sơ | `GET` | `/api/auth/me` | Authenticated | Trả thông tin user hiện tại (từ JWT). |
| Admin: quản lý user | `GET/PUT/DELETE` | `/api/admin/users[/{id}]` | `ADMIN` | CRUD tài khoản user (tùy chọn). |
| Admin: bảo vệ ví dụ | `GET` | `/api/admin/dashboard` | `ADMIN` | Endpoint mẫu chỉ admin truy cập. |

> Endpoint admin cụ thể sẽ được chốt trong Buổi 5 (`plan.md`: "bảo vệ endpoint admin"). Ở task này chỉ chốt cơ chế auth + ví dụ bảo vệ.

---

## 2. Checklist công việc theo file

### Nhóm A — Dữ liệu user & role
- [ ] `entity/User.java`: `@Entity` `users` — `id`, `username` (unique), `password`, `role` (`enum Role {ADMIN, USER, EMPLOYEE}`), `enabled`, `createdAt`.
- [ ] `repository/UserRepository.java`: `Optional<User> findByUsername(String)`, `boolean existsByUsername(String)`.
- [ ] `DataSeeder.java` (hoặc file seed riêng): seed tài khoản `admin`/`admin` (`ROLE_ADMIN`), `user`/`user` (`ROLE_USER`), `employee`/`employee` (`ROLE_EMPLOYEE`) — chạy khi `app.seed.enabled=true`, bỏ qua profile `test`.
- [ ] `dto/AuthRequest.java` (`username`, `password`), `dto/AuthResponse.java` (`token`, `username`, `role`, `expiresIn`), `dto/UserRequest.java` / `dto/UserResponse.java` (tùy chọn cho đăng ký/CRUD user).

### Nhóm B — JWT
- [ ] `config/JwtUtils.java`: `generateToken(UserDetails)`, `extractUsername(token)`, `extractRoles(token)`, `validateToken(token, UserDetails)`, `extractExpiration(token)`, `extractAllClaims`; đọc `app.jwt.secret`, `app.jwt.expiration-ms`.
- [ ] `application.properties`: thêm `app.jwt.secret=...`, `app.jwt.expiration-ms=86400000` (ví dụ).

### Nhóm C — Spring Security
- [ ] `config/SecurityConfig.java`: `@Configuration` `WebSecurityFilterChain` — `PasswordEncoder` (BCrypt), `/api/auth/**` permitAll, `/api/admin/**` hasRole(`ADMIN`), các endpoint còn lại authenticated (hoặc theo chốt mục 0.5), session stateless.
- [ ] `service/UserDetailsServiceImpl.java`: implements `UserDetailsService`, load user từ `UserRepository`, map role → `GrantedAuthority`.
- [ ] `filter/JwtAuthenticationFilter.java`: `OncePerRequestFilter`, trích xuất JWT từ header `Authorization: Bearer ...`, validate, set `SecurityContextHolder`; đặt trước `UsernamePasswordAuthenticationFilter`.
- [ ] `entrypoint/JwtAuthenticationEntryPoint.java` (tùy chọn): trả 401 khi không xác thực.
- [ ] `access/JwtAccessDeniedHandler.java` (tùy chọn): trả 403 khi không đủ quyền.

### Nhóm D — Service & controller auth
- [ ] `service/AuthService.java` + `service/Impl/AuthServiceImpl.java`: `login(authRequest)` → sinh JWT, trả `AuthResponse`; `logout(token)` → invalidate session nếu có; `register(userRequest)` → tạo `USER`, mã hóa password; `getCurrentUser()`.
- [ ] `controller/AuthController.java`: `POST /api/auth/login`, `POST /api/auth/logout`, `POST /api/auth/register` (tùy chọn), `GET /api/auth/me`.
- [ ] `controller/AdminController.java` (tùy chọn): `GET /api/admin/dashboard` chỉ `ADMIN` làm ví dụ bảo vệ.
- [ ] Cập nhật `GlobalExceptionHandler` nếu cần xử lý `UsernameNotFoundException`, `BadCredentialsException`, `ExpiredJwtException`, `MalformedJwtException` → 401/403.

### Nhóm E — Kiểm thử
- [ ] `controller/AuthControllerMockMvcTests` (mới): `POST /api/auth/login` sai mật khẩu → 401; tài khoản không tồn tại → 401; đăng nhập thành công → 200 có token; `POST /api/auth/logout` → 200/204.
- [ ] Test bảo vệ: endpoint `/api/admin/dashboard` với token `USER` → 403; với token `ADMIN` → 200; chưa đăng nhập → 401.
- [ ] Test JWT hết hạn → 401; token malformed → 401.
- [ ] Chạy `.\mvnw.cmd test` — tất cả xanh (giữ nguyên 47 test hiện có + test auth mới).

### Nhóm F — Tài liệu
- [ ] Cập nhật `README.md`: mục mới "8. Xác thực & phân quyền (User + JWT)" — endpoint auth, cách đăng nhập/admin, role, Swagger.
- [ ] Cập nhật `plan.md`: đánh dấu Buổi 3 (hoàn thiện backend + JWT thay session) đã thực hiện; Buổi 4 chỉ build frontend, không thiep lại backend.
- [ ] Kiểm tra Swagger `/swagger-ui.html` hiển thị endpoint `/api/auth/*`.

---

## 3. Chi tiết dữ liệu mẫu gợi ý (seed)

| username | password (mã hóa BCrypt) | role | Mô tả |
|---|---|---|---|
| admin | admin | `ADMIN` | Quản trị viên |
| user | user | `USER` | Khách hàng |
| employee | employee | `EMPLOYEE` | Nhân viên bán hàng |

> Mật khẩu lưu dạng BCrypt trong DB; trong `application.properties` hoặc `DataSeeder` dùng `passwordEncoder.encode(...)`.

---

## 4. Acceptance criteria

- [ ] `POST /api/auth/login` đúng → 200 trả JWT hợp lệ (có thể decode xem `username`, `role`).
- [ ] `POST /api/auth/login` sai → 401 thông báo rõ.
- [ ] `POST /api/auth/logout` → hủy phiên, trả 200/204.
- [ ] Endpoint admin (`/api/admin/**`) chỉ `ADMIN` truy cập (200); `USER` → 403; chưa đăng nhập → 401.
- [ ] JWT hết hạn / token không hợp lệ → 401.
- [ ] `.\mvnw.cmd test` xanh toàn bộ (47 test cũ + test auth mới).
- [ ] `.\mvnw.cmd clean package` build thành công.
- [ ] Swagger hiển thị đầy đủ endpoint auth và phân quyền.

--- 
