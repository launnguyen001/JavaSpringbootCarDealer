# Kế hoạch phát triển dự án Car Dealer (Đại lý bán xe)

## 1. Mục tiêu

Xây dựng hệ thống backend REST API cho một **đại lý bán xe (car dealer)** trên nền
Spring Boot, phát triển tiếp từ mã nguồn hiện tại (`src/`) và **tuân thủ kiến trúc,
quy ước và phong cách của mã mẫu** trong thư mục `code mẫu bên main/` và
`code mẫu bên test/`.

Sản phẩm cuối cùng gồm:
- Quản lý danh mục (hãng xe / dòng xe) và xe trong kho.
- Tìm kiếm / lọc xe nâng cao.
- Quản lý khách hàng, nhân viên, đơn hàng, hợp đồng, thanh toán, hóa đơn, lịch lái thử.
- Thông số kỹ thuật, đánh giá, log, tìm kiếm gợi ý (dữ liệu linh hoạt).
- Báo cáo/thống kê doanh số.
- Kiểm thử tự động đầy đủ.

---

## 2. Phân tích hiện trạng

### 2.1 Mã nguồn hiện tại (`src/main/java/com/example/lab5/`)

| Layer | File đã có | Trạng thái |
| --- | --- | --- |
| entity | `Category`, `Product` | Có (dạng sản phẩm chung, chưa phải xe) |
| dto | `CategoryRequest/Response`, `ProductRequest/Response` | Có |
| mapper | `CategoryMapper`, `ProductMapper` | Có |
| repository | `CategoryRepository` (interface), `JdbcCategoryRepository`, `ProductRepository` | Có nhưng `ProductRepository` rỗng |
| service | `CategoryService`, `ProductService` + `Impl` | Mới có CRUD cơ bản |
| controller | `CategoryController`, `ProductController` | Chỉ có `POST` + `GET all` |
| exception | `BadRequestException`, `ResourceNotFoundException`, `ErrorResponse`, `GlobalExceptionHandler` | Đã đầy đủ |
| config | `application.properties` (MySQL `lauth43`), `pom.xml` (JPA + MySQL + Lombok) | Có |

**Còn thiếu so với mã mẫu:** tìm kiếm theo tên/giá/tồn kho/tìm kiếm nâng cao,
`getProductById` được expose qua controller, các derived query trong
`ProductRepository`, và test repository (`ProductRepositorySearchTests`).

### 2.2 Mã mẫu (`code mẫu bên main/`, `code mẫu bên test/`)

Mã mẫu là phiên bản hoàn chỉnh của cùng kiến trúc, bổ sung:

- `ProductRepository` với các **derived query** + **JPQL `@Query`** cho tìm kiếm nâng cao (`ProductRepository.java`).
- `ProductServiceImpl` với `@Transactional`, `EntityManager.getReference(...)` và các hàm validate.
- `ProductController` với các endpoint `/search/name`, `/search/price`, `/search/stock`, `/search/advanced`.
- `JdbcCategoryRepository` minh hoạ **truy vấn SQL thuần** qua `JdbcTemplate` (một repository tuỳ biến song song với JPA).
- `ProductRepositorySearchTests` minh hoạ cách viết test cho search.

> **Kết luận:** cần áp dụng đúng các pattern trên cho domain **car dealer** thay vì
> sản phẩm chung, đồng thời mở rộng thêm phần nghiệp vụ và phần NoSQL.

### 2.3 Khoảng trống (gap) cần xử lý

1. Chuyển/mở rộng domain `Product/Category` → `Car/Brand/CarModel` (xe, hãng, dòng xe).
2. Bổ sung toàn bộ endpoint tìm kiếm + chi tiết theo mã mẫu.
3. Thêm các thực thể nghiệp vụ: khách hàng, nhân viên, đơn hàng, hợp đồng, thanh toán, hóa đơn, lịch lái thử.
4. Bổ sung **NoSQL** (MongoDB/Redis) — hiện `pom.xml` chưa có dependency nào.
5. Thêm báo cáo thống kê bằng SQL thuần (theo mẫu `JdbcCategoryRepository`).
6. Viết test đầy đủ.

---

## 3. Kiến trúc & phân chia SQL / NoSQL

### 3.1 Nguyên tắc chọn nơi lưu trữ

- **SQL (MySQL) — nguồn dữ liệu giao dịch chính (source of truth):** dữ liệu có
  quan hệ chặt, cần ràng buộc khoá ngoại, giao dịch ACID, tính nhất quán cao
  (tiền, tồn kho, hợp đồng).
- **NoSQL (MongoDB) — dữ liệu linh hoạt / khối lượng lớn / schema thay đổi:**
  thông số kỹ thuật đa dạng theo từng dòng xe, đánh giá, log, tìm kiếm, media.
- **Redis — cache & session & dữ liệu tạm:** giỏ cấu hình xe (cấu hình xe theo yêu cầu),
  cache tìm kiếm nóng, token đăng nhập, rate limiting.

### 3.2 Bảng ánh xạ chi tiết SQL vs NoSQL

| # | Miền nghiệp vụ | Công nghệ | Lý do |
| --- | --- | --- | --- |
| 1 | Hãng xe `Brand`, dòng xe `CarModel` | **SQL** | Quan hệ phân cấp, ít thay đổi, cần FK |
| 2 | Xe / tồn kho `Car` (giá, số lượng, trạng thái, VIN) | **SQL** | Giao dịch, ràng buộc, bán/giữ chỗ cần ACID |
| 3 | Khách hàng `Customer`, nhân viên `Employee` | **SQL** | Quan hệ với đơn hàng, cần tính nhất quán |
| 4 | Đơn hàng `SalesOrder`, hợp đồng `Contract` | **SQL** | Giao dịch tài chính, ACID |
| 5 | Thanh toán `Payment`, hóa đơn `Invoice` | **SQL** | Bút toán tài chính, không được sai lệch |
| 6 | Lịch lái thử `TestDriveAppointment` | **SQL** | Quan hệ khách–xe–nhân viên, cần ràng buộc |
| 7 | Thông số kỹ thuật `CarSpecification` | **NoSQL (MongoDB)** | Schema thay đổi theo dòng xe (động cơ, hộp số, kích thước...) |
| 8 | Đánh giá / bình luận `CarReview` | **NoSQL (MongoDB)** | Bán cấu trúc, ghi nhiều, lồng phản hồi |
| 9 | Nhật ký hoạt động `ActivityLog` / audit | **NoSQL (MongoDB)** | Ghi nhiều, schema tự do, không cần join |
| 10 | Tài liệu/hình ảnh xe `CarMedia` (metadata) | **NoSQL (MongoDB)** | Danh sách ảnh/video linh hoạt theo xe |
| 11 | Tìm kiếm & gợi ý `SearchSuggestion`, index tìm kiếm | **NoSQL (MongoDB)** | Text index, dữ liệu phi cấu trúc |
| 12 | Giỏ cấu hình xe / phiên đăng nhập / cache | **Redis** | Truy cập nhanh, TTL, dữ liệu tạm |
| 13 | Báo cáo doanh số, top xe bán chạy, tồn kho | **SQL thuần (JdbcTemplate)** | Query tổng hợp/join phức tạp theo mẫu `JdbcCategoryRepository` |

> Điểm chốt: **nghiệp vụ lõi & tài chính = SQL; dữ liệu linh hoạt/log/tìm kiếm = NoSQL;
> dữ liệu tạm & cache = Redis.**

---

## 4. Lộ trình phát triển (Phases)

### Phase 0 — Chuẩn bị hạ tầng
- [ ] Thêm dependency vào `pom.xml`: `spring-boot-starter-data-mongodb`, `spring-boot-starter-data-redis`, (tuỳ chọn) `spring-boot-starter-validation`, `spring-boot-starter-security`.
- [ ] Cấu hình `application.properties`: kết nối MongoDB, Redis; tách profile `dev`/`test` (mẫu test dùng `@ActiveProfiles("test")`).
- [ ] Tạo `application-test.properties` dùng H2 + embedded Mongo (hoặc Testcontainers).

### Phase 1 — Hoàn thiện phần SQL theo mã mẫu (ưu tiên cao)
- [ ] Áp dụng đúng các derived query + JPQL `@Query` như `ProductRepository` mẫu cho `CarRepository` (theo tên, giá, tồn kho, tìm kiếm nâng cao theo hãng/dòng xe).
- [ ] Bổ sung endpoint tìm kiếm trong controller theo mẫu: `/search/name`, `/search/price`, `/search/stock`, `/search/advanced`.
- [ ] Bổ sung `getById` và expose qua controller.
- [ ] Hoàn thiện validate trong `ServiceImpl` theo mẫu (giá, số lượng, tham chiếu FK, `EntityManager.getReference`).
- [ ] Giữ `JdbcCategoryRepository` làm mẫu cho truy vấn SQL thuần; bổ sung `JdbcReportRepository` cho báo cáo.

### Phase 2 — Mở rộng domain car dealer (SQL)
- [ ] Đổi/mở rộng `Category` → `Brand` (hãng), thêm `CarModel` (dòng xe); `Product` → `Car` với `vin`, `year`, `color`, `status`, `price`, `quantity`.
- [ ] Thêm entity + repository + service + controller + dto + mapper cho: `Customer`, `Employee`, `SalesOrder`, `OrderItem`, `Contract`, `Payment`, `Invoice`, `TestDriveAppointment`.
- [ ] Nghiệp vụ tồn kho: giữ chỗ (reserve) – bán (sold) trong `@Transactional`, chống bán âm.
- [ ] Ràng buộc và exception theo `GlobalExceptionHandler` hiện có.

### Phase 3 — Bổ sung NoSQL (MongoDB)
- [ ] Document `CarSpecification` (kèm `carId` tham chiếu mềm tới SQL) + repository + service + controller.
- [ ] Document `CarReview` (rating, nội dung, phản hồi lồng nhau) + API.
- [ ] Document `ActivityLog` (audit qua AOP/interceptor).
- [ ] Document `CarMedia` (danh sách ảnh/video) + API.
- [ ] Tạo text index cho tìm kiếm & `SearchSuggestion`.

### Phase 4 — Redis (cache/session/tạm)
- [ ] Cache kết quả tìm kiếm nóng, cache chi tiết xe (`@Cacheable`).
- [ ] Lưu giỏ cấu hình xe theo phiên (`Cấu hình xe theo yêu cầu`).
- [ ] Lưu session/token đăng nhập, rate limiting.

### Phase 5 — Báo cáo & thống kê (SQL thuần)
- [ ] `JdbcReportRepository` với query tổng hợp: doanh số theo tháng/quý, top xe bán chạy, tồn kho theo hãng, doanh thu theo nhân viên.
- [ ] Endpoint `/api/reports/...` trả về dữ liệu tổng hợp.

### Phase 6 — Kiểm thử
- [ ] Viết test repository theo mẫu `ProductRepositorySearchTests` (dùng `@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`, `EntityManager`).
- [ ] Test service (validate, nghiệp vụ tồn kho, exception).
- [ ] Test controller (MockMvc) cho các endpoint chính.
- [ ] Test NoSQL repository với embedded MongoDB.

### Phase 7 — Hoàn thiện, bảo mật, tài liệu
- [ ] Validation DTO (`@Valid`, `@NotNull`, `@Min`...).
- [ ] Phân quyền (nếu cần): admin / nhân viên bán hàng / khách.
- [ ] Tài liệu API (Swagger/OpenAPI).
- [ ] Seed dữ liệu mẫu, README hướng dẫn chạy.

---

## 5. Chi tiết công việc theo layer (bám sát mã mẫu)

Với mỗi thực thể mới, tạo đủ bộ file theo đúng thứ tự và quy ước mã mẫu:

| Layer | Quy ước theo mẫu | Ghi chú |
| --- | --- | --- |
| `entity` | JPA `@Entity`, `@Table`, Lombok `@Data/@NoArgsConstructor/@AllArgsConstructor`, `@ToString.Exclude`/`@EqualsAndHashCode.Exclude` cho quan hệ | Quan hệ 2 chiều `@OneToMany`/`@ManyToOne` |
| `dto` | Java `record` Request/Response | Giống `ProductRequest/Response` |
| `mapper` | `@Component`, hàm `toEntity`/`toResponse` | Giống `ProductMapper` |
| `repository` | JPA interface (derived query + `@Query`) hoặc `JdbcTemplate` cho SQL thuần | Giống `ProductRepository` / `JdbcCategoryRepository` |
| `service` + `Impl` | Interface + `@Service`, `@Transactional` cho ghi, validate trong private method | Giống `ProductServiceImpl` |
| `controller` | `@RestController`, `@RequestMapping("/api/...")` | Giống `ProductController` |
| `exception` | Dùng lại `BadRequestException`, `ResourceNotFoundException`, `GlobalExceptionHandler` | Không tạo trùng |
| `NoSQL document` | `@Document`, `@Id`; repository `MongoRepository` | Tách package `document`/`mongorepository` |

**Cấu trúc package đề xuất (mở rộng từ hiện tại):**

```
com.example.lab5
├── controller
├── dto
├── entity            (SQL/JPA)
├── document          (NoSQL/MongoDB)
├── mapper
├── repository        (SQL repositories)
├── mongorepository   (NoSQL repositories)
├── service
│   └── Impl
└── exception
```

---

## 6. Ma trận tổng hợp SQL / NoSQL theo chức năng

| Chức năng | SQL (MySQL/JPA) | SQL thuần (JdbcTemplate) | NoSQL (MongoDB) | Redis |
| --- | :---: | :---: | :---: | :---: |
| Quản lý hãng/dòng xe | ✔ | | | |
| Quản lý & tìm kiếm xe | ✔ | | | ✔ (cache) |
| Thông số kỹ thuật xe | | | ✔ | |
| Khách hàng / nhân viên | ✔ | | | |
| Đơn hàng / hợp đồng / thanh toán / hóa đơn | ✔ | | | |
| Lịch lái thử | ✔ | | | |
| Đánh giá / bình luận | | | ✔ | |
| Log / audit | | | ✔ | |
| Media (ảnh/video) | | | ✔ | |
| Gợi ý / tìm kiếm text | | | ✔ | ✔ (cache nóng) |
| Cấu hình xe / giỏ hàng / session | | | | ✔ |
| Báo cáo doanh số / tồn kho | | ✔ | | |

---

## 7. Thứ tự ưu tiên thực hiện

1. **Phase 1** (hoàn thiện SQL theo mã mẫu) — bắt buộc, làm trước vì có sẵn nền tảng.
2. **Phase 2** (mở rộng domain SQL) — nghiệp vụ lõi.
3. **Phase 5** (báo cáo SQL thuần) — theo mẫu `JdbcCategoryRepository`.
4. **Phase 3** (MongoDB) — dữ liệu linh hoạt.
5. **Phase 4** (Redis) — tối ưu.
6. **Phase 6** (test) — chạy song song mỗi phase.
7. **Phase 7** (hoàn thiện) — cuối cùng.

---

## 8. Tiêu chí hoàn thành (Definition of Done)

- [ ] Tất cả endpoint CRUD + tìm kiếm hoạt động, đúng response format `record` như mã mẫu.
- [ ] Validate đầu vào đầy đủ, lỗi trả về qua `GlobalExceptionHandler` (404/400/409/500).
- [ ] Dữ liệu giao dịch & tài chính nằm trong MySQL; dữ liệu linh hoạt trong MongoDB; cache/session trong Redis — đúng ma trận mục 6.
- [ ] `mvn test` xanh toàn bộ (repository, service, controller, NoSQL).
- [ ] `mvn clean package` build thành công, không lỗi lint/compile.

---

## 9. Rủi ro & lưu ý

- **Nhất quán dữ liệu giữa SQL và NoSQL:** MongoDB chỉ tham chiếu mềm `carId`; cần cơ chế đồng bộ khi xoá/sửa xe (event hoặc kiểm tra tồn tại khi đọc).
- **Mã hoá file:** các file hiện tại có comment tiếng Việt bị lỗi font (do lưu sai encoding). Khi tạo file mới nên lưu **UTF-8** để tránh lỗi hiển thị.
- **Không phá vỡ mã mẫu:** giữ nguyên cấu trúc package, tên hàm, kiểu trả về để đảm bảo tính nhất quán và dễ chấm điểm.
- **Profile test:** cần `application-test.properties` + H2/embedded Mongo trước khi viết test theo mẫu.
