# Car Dealer — Backend REST API

Hệ thống backend cho **đại lý bán xe**, xây dựng trên Spring Boot 4, kết hợp
**MySQL (SQL)** cho nghiệp vụ giao dịch, **MongoDB (NoSQL)** cho dữ liệu linh hoạt,
**Redis** cho cache/session, và **JdbcTemplate** cho báo cáo thống kê.

## 1. Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ / Build | Java 21, Maven (wrapper 3.9.16) |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| CSDL giao dịch | MySQL (`ngoclaudb`) |
| CSDL linh hoạt | MongoDB (`cardealer`) |
| Cache / session | Redis |
| Tài liệu API | springdoc OpenAPI 3 + Swagger UI |
| Test | JUnit 5, Mockito, MockMvc, H2 in-memory |

## 2. Kiến trúc & phân chia SQL / NoSQL

| Miền nghiệp vụ | Công nghệ |
| --- | --- |
| Hãng xe, dòng xe, xe/tồn kho | SQL (JPA) |
| Khách hàng, nhân viên, đơn hàng, hợp đồng, thanh toán, hóa đơn, lịch lái thử | SQL (JPA) |
| Thông số kỹ thuật, đánh giá, log hoạt động, media | NoSQL (MongoDB) |
| Cache tìm kiếm/chi tiết xe, giỏ cấu hình, session | Redis |
| Báo cáo doanh số / tồn kho | SQL thuần (JdbcTemplate) |

Cấu trúc package:

```
com.example.lab5
├── config        (cache Redis, seed dữ liệu)
├── controller    (REST controllers)
├── dto           (record request/response)
├── entity        (SQL/JPA)
├── document      (NoSQL/MongoDB)
├── mapper
├── repository    (SQL repositories + JdbcTemplate)
├── mongorepository (NoSQL repositories)
├── service
│   └── Impl
└── exception
```

## 3. Chạy ứng dụng

**Bắt buộc:** MySQL 3306. **Tùy chọn (có thể chạy sau):** MongoDB 27017, Redis 6379.

```powershell
# cấu hình kết nối trong backend/src/main/resources/application.properties
cd backend
.\mvnw.cmd spring-boot:run
```

- Ứng dụng: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Chạy chỉ với MySQL (MongoDB / Redis chưa cài)

Cấu hình mặc định hiện tại hỗ trợ chạy **chỉ với MySQL** — dùng để xem danh sách xe và test CRUD admin:

| Thuộc tính | Giá trị | Khi nào cần đổi |
| --- | --- | --- |
| `spring.cache.type` | `simple` | Đặt lại `redis` sau khi cài Redis (bật `CacheConfig`) |
| `spring.data.mongodb.auto-index-creation` | `false` | Đặt lại `true` sau khi cài MongoDB (tự tạo index) |

Hành vi khi thiếu dịch vụ:

- **Cache:** với `spring.cache.type=simple`, `CacheConfig` không được tạo → `@Cacheable`/`@CacheEvict`
  tại `CarCatalogService` thành no-op → danh sách/chi tiết xe vẫn chạy bình thường.
- **Seeder:** phần SQL (MySQL) được seed đầy đủ; phần NoSQL được **bỏ qua kèm cảnh báo** nếu
  MongoDB không phản hồi (kiểm tra TCP trước, chờ ≤1.5s — không treo chờ driver).
- **Chưa dùng được khi thiếu dịch vụ:** endpoint `/api/cart/**` và `/api/sessions` (cần Redis),
  `/api/car-reviews/**`, `/api/car-media/**`, `/api/car-specs/**`, `/api/activity-logs/**` (cần MongoDB).

Dữ liệu mẫu tự động được seed khi khởi động (`app.seed.enabled=true`, bỏ qua ở profile `test`),
thực hiện bởi `config/DataSeeder.java`. Seeder có tính **idempotent** (bỏ qua nếu dữ liệu đã tồn tại)
và seed MongoDB trong `try/catch` để ứng dụng vẫn khởi động được khi MongoDB chưa chạy.

**Dữ liệu SQL (MySQL):**

| Thực thể | Số lượng | Ghi chú |
| --- | --- | --- |
| `Brand` | 9 | Toyota, Honda, Ford, Mercedes-Benz, BMW, Audi, Lexus, Porsche, VinFast |
| `CarModel` | 22 | 2–3 dòng xe mỗi hãng (Sedan, SUV, Bán tải…) |
| `Car` | 23 | Đủ trạng thái `AVAILABLE` / `RESERVED` / `SOLD` / `MAINTENANCE` |
| `Customer` | 6 | Có email / CMND |
| `Employee` | 4 | Có chức vụ |
| `User` (tài khoản JWT) | 3 | `admin`/`admin`, `user`/`user`, `employee`/`employee` |
| `SalesOrder` + `OrderItem` | 14 | Rải khắp tháng 1 → 10/2026, đủ 4 trạng thái |
| `Payment` | 14 | `PAID` / `PARTIAL` / `UNPAID` / `REFUNDED`, nhiều phương thức |
| `Contract` / `Invoice` | 9 / 9 | Cho các đơn `COMPLETED` |
| `TestDriveAppointment` | 8 | 3 lịch `SCHEDULED` (cho dashboard) |

**Dữ liệu NoSQL (MongoDB):** `CarSpecification` và `CarMedia` cho **mỗi xe**, `CarReview` (8 đánh giá,
có reply), `ActivityLog` (8 bản ghi).

> Để **seed lại từ đầu**, hãy xoá dữ liệu cũ (drop database `ngoclaudb` / `cardealer`,
> hoặc `TRUNCATE` các bảng SQL) rồi khởi động lại — seeder chỉ chèn khi bảng còn trống.

## 4. Chạy test

Test chạy trên **H2 in-memory**, profile `test` (không cần MySQL/MongoDB/Redis thật):

```powershell
cd backend
.\mvnw.cmd test
```

Kết quả hiện tại: **84 test PASS** (bao gồm 16 test phân quyền `SecurityAuthorizationTests` và 9 test dashboard `ReportControllerMockMvcTests`).

## 5. API chính

Mỗi tài nguyên SQL dưới đây đều có đủ **CRUD**: `POST` (tạo), `GET` (danh sách),
`GET /{id}` (chi tiết), `PUT /{id}` (cập nhật), `DELETE /{id}` (xoá).

| Nhóm | Endpoint cơ bản | Endpoint riêng |
| --- | --- | --- |
| Hãng xe | `/api/brands` | `/api/brands/{id}` |
| Dòng xe | `/api/car-models` | `/api/car-models/{id}`, `/api/car-models/search/name`, `/api/car-models/brand/{brandId}` |
| Xe | `/api/cars` | `/api/cars/{id}`, `/api/cars/search/name`, `/api/cars/search/price`, `/api/cars/search/stock`, `/api/cars/search/advanced`, `/api/cars/search/advanced/brand`, `/api/cars/brand/{id}`, `/api/cars/model/{id}` |
| Khách hàng | `/api/customers` | `/api/customers/{id}`, `/api/customers/search/name`, `/api/customers/search/phone` |
| Nhân viên | `/api/employees` | `/api/employees/{id}`, `/api/employees/search/name` |
| Đơn hàng | `/api/orders` | `/api/orders/{id}`, `/api/orders/customer/{customerId}`, `PATCH /api/orders/{id}/status`, `POST /api/orders/{id}/cancel`, `/api/orders/{orderId}/items[/{itemId}]` (CRUD dòng xe) |
| Thanh toán | `/api/payments` | `/api/payments/{id}`, `/api/payments/order/{orderId}` |
| Hợp đồng | `/api/contracts` | `/api/contracts/{id}`, `/api/contracts/order/{orderId}` |
| Hóa đơn | `/api/invoices` | `/api/invoices/{id}`, `/api/invoices/order/{orderId}` |
| Lịch lái thử | `/api/test-drives` | `/api/test-drives/{id}`, `/api/test-drives/customer/{customerId}`, `PATCH /api/test-drives/{id}/status`, `POST /api/test-drives/{id}/cancel` |
| Thông số kỹ thuật (Mongo) | `/api/car-specs` | `/api/car-specs/{carId}` (`PUT` để lưu, `DELETE` để xoá) |
| Đánh giá (Mongo) | `/api/car-reviews` | `/api/car-reviews/{id}`, `/api/car-reviews/car/{carId}`, `/api/car-reviews/car/{carId}/summary`, `POST /api/car-reviews/{reviewId}/replies` |
| Log hoạt động (Mongo) | `/api/activity-logs` | `/api/activity-logs/{id}`, `/api/activity-logs/entity/{type}/{id}` |
| Media (Mongo) | `/api/car-media` | `/api/car-media/{carId}` (`PUT` để lưu, `DELETE` để xoá) |
| Giỏ cấu hình (Redis) | `/api/cart` | theo `sessionId` |
| Xác thực | `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout` | JWT (Bearer token) |
| Session (Redis) | `/api/sessions` | `POST` đăng nhập, `DELETE` đăng xuất |
| Báo cáo (SQL thuần) | `/api/reports/top-selling-cars`, `/api/reports/inventory-by-brand`, `/api/reports/revenue-summary`, `/api/reports/orders-by-status` | |
| Dashboard & drilldown | `/api/reports/dashboard-summary`, `/api/reports/revenue-by-month`, `/api/reports/inventory-by-brand/{brandId}/models`, `/api/reports/inventory-by-model/{modelId}/cars`, `/api/reports/top-selling-cars/{carId}/orders`, `/api/reports/orders-by-status/{status}` | |

### Xác thực & phân quyền (RBAC)

Ứng dụng dùng **JWT stateless** (`Authorization: Bearer <token>`), quy tắc phân quyền tập trung tại
`config/SecurityConfig.java` theo HTTP method (so khớp **first-match-wins**).

| Vai trò (`Role`) | Quyền chính |
| --- | --- |
| 🟢 Công khai | `GET` catalog: `/api/cars`, `/api/brands`, `/api/car-models`, `/api/car-specs`, `/api/car-media`, `/api/car-reviews/**`; `/api/auth/**`; Swagger |
| 🔵 Đã đăng nhập (`USER`/`EMPLOYEE`/`ADMIN`) | `/api/auth/me`, `/api/cart/**`, `GET /api/orders/customer/**`, `GET /api/test-drives/customer/**`, `POST /api/customers`, `POST /api/orders`, `POST /api/test-drives`, `POST /api/car-reviews/**` |
| 🟠 `EMPLOYEE` hoặc `ADMIN` | CRUD khách hàng/đơn hàng/lịch lái thử/thanh toán/hợp đồng/hóa đơn; `GET/PUT/DELETE` media & specs; `PUT/DELETE` đánh giá; `GET` nhân viên; nhật ký hoạt động |
| 🔴 `ADMIN` | Ghi catalog (xe/hãng/dòng xe), CRUD nhân viên, `/api/reports/**`, `/api/admin/**` |

> Lưu ý: `USER` tự tạo hồ sơ `Customer` (`POST /api/customers`) rồi dùng `customerId` cho đơn hàng /
> lái thử / đánh giá (chưa có liên kết FK giữa `User` và `Customer`).


### Chính sách xoá (guard)

`DELETE` sẽ trả **400** kèm thông báo khi tài nguyên còn dữ liệu tham chiếu:

| Xoá | Bị chặn khi |
| --- | --- |
| Brand | còn `CarModel` |
| CarModel | còn `Car` |
| Car | có `OrderItem` hoặc `TestDriveAppointment` |
| Customer | có `SalesOrder` hoặc `TestDriveAppointment` |
| Employee | có `SalesOrder` hoặc `TestDriveAppointment` |
| SalesOrder | không phải `CANCELLED`, hoặc đã có Payment/Contract/Invoice |
| Payment / Contract / Invoice / TestDrive | (không có tham chiếu) → xoá tự do |

## 7. Nghiệp vụ tồn kho

`SalesOrderServiceImpl.create` (trong `@Transactional`) kiểm tra và trừ tồn kho,
đặt xe `SOLD` khi hết hàng; `cancel` khôi phục tồn kho và trạng thái xe.
`PaymentServiceImpl` cập nhật đơn thành `COMPLETED` khi thanh toán `PAID`.

## 8. Frontend (Next.js)

Giao diện người dùng & quản trị: **Next.js 16 (App Router) + React 19 + Tailwind CSS v4**.

```powershell
cd frontend
npm install
npm run dev     # http://localhost:3000 (dev server)
npm run build   # kiểm tra build production
```

`next.config.ts` rewrite `/api/*` → `http://localhost:8080/api`, nên cần chạy backend song song.
JWT được lưu ở `localStorage`, gắn tự động vào header qua `lib/api/http.ts`.

**Base URL & CORS:** `lib/api/http.ts` tự chọn URL theo ngữ cảnh — ở **browser** luôn dùng đường dẫn
tương đối `/api` (đi qua rewrite của Next → *same-origin*, không dính CORS); ở **server (RSC)** dùng
`NEXT_PUBLIC_API_BASE_URL` (mặc định `http://localhost:8080/api`). Backend cũng cấu hình CORS cho
`app.cors.allowed-origins` (`:3000`/`:3001`) để gọi trực tiếp `http://localhost:8080` từ trình duyệt
vẫn chạy — xem `WebCorsConfig` + `.cors(withDefaults())` trong `SecurityConfig`.

Cấu trúc chính:

```
frontend/src
├── app
│   ├── page.tsx, cars/, cars/[id]/   (catalog công khai)
│   ├── login/, register/             (xác thực)
│   ├── cart/, checkout/, account/    (người dùng)
│   └── admin/                        (dashboard + 15 trang CRUD, chỉ ADMIN)
├── components
│   ├── ui/        (Button, Field, Modal, Toast, DataTable, BarChart, KpiCard…)
│   ├── admin/     (ResourceManager – CRUD tổng quát)
│   ├── car/       (CarPurchasePanel, ReviewForm)
│   └── layout/    (Header, Footer, AdminSidebar)
├── context/AuthContext.tsx
└── lib/api/*, lib/auth/token.ts, lib/utils/*
```

| Khu vực | Trang |
| --- | --- |
| Công khai | `/`, `/cars`, `/cars/[id]` |
| Xác thực | `/login`, `/register` |
| Người dùng | `/cart`, `/checkout`, `/account` |
| Quản trị (ADMIN) | `/admin` (dashboard + drilldown), `/admin/cars`, `/admin/brands`, `/admin/car-models`, `/admin/customers`, `/admin/employees`, `/admin/orders`, `/admin/test-drives`, `/admin/payments`, `/admin/contracts`, `/admin/invoices`, `/admin/reviews`, `/admin/media`, `/admin/specs`, `/admin/activity-logs` |

**Ghi chú UI:**
- `FilterPanel` (`/cars`) là **sidebar dọc 1 cột** (`lg:w-72`), không dùng grid theo breakpoint — tránh việc
  panel 288px lại chia 4 cột trên màn hình lớn (breakpoint Tailwind tính theo *viewport*, không theo container).
- `Header` có trạng thái **active** theo `usePathname()`: mục đang xem được tô vàng + gạch chân (desktop),
  nền xám + chữ vàng (mobile), kèm `aria-current="page"`.


Dashboard có **drilldown**: nhấp cột trạng thái → danh sách đơn; nhấp hãng → dòng xe → danh sách xe;
nhấp xe bán chạy → các dòng đơn chứa xe đó.

## 9. Tài liệu liên quan

- `plan.md` — kế hoạch 7 phase.
- `task2.md`, `task3.md` — đề bài & đặc tả các buổi.
- `task4.md` — kế hoạch bổ sung: RBAC đầy đủ + API dashboard/drilldown + hoàn thiện frontend.
- `report.md` — báo cáo tổng hợp.
