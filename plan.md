# Kế hoạch phát triển dự án Car Dealer (full-stack, 5 buổi)

## 1. Mục tiêu

Xây dựng **website đại lý bán xe hoàn chỉnh (backend + frontend)** trên nền Spring Boot,
tuân thủ kiến trúc/quy ước của mã mẫu trong `code mẫu bên main/` và `code mẫu bên test/`.

Sản phẩm cuối cùng gồm:
- Quản lý hãng xe / dòng xe / xe trong kho + tìm kiếm nâng cao.
- Quản lý khách hàng, nhân viên, đơn hàng, hợp đồng, thanh toán, hóa đơn, lịch lái thử.
- Dữ liệu linh hoạt (thông số kỹ thuật, đánh giá, log, media) trên MongoDB.
- Cache / giỏ cấu hình / session trên Redis.
- Báo cáo thống kê doanh số (SQL thuần).
- Website frontend xem/mua xe + trang quản trị.
- Phân quyền, kiểm thử, tài liệu, triển khai.

## 2. Kiến trúc & phân chia SQL / NoSQL / Redis

| Miền nghiệp vụ | Công nghệ | Lý do |
| --- | --- | --- |
| Hãng xe `Brand`, dòng xe `CarModel`, xe `Car` | SQL (JPA) | Quan hệ phân cấp, FK, giao dịch |
| Khách hàng `Customer`, nhân viên `Employee` | SQL (JPA) | Quan hệ với đơn hàng |
| Đơn hàng `SalesOrder`, `OrderItem`, `Payment`, `Contract`, `Invoice` | SQL (JPA) | Giao dịch tài chính, ACID |
| Lịch lái thử `TestDriveAppointment` | SQL (JPA) | Quan hệ khách–xe–nhân viên |
| Thông số kỹ thuật, đánh giá, log, media | MongoDB | Schema linh hoạt, ghi nhiều, lồng nhau |
| Cache tìm kiếm/chi tiết xe, giỏ cấu hình, session | Redis | Truy cập nhanh, TTL |
| Báo cáo doanh số / tồn kho | SQL thuần (JdbcTemplate) | Query tổng hợp/join phức tạp |

**Cấu trúc package backend hiện tại:**

```
com.example.lab5
├── config            (CacheConfig, DataSeeder)
├── controller
├── dto               (record request/response)
├── entity            (SQL/JPA)
├── document          (NoSQL/MongoDB)
├── mapper
├── repository        (SQL repositories + JdbcTemplate)
├── mongorepository   (NoSQL repositories)
├── service
│   └── Impl
└── exception
```

## 3. Lộ trình 5 buổi

> Đã hoàn thành **Buổi 1 + Buổi 2**, chuẩn bị sang **Buổi 3**.

### Buổi 1 — Phân tích, thiết kế CSDL & backend SQL lõi  [HOÀN THÀNH]
- Khảo sát mã mẫu, chốt kiến trúc đa tầng.
- Thiết kế ERD SQL (Brand/CarModel/Car + Customer/Employee/Order/Payment/Contract/Invoice/TestDrive).
- Dựng `entity`, `dto`, `mapper`, `repository` (JPA + derived/JPQL + JdbcTemplate), `service/Impl`, `controller`.
- Cấu hình MySQL prod + H2 test, `GlobalExceptionHandler`.
- CRUD + tìm kiếm xe chạy được.

### Buổi 2 — Hoàn thiện backend đa tầng  [HOÀN THÀNH]
- MongoDB (4 document), Redis (cache/cart/session), báo cáo SQL thuần, Swagger/OpenAPI,
  seed dữ liệu, README.
- **CRUD đầy đủ cho mọi tài nguyên SQL** (`POST`, `GET` list, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`),
  gồm `GET /{id}` cho Brand; quản lý dòng xe trong đơn `/api/orders/{orderId}/items`.
- **Guard xoá** chặn xoá khi còn tham chiếu (Brand→CarModel, CarModel→Car, Car→OrderItem/TestDrive,
  Customer/Employee→đơn/lịch, SalesOrder→phải `CANCELLED` và không có chứng từ).
- **NoSQL:** `CarReview` thêm list/update/delete; `CarSpecification`/`CarMedia` thêm list.
- **47 test PASS**; `.\mvnw.cmd clean package` build thành công.
- Deliverable: backend REST đủ CRUD, tài liệu hóa, test xanh.

### Buổi 3 — Khởi tạo Frontend & tính năng công khai
- Khởi tạo `frontend/` (Next.js App Router + TypeScript + Tailwind CSS), cấu hình rewrite `/api` và CORS backend (tùy chọn).
- Lớp `api/` gọi REST; layout, routing, trang chủ.
- Trang **danh sách xe** (phân trang), **chi tiết xe** (ảnh từ `car-media`, thông số từ `car-specs`,
  đánh giá từ `car-reviews`), **tìm kiếm/lọc nâng cao** (theo tên/giá/tồn kho/hãng/dòng).
- Deliverable: website xem/sản phẩm xe hoạt động, dữ liệu lấy từ backend.

### Buổi 4 — Frontend nghiệp vụ & tài khoản
- Giỏ cấu hình (gọi `/api/cart`, lưu theo `sessionId`); đặt hàng (`/api/orders`).
- Thanh toán (`/api/payments`), lịch lái thử (`/api/test-drives`), gửi đánh giá (`/api/car-reviews`).
- Đăng nhập/đăng xuất với session Redis (`/api/sessions`).
- Trang **quản trị** CRUD: brands, car-models, cars, customers, employees, orders.
- Deliverable: luồng mua xe end-to-end + trang admin.

### Buổi 5 — Bảo mật, triển khai & hoàn thiện
- Phân quyền: Spring Security + JWT (admin / nhân viên bán hàng / khách), bảo vệ endpoint admin.
- Upload media xe, dashboard báo cáo (chart từ `/api/reports/*`).
- `docker-compose.yml` (MySQL + MongoDB + Redis), seed, biến môi trường.
- Kiểm thử end-to-end, tối ưu cache, xuất `openapi.yaml`, hoàn thiện tài liệu.
- Deliverable: website full-stack chạy end-to-end, có phân quyền, đóng gói triển khai.

## 4. Quy ước theo layer (bám mã mẫu)

| Layer | Quy ước | Ghi chú |
| --- | --- | --- |
| `entity` | JPA `@Entity`, `@Table`, Lombok, loại trừ quan hệ khỏi `toString/equals` | Quan hệ 2 chiều |
| `dto` | Java `record` Request/Response | Dùng lại cho cả create + update |
| `mapper` | `@Component`, `toEntity`/`toResponse` | |
| `repository` | JPA (derived + `@Query`) hoặc `JdbcTemplate` cho SQL thuần | |
| `service`+`Impl` | Interface + `@Service`, `@Transactional` cho ghi, validate private | |
| `controller` | `@RestController`, `@RequestMapping("/api/...")` | |
| `exception` | Dùng lại `BadRequestException`, `ResourceNotFoundException`, `GlobalExceptionHandler` | không tạo trùng |

## 5. Tiêu chí hoàn thành (Definition of Done)

- Mỗi bảng quan trọng đủ `POST`, `GET` list, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`
  (trừ ngoại lệ đã thống nhất trong `task.md`).
- Validate đầy đủ; lỗi trả về qua `GlobalExceptionHandler` (400/404/409/500).
- Dữ liệu giao dịch trong MySQL, linh hoạt trong MongoDB, cache/session trong Redis.
- `.\mvnw.cmd test` xanh toàn bộ; `.\mvnw.cmd clean package` build thành công.
- Swagger hiển thị đủ endpoint.

## 6. Rủi ro & lưu ý

- **Nhất quán SQL–NoSQL:** MongoDB tham chiếu mềm `carId`; cần kiểm tra tồn tại khi đọc/xoá.
- **Encoding:** lưu file mới dạng UTF-8 để tránh lỗi font tiếng Việt.
- **Không phá vỡ mã mẫu:** giữ cấu trúc package/tên hàm/kiểu trả về.
- **Profile test:** dùng `application-test.properties` + H2; Mongo/Redis chỉ tạo bean, không kết nối.
