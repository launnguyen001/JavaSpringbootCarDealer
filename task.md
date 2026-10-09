# Task — Buổi 2: Hoàn thiện CRUD API còn thiếu

> Mục tiêu: bổ sung **Update (PUT/PATCH)** và **Delete (DELETE)** cho các bảng quan trọng,
> cùng `GET /{id}` cho Brand, để mỗi tài nguyên có đủ CRUD. Bám pattern hiện có
> (`entity → dto → mapper → repository → service/Impl → controller`) và dùng lại DTO Request.
>
> Sau khi xong: chạy `.\mvnw.cmd test` phải xanh (giữ nguyên 21 test hiện có + test mới).

---

## 0. Quyết định thiết kế (chốt sẵn — có thể đổi)

1. **Chính sách xoá:** chặn xoá nếu còn dữ liệu tham chiếu → ném `BadRequestException`
   (trả 400/409). **Không** xoá dây chuyền, trừ entity con của đơn (`OrderItem` cascade theo đơn).
2. **Update:** `PUT /api/<resource>/{id}`, nạp entity đang có → cập nhật field từ Request → lưu
   (không tạo entity mới, không đổi `id`).
3. **OrderItem:** thêm API quản lý dòng xe trong đơn (`/api/orders/{orderId}/items`) — mức ưu tiên thấp.
4. **SalesOrder / TestDrive:** giữ `cancel` mềm; thêm `PUT` cho field được phép sửa. Delete cứng
   chỉ cho TestDrive (không có tham chiếu) và SalesOrder (chỉ khi `CANCELLED` + không có payment/contract/invoice).
5. Trùng dữ liệu (email, VIN, nationalId, order_code, contract_number, invoice_number) đã được
   `GlobalExceptionHandler` xử lý qua `DataIntegrityViolationException → 409`.

---

## 1. Hiện trạng CRUD

| Tài nguyên (SQL) | POST | GET list | GET /{id} | PUT | DELETE |
| --- | :--: | :--: | :--: | :--: | :--: |
| Brand | ✅ | ✅ | ❌ | ❌ | ❌ |
| CarModel | ✅ | ✅ | ✅ | ❌ | ❌ |
| Car | ✅ | ✅ | ✅ | ❌ | ❌ |
| Customer | ✅ | ✅ | ✅ | ❌ | ❌ |
| Employee | ✅ | ✅ | ✅ | ❌ | ❌ |
| SalesOrder | ✅ | ✅ | ✅ | ⚠️ status | ⚠️ cancel |
| Payment | ✅ | ✅ | ✅ | ❌ | ❌ |
| Contract | ✅ | ✅ | ✅ | ❌ | ❌ |
| Invoice | ✅ | ✅ | ✅ | ❌ | ❌ |
| TestDriveAppointment | ✅ | ✅ | ✅ | ⚠️ status | ⚠️ cancel |
| OrderItem | ❌ | ❌ | ❌ | ❌ | ❌ |

| Tài nguyên (Mongo) | POST | GET list | GET /{key} | PUT | DELETE |
| --- | :--: | :--: | :--: | :--: | :--: |
| CarSpecification | – | ❌ | ✅ | ✅ | ✅ |
| CarMedia | – | ❌ | ✅ | ✅ | ✅ |
| CarReview | ✅ | ❌ | ✅ | ❌ | ❌ |
| ActivityLog | ✅ | ✅ | ✅ | – | – |

---

## 2. Ma trận endpoint MỤC TIÊU

| Tài nguyên | Tạo | Danh sách | Chi tiết | Cập nhật | Xoá |
| --- | --- | --- | --- | --- | --- |
| Brand | `POST /api/brands` | `GET /api/brands` | `GET /api/brands/{id}` | `PUT /api/brands/{id}` | `DELETE /api/brands/{id}` |
| CarModel | `POST /api/car-models` | `GET /api/car-models` | `GET /api/car-models/{id}` | `PUT /api/car-models/{id}` | `DELETE /api/car-models/{id}` |
| Car | `POST /api/cars` | `GET /api/cars` | `GET /api/cars/{id}` | `PUT /api/cars/{id}` | `DELETE /api/cars/{id}` |
| Customer | `POST /api/customers` | `GET /api/customers` | `GET /api/customers/{id}` | `PUT /api/customers/{id}` | `DELETE /api/customers/{id}` |
| Employee | `POST /api/employees` | `GET /api/employees` | `GET /api/employees/{id}` | `PUT /api/employees/{id}` | `DELETE /api/employees/{id}` |
| Payment | `POST /api/payments` | `GET /api/payments` | `GET /api/payments/{id}` | `PUT /api/payments/{id}` | `DELETE /api/payments/{id}` |
| Contract | `POST /api/contracts` | `GET /api/contracts` | `GET /api/contracts/{id}` | `PUT /api/contracts/{id}` | `DELETE /api/contracts/{id}` |
| Invoice | `POST /api/invoices` | `GET /api/invoices` | `GET /api/invoices/{id}` | `PUT /api/invoices/{id}` | `DELETE /api/invoices/{id}` |
| SalesOrder | `POST /api/orders` | `GET /api/orders` | `GET /api/orders/{id}` | `PUT /api/orders/{id}` | `DELETE /api/orders/{id}` (guard) |
| TestDrive | `POST /api/test-drives` | `GET /api/test-drives` | `GET /api/test-drives/{id}` | `PUT /api/test-drives/{id}` | `DELETE /api/test-drives/{id}` |
| OrderItem | `POST /api/orders/{orderId}/items` | `GET /api/orders/{orderId}/items` | – | `PUT /api/orders/{orderId}/items/{itemId}` | `DELETE /api/orders/{orderId}/items/{itemId}` |
| CarReview (Mongo) | `POST /api/car-reviews` | `GET /api/car-reviews` | `GET /api/car-reviews/car/{carId}` | `PUT /api/car-reviews/{id}` | `DELETE /api/car-reviews/{id}` |
| CarSpecification (Mongo) | – | `GET /api/car-specs` | `GET /api/car-specs/{carId}` | `PUT /api/car-specs` | `DELETE /api/car-specs/{carId}` |
| CarMedia (Mongo) | – | `GET /api/car-media` | `GET /api/car-media/{carId}` | `PUT /api/car-media` | `DELETE /api/car-media/{carId}` |

---

## 3. Checklist công việc theo file

### Nhóm A — SQL: thêm Update + Delete

#### A1. Brand (cần bổ sung cả Read-by-id + repository)
- [ ] `repository/BrandRepository.java`: thêm `Optional<Brand> findById(Long)`, `void deleteById(Long)`,
      `boolean existsByName(String)`.
- [ ] `repository/JdbcBrandRepository.java`: implement 3 method trên bằng `JdbcTemplate`
      (`SELECT ... WHERE id=?`, `DELETE FROM brands WHERE id=?`, `SELECT COUNT(*)...`).
- [ ] `service/BrandService.java`: thêm `Brand getById(Long)`, `Brand update(Long, Brand)`, `void delete(Long)`.
- [ ] `service/Impl/BrandServiceImpl.java`: `getById` (404 nếu không có); `update` (nạp → đổi name/country/description → save);
      `delete` (**chặn nếu còn CarModel**).
- [ ] `controller/BrandController.java`: thêm `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (204).

#### A2. CarModel
- [ ] `repository/CarModelRepository.java`: thêm `boolean existsByBrand_Id(Long)`.
- [ ] `service/CarModelService.java` + `Impl`: `update(Long, CarModel)` (đổi name/bodyType/description/brand), `delete(Long)`
      (chặn nếu còn Car thuộc model).
- [ ] `controller/CarModelController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A3. Car
- [ ] `repository/OrderItemRepository.java`: thêm `boolean existsByCar_Id(Long)`;
      `TestDriveAppointmentRepository.java`: thêm `boolean existsByCar_Id(Long)`.
- [ ] `service/CarService.java` + `Impl`: `update(Long, Car)` (đổi name/description/price/quantity/vin/year/color/status/model;
      dùng `entityManager.getReference` cho model), `delete(Long)` (chặn nếu có OrderItem/TestDrive).
- [ ] `controller/CarController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A4. Customer
- [ ] `repository/SalesOrderRepository.java`: `boolean existsByCustomer_Id(Long)`;
      `TestDriveAppointmentRepository.java`: `boolean existsByCustomer_Id(Long)`.
- [ ] `service/CustomerService.java` + `Impl`: `update`, `delete` (chặn nếu có đơn/lịch lái thử).
- [ ] `controller/CustomerController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A5. Employee
- [ ] `repository/SalesOrderRepository.java`: `boolean existsByEmployee_Id(Long)`;
      `TestDriveAppointmentRepository.java`: `boolean existsByEmployee_Id(Long)`.
- [ ] `service/EmployeeService.java` + `Impl`: `update`, `delete` (chặn nếu đang phụ trách đơn/lịch).
- [ ] `controller/EmployeeController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A6. Payment
- [ ] `service/PaymentService.java` + `Impl`: `update(Long, PaymentRequest)` (amount/method/status/paidAt),
      `delete(Long)`.
- [ ] `controller/PaymentController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A7. Contract
- [ ] `service/ContractService.java` + `Impl`: `update(Long, ContractRequest)`, `delete(Long)`.
- [ ] `controller/ContractController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A8. Invoice
- [ ] `service/InvoiceService.java` + `Impl`: `update(Long, InvoiceRequest)`, `delete(Long)`.
- [ ] `controller/InvoiceController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A9. SalesOrder
- [ ] `service/SalesOrderService.java` + `Impl`: `update(Long, SalesOrderRequest)` (chỉ đổi `employee`/`items` khi `PENDING`;
      nếu không cho sửa → dùng PATCH status hiện có), `delete(Long)` (**chỉ khi `CANCELLED` và không có Payment/Contract/Invoice**).
- [ ] `repository/PaymentRepository.java`: `boolean existsByOrder_Id(Long)`; tương tự `ContractRepository`, `InvoiceRepository`.
- [ ] `controller/SalesOrderController.java`: `PUT /{id}`, `DELETE /{id}`.

#### A10. TestDriveAppointment
- [ ] `service/TestDriveAppointmentService.java` + `Impl`: `update(Long, TestDriveAppointmentRequest)` (scheduledAt/note/car/employee),
      `delete(Long)`.
- [ ] `controller/TestDriveAppointmentController.java`: `PUT /{id}`, `DELETE /{id}`.

### Nhóm B — OrderItem (ưu tiên thấp)
- [ ] `dto/OrderItemRequest.java`: dùng lại (carId, quantity).
- [ ] Thêm endpoint vào `SalesOrderController` (hoặc controller riêng): `POST/GET/PUT/DELETE /api/orders/{orderId}/items[/{itemId}]`.
- [ ] Service xử lý: kiểm tra tồn kho, cộng/trừ `quantity` của `Car`, cập nhật lại `SalesOrder.totalAmount`.

### Nhóm C — NoSQL (Mongo)
- [ ] `service/CarReviewService.java` + `Impl`: `getAll()`, `update(String id, CarReviewRequest)`, `delete(String id)`.
- [ ] `controller/CarReviewController.java`: `GET /api/car-reviews`, `PUT /{id}`, `DELETE /{id}`.
- [ ] `service/CarSpecificationService.java` + `Impl`: `getAll()`; `controller`: `GET /api/car-specs`.
- [ ] `service/CarMediaService.java` + `Impl`: `getAll()`; `controller`: `GET /api/car-media`.
- [ ] `ActivityLog`: giữ nguyên (bất biến).

### Nhóm D — Kiểm thử
- [ ] `controller/*ControllerMockMvcTests`: thêm case `PUT` (200) và `DELETE` (204) cho ít nhất
      Brand, CarModel, Car, Customer.
- [ ] Service test: `update` đổi dữ liệu đúng; `delete` chặn khi còn tham chiếu (kỳ vọng exception).
- [ ] Test case lỗi: `PUT`/`DELETE` với id không tồn tại → 404.
- [ ] Chạy `.\mvnw.cmd test` — tất cả xanh.

### Nhóm E — Tài liệu
- [ ] Cập nhật danh sách endpoint trong `README.md`.
- [ ] Cập nhật mục "Lộ trình" trong `plan.md` nếu có thay đổi.
- [ ] Kiểm tra Swagger `/swagger-ui.html` hiển thị đủ endpoint mới.

---

## 4. Chi tiết chính sách xoá (guard)

| Xoá | Điều kiện chặn | Thông báo gợi ý |
| --- | --- | --- |
| Brand | còn `CarModel` | "Không thể xoá hãng vì còn dòng xe" |
| CarModel | còn `Car` | "Không thể xoá dòng xe vì còn xe" |
| Car | có `OrderItem` hoặc `TestDriveAppointment` | "Không thể xoá xe vì đã phát sinh giao dịch" |
| Customer | có `SalesOrder` hoặc `TestDriveAppointment` | "Không thể xoá khách vì đã có đơn/lịch" |
| Employee | có `SalesOrder` hoặc `TestDriveAppointment` | "Không thể xoá nhân viên vì đang phụ trách" |
| SalesOrder | status ≠ CANCELLED, hoặc có Payment/Contract/Invoice | "Chỉ xoá đơn đã huỷ và chưa có chứng từ" |
| Payment/Contract/Invoice | (không có tham chiếu tới) → xoá tự do | – |
| TestDrive | (không có tham chiếu tới) → xoá tự do | – |

---

## 5. Acceptance criteria (buổi 2)

- [ ] Mỗi bảng ở mục 2 có đủ endpoint như ma trận mục tiêu.
- [ ] `PUT` id không tồn tại → 404; dữ liệu trùng → 409; thiếu field → 400.
- [ ] `DELETE` bị chặn khi còn tham chiếu, trả 400 kèm thông báo rõ.
- [ ] `.\mvnw.cmd test` xanh toàn bộ.
- [ ] `.\mvnw.cmd clean package` build thành công.
- [ ] Swagger hiển thị đầy đủ CRUD.
