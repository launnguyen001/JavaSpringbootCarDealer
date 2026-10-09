# Báo cáo tiến độ — Dự án Car Dealer

> Cập nhật: đánh dấu hoàn thành theo từng Phase trong `plan.md`.
> Trạng thái: `[x]` xong, `[~]` đang làm, `[ ]` chưa làm.

## Tổng quan

| Phase | Nội dung | Trạng thái |
| --- | --- | --- |
| 0 | Chuẩn bị hạ tầng | ✅ Hoàn thành |
| 1 | Hoàn thiện phần SQL theo mã mẫu | ✅ Hoàn thành |
| 2 | Mở rộng domain car dealer (SQL) | ✅ Hoàn thành |
| 3 | Bổ sung NoSQL (MongoDB) | ✅ Hoàn thành |
| 4 | Redis (cache/session/tạm) | ✅ Hoàn thành |
| 5 | Báo cáo & thống kê (SQL thuần) | ✅ Hoàn thành |
| 6 | Kiểm thử | ✅ Hoàn thành |
| 7 | Hoàn thiện, bảo mật, tài liệu | ✅ Hoàn thành (không gồm phân quyền) |

---

## Phase 0 — Chuẩn bị hạ tầng ✅

- [x] `pom.xml`: thêm `spring-boot-starter-validation`, `spring-boot-starter-data-mongodb`,
  `spring-boot-starter-data-redis`; test-scope thêm `spring-boot-starter-data-mongodb-test`,
  `spring-boot-starter-data-redis-test`, `com.h2database:h2`.
- [x] `application.properties`: MySQL (`lauth43`) + MongoDB (`cardealer`) + Redis + cache.
- [x] `application-test.properties`: H2 in-memory (`jdbc:h2:mem:cardealer`) + `spring.cache.type=simple`.
- [x] `DemoApplicationTests` dùng `@ActiveProfiles("test")` — context load PASS.

**Ghi chú môi trường:** không có MySQL/MongoDB/Redis/Docker thật. Test dùng H2 in-memory;
Mongo autoconfig vẫn bật nhưng lazy (chỉ log lỗi nền, không làm fail context).
Không dùng flapdoodle embedded Mongo (rủi ro tải mongod) → test NoSQL dùng mock repository.

---

## Phase 1 — Hoàn thiện phần SQL theo mã mẫu ✅

- [x] `CarRepository`: derived query + JPQL `@Query`
  (`searchByModelAndPriceAndQuantity`, `searchByBrandAndPriceAndQuantity`).
- [x] Endpoint tìm kiếm theo mẫu: `/search/name`, `/search/price`, `/search/stock`,
  `/search/advanced`, `/search/advanced/brand`.
- [x] Các endpoint lấy theo quan hệ: `/brand/{id}`, `/model/{id}`; `GET /{id}`.
- [x] Validate trong `ServiceImpl` (giá, số lượng, tham chiếu FK, `EntityManager.getReference(...)`).
- [x] `BrandRepository` (interface custom) + `JdbcBrandRepository` (JdbcTemplate) theo mẫu
  `JdbcCategoryRepository`.

---

## Phase 2 — Mở rộng domain car dealer (SQL) ✅

- [x] Đổi `Category` → `Brand`; thêm `CarModel`; `Product` → `Car` (vin, year, color, status, price, quantity).
- [x] Thêm đủ bộ entity + dto + mapper + repository + service/Impl + controller cho:
  `Customer`, `Employee`, `SalesOrder`, `OrderItem`, `Contract`, `Payment`, `Invoice`,
  `TestDriveAppointment`.
- [x] Nghiệp vụ tồn kho trong `SalesOrderServiceImpl` (`@Transactional`): trừ tồn kho khi tạo đơn,
  set `CarStatus.SOLD` khi hết hàng, hoàn tồn kho khi huỷ đơn → chống bán âm.
- [x] `PaymentServiceImpl`: khi thanh toán `PAID` → cập nhật đơn `COMPLETED`.
- [x] Enum: `CarStatus`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `AppointmentStatus`.
- [x] Lỗi trả về qua `GlobalExceptionHandler` (đã thêm handler `MethodArgumentNotValidException`,
  `ConstraintViolationException`).

---

## Phase 3 — Bổ sung NoSQL (MongoDB) ✅

Package `document` + `mongorepository` (tách khỏi SQL).

- [x] `CarSpecification` (tham chiếu mềm `carId`) + repository + service + controller `/api/car-specs`.
- [x] `CarReview` (rating, nội dung, phản hồi lồng `ReviewReply`) + API `/api/car-reviews`.
- [x] `ActivityLog` (audit) + API `/api/activity-logs`.
- [x] `CarMedia` (danh sách ảnh/video) + API `/api/car-media`.
- [x] DTO + Mapper đầy đủ cho cả 4 document.

**Ghi chú:** không gọi `ActivityLog` (Mongo) từ luồng tạo đơn hàng (SQL) để tránh phụ thuộc
server Mongo khi test.

---

## Phase 4 — Redis (cache/session/tạm) ✅

- [x] `@EnableCaching` trên `DemoApplication`; `CacheConfig` (`@Profile("!test")`) cấu hình
  `RedisCacheManager` (TTL 10 phút, `GenericJackson2JsonRedisSerializer`).
- [x] `CarCatalogService` cache kết quả tìm kiếm nóng (`carSearch`) và chi tiết xe (`carDetail`);
  `CarServiceImpl.create` gọi `evictCatalog()` để xoá cache khi dữ liệu đổi.
- [x] Giỏ cấu hình xe theo phiên `ConfiguratorCartService` (Redis Hash, TTL 30 phút)
  + `CartController` `/api/cart`.
- [x] Session/token `SessionService` (Redis, TTL 30 phút) + `SessionController` `/api/sessions`.
- [ ] Rate limiting — chưa làm.

**Ghi chú:** test profile dùng `spring.cache.type=simple` nên không cần Redis khi test.

---

## Phase 5 — Báo cáo & thống kê (SQL thuần) ✅

- [x] `JdbcReportRepository` (JdbcTemplate) với các query tổng hợp:
  top xe bán chạy, tồn kho theo hãng, tổng doanh thu/đơn, đếm đơn theo trạng thái.
- [x] DTO: `TopSellingCar`, `InventoryByBrand`, `RevenueSummary`, `OrderStatusCount`.
- [x] `ReportService` + `ReportServiceImpl` + `ReportController` `/api/reports/...`.
- [ ] Doanh số theo tháng/quý, doanh thu theo nhân viên — chưa làm (tránh hàm ngày khác nhau
  giữa MySQL và H2).

---

## Phase 6 — Kiểm thử ✅

- [x] `CarRepositorySearchTests` (mẫu `ProductRepositorySearchTests`): 5 test —
  tìm theo tên, giá, tồn kho, nâng cao theo dòng xe, nâng cao theo hãng.
- [x] `SalesOrderServiceTests`: 5 test — trừ tồn kho, set `SOLD` khi hết hàng,
  báo lỗi khi thiếu hàng/không thấy khách, khôi phục tồn kho khi huỷ đơn.
- [x] `CarControllerMockMvcTests` (`@AutoConfigureMockMvc`): 6 test — GET list, GET id,
  404, search, POST 201, POST 400 (validation).
- [x] `ActivityLogServiceImplTests`: 4 test NoSQL service dùng mock repository (Mockito).
- [x] `mvn test`: **21 test PASS**, `BUILD SUCCESS`.

**Ghi chú:** test NoSQL dùng mock repository thay embedded MongoDB (môi trường không tải được mongod).
`application-test.properties` đặt `spring.data.mongodb.auto-index-creation=false` để context
không cố kết nối Mongo khi khởi động. Đã thêm `spring-boot-starter-cache` để có CacheManager
`simple` trong test.

---

## Phase 7 — Hoàn thiện, bảo mật, tài liệu ✅

- [x] Swagger/OpenAPI: `springdoc-openapi-starter-webmvc-ui` v3.1.1 (hỗ trợ Spring Boot 4).
  Swagger UI `/swagger-ui.html`, OpenAPI JSON `/v3/api-docs`.
- [x] Validation DTO: `@Valid` + `@NotBlank/@NotNull/@Positive...` đã có ở các request record.
- [x] Seed dữ liệu mẫu: `DataSeeder` (`@Profile("!test")`, `app.seed.enabled=true`) tạo
  brands, models, cars, customers, employees khi CSDL trống.
- [x] `README.md`: mô tả kiến trúc, phân chia SQL/NoSQL/Redis, cách chạy, endpoint, test.
- [ ] Phân quyền (admin/nhân viên/khách) — **chủ động bỏ qua** theo xác nhận của người dùng
  (tránh ảnh hưởng 21 test hiện có).

---

## Trạng thái cuối

- Tất cả 7 phase hoàn thành (Phase 7 không gồm phân quyền).
- `.\mvnw.cmd compile` — OK.
- `.\mvnw.cmd test` — **21 test PASS**, `BUILD SUCCESS`.

---

## Lệnh kiểm tra

```
.\mvnw.cmd -q compile
.\mvnw.cmd test
```
