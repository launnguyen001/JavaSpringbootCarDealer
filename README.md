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
| Thông báo / email | Spring Mail (Gmail SMTP), `@TransactionalEventListener` + `@Async` |
| Xuất PDF | OpenPDF 1.3.39 + font Noto Sans (tiếng Việt) |
| Xuất báo cáo | Apache POI 5.4.1 (`.xlsx`) + CSV UTF-8 |
| Test | JUnit 5, Mockito, MockMvc, H2 in-memory |

## 2. Kiến trúc & phân chia SQL / NoSQL

| Miền nghiệp vụ | Công nghệ |
| --- | --- |
| Hãng xe, dòng xe (`cars`), xe vật lý (`car_items`)/tồn kho | SQL (JPA) |
| Khách hàng, nhân viên, đơn hàng, hợp đồng, thanh toán, hóa đơn, lịch lái thử | SQL (JPA) |
| Thông số kỹ thuật, đánh giá, log hoạt động, media | NoSQL (MongoDB) |
| Cache tìm kiếm/chi tiết xe, giỏ cấu hình, session | Redis |
| Báo cáo doanh số / tồn kho | SQL thuần (JdbcTemplate) |

Cấu trúc package:

```
com.example.lab5
├── config        (cache Redis, seed dữ liệu, async executor)
├── controller    (REST controllers)
├── dto           (record request/response)
├── entity        (SQL/JPA)
├── document      (NoSQL/MongoDB)
├── event         (sự kiện nghiệp vụ + listener thông báo)
├── mapper
├── repository    (SQL repositories + JdbcTemplate)
├── mongorepository (NoSQL repositories)
├── service
│   └── Impl
├── statemachine  (StateMachine tổng quát + đơn hàng/lịch lái thử/thanh toán)
├── util
└── exception
```

## 3. Chạy ứng dụng

**Bắt buộc:** MySQL 3306. **Tùy chọn (có thể chạy sau):** MongoDB 27017, Redis 6379.

### Cách A — Dùng DDEV (đã cấu hình sẵn: MariaDB + MongoDB + Redis)

Thư mục gốc dự án có sẵn `.ddev/` (project type `generic`) chạy **cả 3 dịch vụ**, đều bind ra
`127.0.0.1` đúng port mặc định nên `application.properties` không cần sửa:

| Dịch vụ | Add-on / cấu hình | Host | Tài khoản |
| --- | --- | --- | --- |
| MariaDB 10.11 | `database` trong `.ddev/config.yaml` (`host_db_port: "3306"`) | `127.0.0.1:3306` | `root/root` hoặc `db/db` |
| MongoDB 9 | add-on `ddev/ddev-mongo` + `.ddev/docker-compose.hostports.yaml` | `127.0.0.1:27017` | `db/db` (authSource `admin`) |
| Redis 8 | add-on `ddev/ddev-redis` + `.ddev/docker-compose.hostports.yaml` | `127.0.0.1:6379` | không mật khẩu |

```bash
# 1) Khởi động MariaDB + MongoDB + Redis (hook post-start tự tạo DB `ngoclaudb`)
ddev start

# 2) Chạy backend trên máy host (Java 21 + Maven wrapper)
cd backend
./mvnw spring-boot:run
```

- DB `ngoclaudb` được tạo tự động bởi `hooks.post-start` trong `.ddev/config.yaml`.
- Kiểm tra dịch vụ: `ddev describe`, `ddev mysql`, `ddev redis-cli`, `ddev mongosh`,
  hoặc UI Mongo Express: `ddev mongo-express`.
- Đổi cấu hình port/version: `.ddev/config.yaml`, rồi `ddev restart`.

> **Linux + SELinux (Enforcing):** bind-mount thư mục dự án vào container cần nhãn
> `container_file_t`, nếu không web container sẽ chết với `Permission denied` khi đọc 
> `/mnt/ddev_config`. Cấp quyền (một lần, cần `sudo`):
>
> ```bash
> sudo semanage fcontext -a -t container_file_t "/home/user/projects/JavaSpringbootCarDealer(/.*)?"
> sudo semanage fcontext -a -t container_file_t "/home/user/projects/JavaSpringbootCarDealer"
> sudo restorecon -R /home/user/projects/JavaSpringbootCarDealer
> ddev restart
> ```
>
> Ngoài ra `--release 21` của Maven cần `lib/ct.sym` — phải cài JDK đầy đủ
> (`sudo dnf install java-21-openjdk-devel`), không dùng bản `headless`/JRE thuần.

### Lưu trữ dữ liệu & sao lưu (khi chạy thật với DDEV)

Dịch vụ lưu trữ chính khi chạy app là **MariaDB của DDEV** (`ngoclaudb`), NoSQL là **MongoDB** (`cardealer`).
**H2 chỉ dùng cho test** (`scope=test` trong `pom.xml` + `application-test.properties`) — H2 không có mặt trên
classpath khi chạy thật, nên app không thể "rơi" về H2.

| Dữ liệu | Nơi lưu (Docker volume) | Sống qua `ddev stop/start/restart`? |
| --- | --- | --- |
| MariaDB `ngoclaudb` | `javaspringbootcardealer-mariadb` | ✅ (container bị xoá/tạo lại vẫn còn) |
| MongoDB `cardealer` | `ddev-javaspringbootcardealer_mongo` | ✅ |
| Redis (cache/session/cart) | `ddev-javaspringbootcardealer_redis` | ✅ (RDB snapshot mặc định) |

**Chỉ mất dữ liệu khi** chạy `ddev delete` (mặc định có tạo snapshot), `ddev delete -O`, hoặc `docker volume rm <volume>`.
Vì `spring.jpa.hibernate.ddl-auto=update`, Hibernate chỉ cập nhật schema chứ **không xoá bảng** — đừng đổi giá trị này
thành `create` / `create-drop` khi đang có dữ liệu thật.

Sao lưu / phục hồi (command tuỳ biến trong `.ddev/commands/host/`):

```bash
ddev db-backup                                  # -> .ddev/backups/mariadb-<db>-<time>.sql + mongodb-<db>-<time>.archive.gz
ddev db-restore .ddev/backups/mariadb-ngoclaudb-20261016-175522.sql
ddev db-restore .ddev/backups/mongodb-cardealer-20261016-175522.archive.gz
```

Ngoài ra DDEV có sẵn: `ddev snapshot --name=<ten>` (snapshot MariaDB), `ddev snapshot restore --name=<ten>`,
`ddev export-db --database=ngoclaudb --file=...`, `ddev import-db --file=...`.
Thư mục `.ddev/backups/` đã được thêm vào `.gitignore`.

### Cách B — Tự cài dịch vụ rồi chạy trực tiếp

```powershell
# cấu hình kết nối trong backend/src/main/resources/application.properties
cd backend
.\mvnw.cmd spring-boot:run
```

- Ứng dụng: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Chạy chỉ với MySQL (khi không có MongoDB / Redis)

Cấu hình hiện tại đã bật đủ **MariaDB + MongoDB + Redis** (xem Cách A). Nếu muốn chạy **chỉ với MySQL**
(dùng để xem danh sách xe và test CRUD admin), đổi lại các giá trị sau trong `application.properties`:

| Thuộc tính | Giá trị khi chỉ có MySQL | Giá trị hiện tại (đủ dịch vụ) |
| --- | --- | --- |
| `spring.cache.type` | `simple` | `redis` (bật `CacheConfig`) |
| `spring.data.mongodb.auto-index-creation` | `false` | `true` (tự tạo index unique/text) |

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
| `Car` (dòng xe) | 23 | Sản phẩm/phiên bản: `name`, `base_price`, `model_id` |
| `CarItem` (xe vật lý) | 85 | Từng chiếc: `vin`, `color`, `model_year`, `condition` (`NEW`/`USED`), `price`, `status`, `mileage_km` (68 `AVAILABLE`, 3 `RESERVED`, 1 `MAINTENANCE`, 13 `SOLD`) |
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

Kết quả hiện tại: **137 test PASS** (bao gồm 16 test phân quyền `SecurityAuthorizationTests`, 12 test dashboard & xuất báo cáo
`ReportControllerMockMvcTests`, 15 test quản trị người dùng `UserAdminControllerMockMvcTests`, 8 test
`CarItemControllerMockMvcTests`, cùng test mới cho state machine, lịch sử trạng thái, notification service, PDF và xuất báo cáo).

## 5. API chính

Mỗi tài nguyên SQL dưới đây đều có đủ **CRUD**: `POST` (tạo), `GET` (danh sách),
`GET /{id}` (chi tiết), `PUT /{id}` (cập nhật), `DELETE /{id}` (xoá).

| Nhóm | Endpoint cơ bản | Endpoint riêng |
| --- | --- | --- |
| Hãng xe | `/api/brands` | `/api/brands/{id}` |
| Dòng xe | `/api/car-models` | `/api/car-models/{id}`, `/api/car-models/search/name`, `/api/car-models/brand/{brandId}` |
| Xe (dòng xe) | `/api/cars` | `/api/cars/{id}` (kèm `items[]` từng chiếc), `/api/cars/{id}/items`, `/api/cars/search/name`, `/api/cars/search/price`, `/api/cars/search/stock`, `/api/cars/search/advanced`, `/api/cars/search/advanced/brand`, `/api/cars/brand/{id}`, `/api/cars/model/{id}` |
| Xe trong kho (từng chiếc) | `/api/car-items` | `/api/car-items/{id}`, `/api/car-items/available?carId=`, `PATCH /api/car-items/{id}/status` |
| Khách hàng | `/api/customers` | `/api/customers/{id}`, `/api/customers/search/name`, `/api/customers/search/phone`, **`GET/PUT /api/customers/me`** (hồ sơ gắn tài khoản) |
| Nhân viên | `/api/employees` | `/api/employees/{id}`, `/api/employees/search/name` |
| Đơn hàng | `/api/orders` | `/api/orders/{id}`, `/api/orders/customer/{customerId}`, `PATCH /api/orders/{id}/status`, `POST /api/orders/{id}/cancel`, **`GET /api/orders/{id}/history`**, **`GET /api/orders/{id}/allowed-transitions`**, **`GET /api/orders/state-machine`** (ADMIN), `/api/orders/{orderId}/items[/{itemId}]` (CRUD dòng xe) |
| Thanh toán | `/api/payments` | `/api/payments/{id}`, `/api/payments/order/{orderId}` |
| Hợp đồng | `/api/contracts` | `/api/contracts/{id}`, `/api/contracts/order/{orderId}`, **`GET /api/contracts/{id}/pdf`** |
| Hóa đơn | `/api/invoices` | `/api/invoices/{id}`, `/api/invoices/order/{orderId}`, **`GET /api/invoices/{id}/pdf`** |
| Thông báo | `/api/notifications` | **`GET /api/notifications`**, **`GET /api/notifications/unread-count`**, **`PATCH /api/notifications/{id}/read`**, **`PATCH /api/notifications/read-all`** |
| Lịch lái thử | `/api/test-drives` | `/api/test-drives/{id}`, `/api/test-drives/customer/{customerId}`, `PATCH /api/test-drives/{id}/status`, `POST /api/test-drives/{id}/cancel` |
| Thông số kỹ thuật (Mongo) | `/api/car-specs` | `/api/car-specs/{carId}` (`PUT` để lưu, `DELETE` để xoá) |
| Đánh giá (Mongo) | `/api/car-reviews` | `/api/car-reviews/{id}`, `/api/car-reviews/car/{carId}`, `/api/car-reviews/car/{carId}/summary`, `POST /api/car-reviews/{reviewId}/replies` |
| Log hoạt động (Mongo) | `/api/activity-logs` | `/api/activity-logs/{id}`, `/api/activity-logs/entity/{type}/{id}` |
| Media (Mongo) | `/api/car-media` | `/api/car-media/{carId}` (`PUT` để lưu, `DELETE` để xoá) |
| Giỏ cấu hình (Redis) | `/api/cart` | theo `sessionId` |
| Xác thực | `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout` | JWT (Bearer token); **`PUT /api/auth/me`** (tự đổi email/mật khẩu) |
| Quản trị người dùng (ADMIN) | `/api/admin/users` | `PUT /api/admin/users/{id}/status` (khoá/mở khoá), `PUT /api/admin/users/{id}/role` (đổi vai trò) |
| Session (Redis) | `/api/sessions` | `POST` đăng nhập, `DELETE` đăng xuất |
| Báo cáo (SQL thuần) | `/api/reports/top-selling-cars`, `/api/reports/inventory-by-brand`, `/api/reports/revenue-summary`, `/api/reports/orders-by-status` | **`GET /api/reports/export?type=...&format=xlsx\|csv`** |
| Dashboard & drilldown | `/api/reports/dashboard-summary`, `/api/reports/revenue-by-month`, `/api/reports/inventory-by-brand/{brandId}/models`, `/api/reports/inventory-by-model/{modelId}/cars`, `/api/reports/top-selling-cars/{carId}/orders`, `/api/reports/orders-by-status/{status}` | |

### Xác thực & phân quyền (RBAC)

Ứng dụng dùng **JWT stateless** (`Authorization: Bearer <token>`), quy tắc phân quyền tập trung tại
`config/SecurityConfig.java` theo HTTP method (so khớp **first-match-wins**).

| Vai trò (`Role`) | Quyền chính |
| --- | --- |
| 🟢 Công khai | `GET` catalog: `/api/cars`, `/api/car-items`, `/api/brands`, `/api/car-models`, `/api/car-specs`, `/api/car-media`, `/api/car-reviews/**`; `/api/auth/**`; Swagger |
| 🔵 Đã đăng nhập (`USER`/`EMPLOYEE`/`ADMIN`) | `/api/auth/me` (+`PUT`), `/api/customers/me` (`GET`/`PUT`), `/api/notifications/**`, `/api/cart/**`, `GET /api/orders/customer/**`, `GET /api/orders/{id}/history`, `GET /api/test-drives/customer/**`, `POST /api/customers`, `POST /api/orders`, `POST /api/test-drives`, `POST /api/car-reviews/**`; tải PDF chứng từ của chính mình |
| 🟠 `EMPLOYEE` hoặc `ADMIN` | CRUD khách hàng/đơn hàng/lịch lái thử/thanh toán/hợp đồng/hóa đơn; `GET/PUT/DELETE` media & specs; `PUT/DELETE` đánh giá; `GET` nhân viên; nhật ký hoạt động |
| 🔴 `ADMIN` | Ghi catalog (xe/hãng/dòng xe/**xe trong kho**), CRUD nhân viên, `/api/reports/**`, `/api/admin/**` |

> Đăng ký luôn tạo tài khoản `USER`. Người dùng tự quản lý hồ sơ khách hàng của mình qua
> **`GET/PUT /api/customers/me`** (liên kết `customers.user_id` với tài khoản đang đăng nhập) —
> không cần biết `customerId` thủ công. Admin quản lý tài khoản qua `/api/admin/users`.


### Chính sách xoá (guard)

`DELETE` sẽ trả **400** kèm thông báo khi tài nguyên còn dữ liệu tham chiếu:

| Xoá | Bị chặn khi |
| --- | --- |
| Brand | còn `CarModel` |
| CarModel | còn `Car` |
| Car (dòng xe) | còn `CarItem` |
| CarItem (xe vật lý) | có `OrderItem` hoặc `TestDriveAppointment` |
| Customer | có `SalesOrder` hoặc `TestDriveAppointment` |
| Employee | có `SalesOrder` hoặc `TestDriveAppointment` |
| SalesOrder | không phải `CANCELLED`, hoặc đã có Payment/Contract/Invoice |
| Payment / Contract / Invoice / TestDrive | (không có tham chiếu) → xoá tự do |

## 6. Thông báo, PDF, xuất báo cáo & state machine

### 6.1. Thông báo in-app + email

- Mỗi sự kiện nghiệp vụ (tạo đơn, đổi trạng thái đơn, thanh toán, hợp đồng, hóa đơn, lịch lái thử)
  phát `BusinessEvent` **bên trong transaction**; `BusinessEventListener` xử lý **AFTER_COMMIT + @Async**
  nên rollback hoặc SMTP chậm không làm hỏng giao dịch chính.
- Thông báo được lưu vào bảng `notifications` cho tài khoản của khách; khách không có tài khoản vẫn nhận email.
- API: `/api/notifications`, `/api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`,
  `PATCH /api/notifications/read-all`.
- Frontend: chuông thông báo trên `Header` (badge chưa đọc, tự làm mới mỗi 60 giây) và tab **Thông báo**
  trong `/account`.

Bật gửi email bằng biến môi trường (không lưu App Password trong source):

```bash
MAIL_USERNAME=your@gmail.com \
MAIL_PASSWORD=your-google-app-password \
MAIL_ENABLED=true \
NOTIFICATION_EMAIL_ENABLED=true \
./mvnw spring-boot:run
```

Mặc định email **tắt** (`MAIL_ENABLED=false`); thông báo in-app vẫn hoạt động bình thường.
Có thể tắt riêng phần email qua `NOTIFICATION_EMAIL_ENABLED=false`.

### 6.2. PDF hợp đồng / hóa đơn

- `GET /api/contracts/{id}/pdf`, `GET /api/invoices/{id}/pdf` trả file PDF có font Noto Sans,
  hiển thị đúng tiếng Việt.
- Khách chỉ tải được chứng từ thuộc đơn của mình; `EMPLOYEE`/`ADMIN` tải được mọi chứng từ.
- Nút **PDF** có trong trang quản trị hợp đồng/hóa đơn và trong popup chi tiết đơn hàng.

### 6.3. Xuất báo cáo Excel / CSV

`GET /api/reports/export?type=<loại>&format=xlsx|csv` (ADMIN). Các loại hỗ trợ:

`dashboard-summary`, `top-selling-cars`, `inventory-by-brand`, `revenue-summary`,
`orders-by-status`, `revenue-by-month`.

File `.xlsx` dùng Apache POI (header tô màu, freeze pane, định dạng tiền); CSV dùng UTF-8 BOM
để Excel mở đúng tiếng Việt.

### 6.4. State machine chuẩn hóa

Luồng trạng thái được khai báo tập trung tại `statemachine/` và **mọi** thay đổi trạng thái
đơn hàng/lịch lái thử/thanh toán đều đi qua `StateMachine.assertCanTransition`.

```
Đơn hàng:      PENDING ──► CONFIRMED ──► COMPLETED
                  │            │
                  └────────────┴──────► CANCELLED
Lịch lái thử:  SCHEDULED ──► CONFIRMED ──► COMPLETED
                    │            │
                    └────────────┴──────► CANCELLED
Thanh toán:    UNPAID ──► PARTIAL ──► PAID ──► REFUNDED
```

- `PENDING → COMPLETED` được cho phép vì luồng thanh toán hiện tại có thể hoàn tất đơn chưa xác nhận.
- Mỗi lần đổi trạng thái đơn được ghi vào `order_status_history` (ai đổi, từ → đến, ghi chú, thời điểm).
- Frontend chỉ hiển thị các trạng thái hợp lệ trong dropdown và hiển thị timeline lịch sử trong popup đơn hàng.

## 7. Nghiệp vụ tồn kho

Sau khi tách bảng, **`cars`** là *dòng xe* (sản phẩm) còn **`car_items`** là *từng chiếc vật lý*
(`condition` `NEW`/`USED`, mỗi chiếc một giá). Mỗi dòng đơn hàng (`order_items`) trỏ tới **một chiếc**
qua `car_item_id`, số lượng luôn = 1.

`SalesOrderServiceImpl.create` (trong `@Transactional`) kiểm tra chiếc còn `AVAILABLE`, đặt chiếc đó `SOLD`;
`cancel` khôi phục chiếc về `AVAILABLE`. `PaymentServiceImpl` cập nhật đơn thành `COMPLETED` khi thanh toán `PAID`.
Cache Redis `carDetail`/`carSearch` được evict khi sửa **cả** `cars` lẫn `car_items`.

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
│   └── admin/                        (dashboard + 17 trang CRUD, chỉ ADMIN)
├── components
│   ├── ui/        (Button, Field, Modal, Toast, DataTable, BarChart, KpiCard…)
│   ├── admin/     (ResourceManager – CRUD tổng quát)
│   ├── car/       (CarGallery, CarPurchasePanel, ReviewForm)
│   └── layout/    (Header, Footer, AdminSidebar)
├── context/AuthContext.tsx
└── lib/api/*, lib/auth/token.ts, lib/utils/*
```

| Khu vực | Trang |
| --- | --- |
| Công khai | `/`, `/cars`, `/cars/[id]` |
| Xác thực | `/login`, `/register` |
| Người dùng | `/cart`, `/checkout`, `/account` |
| Quản trị (ADMIN) | `/admin` (dashboard + drilldown), `/admin/cars` (dòng xe), `/admin/car-items` (xe trong kho), `/admin/brands`, `/admin/car-models`, `/admin/customers`, `/admin/employees`, `/admin/orders`, `/admin/test-drives`, `/admin/payments`, `/admin/contracts`, `/admin/invoices`, `/admin/reviews`, `/admin/media`, `/admin/specs`, `/admin/users`, `/admin/activity-logs` |

**Ghi chú UI:**
- `FilterPanel` (`/cars`) là **thanh ngang** đặt trên kết quả (`grid` theo breakpoint, 6 nhóm ở màn lớn),
  giữ đồng bộ tham số URL — danh sách hiển thị **trọn 1 view** (thẻ gọn `compact`).
- Trang chi tiết `/cars/[id]` dùng `CarGallery` (client) để **bấm thumbnail đổi ảnh**; `CarPurchasePanel`
  cho chọn **chiếc xe cụ thể** (VIN/màu/tình trạng/giá) trước khi thêm vào giỏ hoặc đăng ký lái thử.
- `Header` có trạng thái **active** theo `usePathname()`: mục đang xem được tô vàng + gạch chân (desktop),
  nền xám + chữ vàng (mobile), kèm `aria-current="page"`. Chuông thông báo hiển thị badge chưa đọc cho
  tài khoản đã đăng nhập.
- `/account` có thêm tab **Thông báo** (đọc từng thông báo hoặc đánh dấu tất cả đã đọc).
- Trang quản trị hợp đồng/hóa đơn có nút tải **PDF**; popup chi tiết đơn hàng có timeline lịch sử trạng thái.
- Dashboard có bộ chọn loại báo cáo + nút **CSV / Excel** để tải file trực tiếp.


Dashboard có **drilldown**: nhấp cột trạng thái → danh sách đơn; nhấp hãng → dòng xe → danh sách xe;
nhấp xe bán chạy → các dòng đơn chứa xe đó.

## 9. Tài liệu liên quan

- `plan.md` — kế hoạch 7 phase.
- `task2.md`, `task3.md` — đề bài & đặc tả các buổi.
- `task4.md` — kế hoạch bổ sung: RBAC đầy đủ + API dashboard/drilldown + hoàn thiện frontend.
- `task5.md` — kế hoạch buổi 5: tách `cars` + `car_items`, quản trị người dùng, sửa UI (thanh lọc ngang, 1 view, media).
- `report.md` — báo cáo tổng hợp.
