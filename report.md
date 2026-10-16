# Báo cáo: Xác thực & phân quyền (User + JWT) — đăng nhập/đăng xuất admin/user

> Báo cáo hiện trạng và khuyến nghị bổ sung tính năng tài khoản (user) và phân quyền admin
> bằng JWT cho dự án **Car Dealer — Backend REST API** (xem `README.md`).

---

## 1. Mục tiêu của báo cáo

- Báo cáo hiện trạng cơ chế đăng nhập/đăng xuất hiện có.
- Xác định thiếu hở so với yêu cầu "user – admin đăng nhập/đăng xuất" (xác thực + phân quyền).
- Khuyến nghị kế hoạch bổ sung (chi tiết trong `task2.md`), dùng làm cơ sở cho Buổi 4–5 theo `plan.md`.

---

## 2. Hiện trạng (theo README.md + mã nguồn)

### 2.1. Cơ chế hiện có: `/api/sessions` (session Redis, KHÔNG phải JWT)

| Endpoint | Method | Mô tả |
|---|---|---|
| `/api/sessions` | `POST` | Tạo session: nhận `userId` → trả `token` (UUID), lưu Redis key `session:<token>` → `userId`, TTL 30 phút. |
| `/api/sessions/{token}` | `GET` | Tra `userId` từ token; lỗi nếu token không tồn tại/hết hạn. |
| `/api/sessions/{token}` | `DELETE` | Hủy session (đăng xuất): xóa key Redis. |

Lớp liên quan: `controller/SessionController.java`, `service/SessionService.java` (dùng `StringRedisTemplate`).

### 2.2. Chưa có

| Thành phần | Trạng thái |
|---|---|
| `User` entity / `Customer`-tài khoản | ❌ Chưa có |
| Role `ADMIN` / `USER` (nhân viên bán hàng, khách) | ❌ Chưa có |
| Spring Security (`spring-boot-starter-security`) | ❌ Chưa có trong `pom.xml` |
| JWT (tạo/validate/trích xuất token) | ❌ Chưa có |
| Endpoint đăng nhập thật (`/api/auth/login` …) | ❌ Chưa có; chỉ có `/api/sessions` tạo session bằng `userId` thô |
| Endpoint đăng ký tài khoản user | ❌ Chưa có |
| Bảo vệ endpoint admin (chỉ ADMIN truy cập) | ❌ Tất cả endpoint hiện tại đều **public** |
| Kết nối session ↔ user thực tế (Customer/Employee) | ❌ Chưa có |

### 2.3. Tham chiếu kế hoạch (`plan.md`)

- **Buổi 4:** "Đăng nhập/đăng xuất với session Redis (`/api/sessions`)", trang quản trị CRUD.
- **Buổi 5:** "Phân quyền: Spring Security + JWT (admin / nhân viên bán hàng / khách), bảo vệ endpoint admin".

---

## 3. Thiếu hở so với yêu cầu "user – admin đăng nhập/đăng xuất"

1. **Không có bảng tài khoản:** Không có entity `User` (username, password, role, enabled…) nên không thể xác thực ai là user hay admin.
2. **Không phân quyền:** Không có `ROLE_ADMIN` / `ROLE_USER` → không thể bảo vệ endpoint admin (quản lý hãng/xe/khách/đơn…).
3. **Token không phải JWT:** Hiện tại dùng UUID ngẫu nhiên lưu Redis (session cơ bản). Yêu cầu JWT → cần ký token, chứa claims (userId, role, exp), validate stateless hoặc trỏ Redis cho blacklist/revoke.
4. **Không có endpoint auth chuẩn:** Cần `/api/auth/login` (trả JWT), `/api/auth/logout` (hủy JWT/refresh token), tùy chọn `/api/auth/register` (đăng ký user).
5. **Mọi endpoint public:** Không có filter bảo vệ → admin endpoint có thể bị truy cập bởi user thường hoặc khách chưa đăng nhập.

---

## 4. Khuyến nghị kế hoạch bổ sung (xem chi tiết `task2.md`)

| Khối | Nội dung chính |
|---|---|
| **Dữ liệu** | Thêm entity `User` (id, username, password, role `ADMIN`/`USER`/`EMPLOYEE`, enabled), `UserRepository`, seed tài khoản admin + user mẫu. |
| **JWT** | `JwtUtils`: sinh JWT (username, role, exp), validate, trích xuất claims; secret/expiration qua `application.properties`. |
| **Bảo mật** | Spring Security: `WebSecurityFilterChain` (mã hóa BCrypt, `UserDetailsService` từ `UserRepository`), `JwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter`, bảo vệ `/api/admin/**` chỉ `ADMIN`, `/api/**` yêu cầu xác thực (tùy endpoint). |
| **API auth** | `AuthController`: `POST /api/auth/login` (trả JWT), `POST /api/auth/logout` (hủy session/token), tùy chọn `POST /api/auth/register` (user). |
| **Kiểm thử** | Test login thành công/sai mật khẩu/không tồn tại; JWT hết hạn; admin truy cập endpoint admin (200) vs user bị chặn (403); logout. |
| **Tài liệu** | Cập nhật `README.md` (mục Xác thực/Phân quyền), `plan.md` (đánh dấu Buổi 4/5 liên quan), Swagger hiển thị endpoint auth. |

---

## 5. Kết luận

`report.md` được tạo vì file chưa tồn tại. Hiện tại dự án chỉ có session Redis cơ bản (`/api/sessions`), **chưa có user, JWT, phân quyền admin**. Để thực hiện "đăng nhập/đăng xuất user – admin", cần bổ sung theo kế hoạch trong `task2.md` (được ưu tiên ở Buổi 4–5 theo `plan.md`).
