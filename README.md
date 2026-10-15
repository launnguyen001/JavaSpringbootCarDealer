# Car Dealer — Backend REST API

Hệ thống backend cho **đại lý bán xe**, xây dựng trên Spring Boot 4, kết hợp
**MySQL (SQL)** cho nghiệp vụ giao dịch, **MongoDB (NoSQL)** cho dữ liệu linh hoạt,
**Redis** cho cache/session, và **JdbcTemplate** cho báo cáo thống kê.

## 1. Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ / Build | Java 21, Maven (wrapper 3.9.16) |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| CSDL giao dịch | MySQL (`lauth43`) |
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

Cần sẵn: **MySQL 3306**, **MongoDB 27017**, **Redis 6379**.

```powershell
# cấu hình kết nối trong src/main/resources/application.properties
.\mvnw.cmd spring-boot:run
```

- Ứng dụng: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Dữ liệu mẫu (brands/models/cars/customers/employees) tự động được seed khi khởi động
(`app.seed.enabled=true`, bỏ qua ở profile `test`).

## 4. Chạy test

Test chạy trên **H2 in-memory**, profile `test` (không cần MySQL/MongoDB/Redis thật):

```powershell
.\mvnw.cmd test
```

Kết quả hiện tại: **47 test PASS**.

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
| Session (Redis) | `/api/sessions` | `POST` đăng nhập, `DELETE` đăng xuất |
| Báo cáo (SQL thuần) | `/api/reports/top-selling-cars`, `/api/reports/inventory-by-brand`, `/api/reports/revenue-summary`, `/api/reports/orders-by-status` | |

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

## 6. Nghiệp vụ tồn kho

`SalesOrderServiceImpl.create` (trong `@Transactional`) kiểm tra và trừ tồn kho,
đặt xe `SOLD` khi hết hàng; `cancel` khôi phục tồn kho và trạng thái xe.
`PaymentServiceImpl` cập nhật đơn thành `COMPLETED` khi thanh toán `PAID`.

## 7. Tài liệu liên quan

- `plan.md` — kế hoạch 7 phase.
- `report.md` — báo cáo tiến độ theo từng phase.
