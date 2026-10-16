# Task 5 — Tách `Car` → `Car` + `CarItem`, tài khoản người dùng & tinh chỉnh UI

> **Mục tiêu:** (1) sửa các lỗi/luồng UI ở frontend (đăng nhập/đăng ký, FilterPanel, danh sách sản phẩm, media xe),
> (2) **tái cấu trúc dữ liệu xe thành 2 bảng `cars` + `car_items`** để mỗi xe là một thực thể riêng
> (trạng thái riêng, giá theo tình trạng mới/cũ), (3) bổ sung **quản lý trạng thái tài khoản** cho admin
> và **tự cập nhật thông tin** cho user.
>
> **Nguyên tắc làm việc:** *backend trước — API xong mới làm frontend*. Mọi thay đổi phải giữ backend
> **`./mvnw test` xanh** và frontend **`npm run build` xanh**.
>
> **Trạng thái: HOÀN TẤT — đợt 1, 2, 3 & frontend XONG.** Đã hoàn thành: UI (quyết định #6, #7, #8), bug đăng ký/tài khoản
> (§4.1–4.2), **API quản trị user + user tự cập nhật** (§4.3–4.4), **tách `Car` → `cars` + `car_items`**
> (§3), và **toàn bộ frontend** (§5.4–5.6: types/API, admin dòng xe + xe trong kho + người dùng, trang tài khoản).
> Đã chạy thật trên DDEV (DB đã reset & seed lại) và build frontend production.
>
> Bằng chứng: `./mvnw test` **110/110 PASS** (84 → 110) · `npm run build` **xanh** · test HTTP thật trên DDEV cho
> car_items CRUD, đơn hàng/lái thử theo từng chiếc, dashboard/drilldown, quản trị user, hồ sơ `/me` (xem §8).

---

## 0. Tám quyết định đã chốt (bạn xác nhận)

| # | Vấn đề | **Chốt** |
|---|---|---|
| 1 | Giỏ hàng / đơn hàng | **Mỗi dòng đơn là MỘT chiếc xe** (`order_items.car_item_id`); giỏ Redis lưu `carItemId` |
| 2 | Migration dữ liệu cũ | **Xoá & seed lại** (môi trường demo) — không viết script SQL migration |
| 3 | Lái thử | **Chọn xe cụ thể** (`test_drive_appointments.car_item_id`) |
| 4 | User tự cập nhật | **Cả 2**: tài khoản (email/mật khẩu) **và** hồ sơ khách (tên/ĐT/địa chỉ) |
| 5 | Trang sản phẩm | **Danh sách vẫn theo LÔ**; **chỉ trang chi tiết** mới liệt kê các xe cụ thể còn khả dụng |
| 6 | FilterPanel | **Thanh NGANG** (đổi lại yêu cầu task4 vốn là thanh dọc) |
| 7 | Danh sách sản phẩm | **Nằm trọn trong 1 view** (không phải cuộn) |
| 8 | Media của xe | Có preview (thumbnail) nhưng **click không ăn** → phải sửa |

---

## 1. Kết quả kiểm chứng hiện trạng (đã khảo sát code + chạy thật backend)

### 1.1. Bảng đối chiếu yêu cầu → hiện trạng

| # | Yêu cầu của bạn | Hiện trạng trong code | Kết luận |
|---|---|---|---|
| 1 | Không cho mở form login khi đã đăng nhập | `frontend/src/app/login/page.tsx` **không có guard**; vẫn render form khi đã có `user` | ❌ Cần sửa |
| 2 | Form đăng ký "yêu cầu đăng nhập mới đăng ký được" | **Bug thật**: đăng ký **không gửi `role`** → backend ném NPE → bị mask thành **HTTP 401 "Chưa xác thực…"** | ❌ Cần sửa (root cause ở backend) |
| 3 | Mặc định role là `USER` khi đăng ký | `AuthController` nhận `role` **từ client**, `AuthServiceImpl` dùng luôn → **ai cũng đăng ký được ADMIN** | ❌ Cần sửa + vá leo quyền |
| 4 | FilterPanel **thanh ngang** | Hiện là **thanh dọc** trong sidebar `lg:w-72` (`cars/page.tsx:124-134`, `FilterPanel.tsx:105`) | ❌ Cần đổi sang ngang |
| 5 | **Danh sách sản phẩm** nằm trọn trong 1 view | `/cars` dùng sidebar dọc + `py-12` + grid 4 cột (`cars/page.tsx:115,122-134`) → phải cuộn | ❌ Cần sửa |
| 6 | `Car` có cả `status` và `quantity` là bất hợp lý | `Car.java:40-54` có cả `quantity` **và** `status`; `SalesOrderServiceImpl:263-272` set `SOLD` cho **cả lô** khi hết hàng | ❌ Cần tách bảng |
| 7 | Admin set trạng thái tài khoản user | `User.enabled` **đã có** (`User.java:41-42`), `UserDetailsServiceImpl:31-33` chặn, JWT filter kiểm tra lại mỗi request (`JwtAuthenticationFilter:46-47`) — nhưng **chưa có API/UI nào để set** | ❌ Cần thêm API + trang admin |
| 8 | User cập nhật thông tin | `/account` **chỉ tạo** hồ sơ khách (`account/page.tsx:89-108`); `PUT /api/customers/**` bị chặn cho user (`SecurityConfig:113` → chỉ EMPLOYEE/ADMIN) | ❌ Cần thêm API + form |
| 9 | **Media xe: có preview nhưng click không ăn** | `cars/[id]/page.tsx` là **server component**; thumbnail là `<button>` **không có `onClick`** (dòng 103-113, 120-130) | ❌ Cần sửa (tách client component) |

### 1.2. Bug đăng ký — nguyên nhân gốc (đã chạy thật để xác nhận)

Chuỗi lỗi:

1. `types/user.ts:18-22` — `RegisterRequest` của frontend **không có `role`**; `register/page.tsx:31` gửi
   `{ username, password, email }`.
2. `AuthController.java:54-60` — record `RegisterRequest(username, password, email, role)` → `role = null`,
   rồi `authService.register(..., request.role())` (`:39-44`).
3. `AuthServiceImpl.java:68-70` gọi `isValidRole(null)`; `isValidRole` (`:106-113`) chỉ bắt
   `IllegalArgumentException`, nhưng `Role.valueOf(null)` ném **`NullPointerException`** → 500.
4. `GlobalExceptionHandler` không có handler cho NPE → Spring forward sang `/error`.
5. `SecurityConfig` **không permit `/error`** (`:53-121`) → request `/error` bị coi là chưa xác thực → **401**.

Kết quả đo được (backend chạy thật trên DDEV):

```text
POST /api/auth/register  {"username":"t5_norole","password":"1234","email":"t5a@example.com"}
→ HTTP 401  {"status":401,"error":"Unauthorized",
             "message":"Chưa xác thực hoặc phiên làm việc đã hết hạn","path":"/error"}

POST /api/auth/register  {... ,"role":"ADMIN"}
→ HTTP 201  {"id":4,...,"role":"ADMIN","enabled":true}    ← leo quyền!

POST /api/auth/register  {... ,"role":"USER"}
→ HTTP 201  {"id":5,...,"role":"USER"}
```

→ Người dùng thấy thông báo kiểu *"cần đăng nhập…"* khi bấm Đăng ký, đúng như bạn mô tả. Đây **không phải**
lỗi thiếu quyền mà là lỗi NPE bị mask thành 401. *(Đã dọn 2 user test khỏi DB sau khi đo: `users=3`.)*

### 1.3. Vì sao `status` + `quantity` trên cùng `cars` gây sai (đúng như bạn nói)

- `Car.java:40-54`: `quantity` (số lượng cả lô) + `status` (trạng thái cả lô).
- `SalesOrderServiceImpl:263-272`: khi `quantity` về 0 → `car.setStatus(SOLD)`; nhưng nếu `status` bị set
  `RESERVED`/`MAINTENANCE` (seed hiện tại: Fortuner `RESERVED` với `quantity=4`, `DataSeeder:250-251`) thì
  toàn bộ 4 xe bị coi là không bán được.
- Hệ quả UI: `CarCard.tsx:32-38` phủ overlay "Không thể đặt", `CarPurchasePanel.tsx:63` `disabled={!isAvailable}`
  → cả lô bị khoá dù vẫn còn xe.

### 1.4. Bug media — preview nhưng click không ăn

- `app/cars/[id]/page.tsx:39` là **server component** (`export default async function CarDetailPage`).
- Khối gallery (`:87-131`) render **ảnh chính + 2 hàng thumbnail** bằng `<button>` nhưng **không gắn `onClick`**
  (`:103-113` và `:120-130`) → bấm thumbnail **không đổi ảnh chính**; ảnh chính luôn lấy `thumbnail ?? images[0]`
  (`:88-93`).
- Trang admin `app/admin/media/page.tsx:59-66` cũng render `<img>` preview **không có tương tác** (bấm không mở gì).

→ Cách sửa: tách gallery thành **client component** `components/car/CarGallery.tsx` (`useState` cho ảnh đang xem,
`onClick` đổi ảnh, có `onError` fallback); trang admin cho bấm preview mở ảnh gốc.

---

## 2. Quyết định thiết kế (chi tiết kỹ thuật)

### 2.1. Mô hình dữ liệu: `cars` (dòng xe) + `car_items` (từng xe vật lý)

| Bảng | Vai trò | Cột |
|---|---|---|
| `cars` | **Sản phẩm / phiên bản** hiển thị catalog | `id, name, description, base_price, model_id` |
| `car_items` | **Từng chiếc xe** trong kho | `id, car_id, vin, color, model_year, condition, price, status, mileage_km, note` |

- `condition` = `NEW | USED` → **giá khác nhau** (`car_items.price` là giá thật của chiếc đó).
- `status` = `AVAILABLE | RESERVED | SOLD | MAINTENANCE` (tái dùng `CarStatus`), **mỗi xe một trạng thái**.
- `cars.base_price` = giá niêm yết để hiển thị "từ …" khi chưa có xe nào trong kho.
- Chuyển khỏi `cars`: `quantity`, `vin`, `color`, `model_year`, `status` → sang `car_items`
  (`cars.vin` bị bỏ vì VIN là của **xe vật lý**).

### 2.2. Quy tắc hiển thị danh sách vs chi tiết (theo quyết định #5)

| Màn hình | Hiển thị |
|---|---|
| `/` (trang chủ) và `/cars` (danh sách) | **Theo LÔ / dòng xe** — giá "từ …", tổng xe còn, badge trạng thái **suy ra** |
| `/cars/{id}` (chi tiết) | Thông tin dòng xe + **danh sách các xe còn khả dụng** (VIN, màu, năm, tình trạng, giá) để chọn chiếc cụ thể |

**Quy tắc suy ra `status` của dòng xe** (để `StatusBadge`/`CarCard` ở danh sách vẫn chạy):

| Điều kiện | status suy ra |
|---|---|
| có ≥ 1 xe `AVAILABLE` | `AVAILABLE` |
| không có `AVAILABLE`, có `RESERVED` | `RESERVED` |
| chỉ còn `MAINTENANCE` | `MAINTENANCE` |
| tất cả `SOLD` (hoặc chưa có xe nào) | `SOLD` |

### 2.3. Ảnh hưởng giỏ hàng / đơn hàng / lái thử

- `order_items` thêm `car_item_id` (**NOT NULL**) — mỗi dòng đơn là **một chiếc xe**; giữ `quantity`
  (luôn = 1) và `unit_price` để **không phải sửa các câu SQL báo cáo** (`SUM(oi.quantity)`,
  `SUM(oi.quantity*oi.unit_price)`).
- Giỏ hàng (Redis, `ConfiguratorCartService`) chuyển khoá `carId` → `carItemId`
  (`CartItem { carItemId }`), mỗi xe một dòng.
- `test_drive_appointments` đổi `car_id` → **`car_item_id`**; khách chọn **chiếc xe cụ thể** để lái thử.
- **Mongo** (`car_reviews`, `car_specifications`, `car_media`) giữ nguyên khoá `carId` (nội dung theo dòng xe).

### 2.4. Tài khoản người dùng

- Đăng ký **công khai**, **luôn** role `USER`, **bỏ hẳn** trường `role` khỏi request (chống leo quyền).
- Admin: `GET /api/admin/users`, `PUT /api/admin/users/{id}/status` (`enabled`), `PUT /api/admin/users/{id}/role`.
- User tự cập nhật: `PUT /api/auth/me` (email, mật khẩu) **và** `PUT /api/customers/me` (hồ sơ khách).
  Cần thêm cột **`customers.user_id → users.id`** (nullable).

### 2.5. UI

- `/login` và `/register`: đã đăng nhập → **tự chuyển hướng**, không cho mở form.
- `FilterPanel`: **thanh NGANG** đặt trên danh sách, các trường xếp 1 hàng
  (`grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6`), nút "Xóa lọc" cùng hàng; mobile tự xuống dòng.
  Thêm lọc **Tình trạng (Mới/Cũ)** và **Trạng thái**, đổi nhãn "Tồn kho tối thiểu" → "Số xe còn hàng".
- `/cars` **nằm trọn trong 1 view**: bố cục cột dọc `min-h-[calc(100vh-4rem)]`, filter bar trên, grid `flex-1`,
  thẻ xe gọn (prop `compact`: ảnh `h-32`, padding nhỏ), phân trang ở đáy → không phải cuộn.
- Trang chi tiết xe: gallery tách thành client component `components/car/CarGallery.tsx`
  (**click thumbnail đổi ảnh chính**), khối đầu gọn hơn.
- Admin: tách **`/admin/cars`** (dòng xe) và **`/admin/car-items`** (từng xe) + thêm **`/admin/users`**.

---

## 3. BACKEND (A) — Tách `Car` + `CarItem`

### 3.1. Entity & schema

| File | Việc làm |
|---|---|
| `entity/CarItem.java` **(mới)** | `id, car(@ManyToOne cars), vin(unique), color, modelYear, condition(@Enumerated), price, status(@Enumerated, default AVAILABLE), mileageKm, note` |
| `entity/CarCondition.java` **(mới)** | `NEW, USED` |
| `entity/Car.java` (sửa) | Bỏ `quantity, vin, year, color, status`. `price` → **`basePrice`** (map `base_price`) |
| `entity/OrderItem.java` (sửa) | `car` → `carItem` (`car_item_id` NOT NULL); giữ `quantity`, `unitPrice` |
| `entity/TestDriveAppointment.java` (sửa) | `car` → `carItem` (`car_item_id` NOT NULL) |
| `entity/Customer.java` (sửa) | Thêm `@ManyToOne User user` (nullable, cột `user_id`) |

### 3.2. Reset & seed lại dữ liệu (theo quyết định #2 — không viết SQL migration)

1. (Tuỳ chọn nhưng nên) sao lưu: `ddev db-backup`.
2. `ddev delete -O` → **xoá sạch volume** MariaDB + MongoDB + Redis.
3. `ddev start` → hook `post-start` tạo lại database `ngoclaudb`.
4. Cập nhật `DataSeeder` (§3.8) rồi chạy backend → Hibernate `ddl-auto=update` tạo schema mới **từ entity**.
5. **Lưu ý:** phải xoá DB (không chỉ chạy `update` trên schema cũ), vì Hibernate **không tự xoá cột**
   `cars.quantity/status/vin/...` → sẽ để lại cột rác.

### 3.3. DTO & mapper

| File | Việc làm |
|---|---|
| `dto/CarRequest.java` | Bỏ `quantity, status, vin, year, color`; giữ `name, description, basePrice, modelId` |
| `dto/CarResponse.java` | Thêm `basePrice, priceFrom, priceTo, totalItems, availableItems, reservedItems, soldItems, maintenanceItems, conditions[], status` (status **suy ra**) |
| `dto/CarItemRequest.java`, `dto/CarItemResponse.java` **(mới)** | CRUD từng xe |
| `dto/OrderItemRequest.java` | `carId, quantity` → `carItemId` |
| `dto/TestDriveAppointmentRequest.java` | `carId` → `carItemId` |
| `dto/TestDriveAppointmentResponse.java` | → `carItemId, carItemVin, carId, carName` |
| `mapper/CarMapper.java`, `mapper/CarItemMapper.java` (**mới**) | Map + tổng hợp số lượng/giá |
| `mapper/SalesOrderMapper.java` | `carName` qua `item.getCarItem().getCar().getName()` |
| `mapper/TestDriveAppointmentMapper.java:18-19` | `carItem.getId()` + tên dòng xe |

### 3.4. Repository & truy vấn

| File | Việc làm |
|---|---|
| `repository/CarItemRepository.java` **(mới)** | `findByCar_IdOrderByIdAsc`, `countByCar_IdAndStatus`, `existsByVin`, `existsByCar_Id`, `findTopByCar_IdAndStatusOrderByIdAsc` (FIFO), lọc `condition`/`price`/`status` |
| `repository/CarRepository.java` | Bỏ `findByQuantityGreaterThanEqual...`; thay 3 query `minQuantity` bằng query theo **số xe AVAILABLE** |
| `repository/OrderItemRepository.java` | `existsByCar_Id` → **`existsByCarItem_Id`** |
| `repository/TestDriveAppointmentRepository.java:17` | `existsByCar_Id` → **`existsByCarItem_Id`** |
| `repository/CustomerRepository.java` | Thêm `findByUser_Id`, `existsByUser_Id` |

### 3.5. Service

| File | Việc làm |
|---|---|
| `service/CarItemService.java` + `Impl/CarItemServiceImpl.java` **(mới)** | CRUD + validate (VIN unique, giá ≥ 0) |
| `service/Impl/CarServiceImpl.java` | Bỏ validate theo `quantity`; `delete(id)` chặn khi `carItemRepository.existsByCar_Id(id)`; evict cache khi car_item đổi |
| `service/CarCatalogService.java` | `getCarDetail` trả `items[]`; thêm `evictItem(carId)` |
| `service/Impl/SalesOrderServiceImpl.java` | `deductStock` → `reserveItem(carItemId)` / `markSold` / `releaseItem` |
| `service/ConfiguratorCartService.java` | Khoá Redis theo `carItemId`; validate `AVAILABLE` |
| `service/Impl/TestDriveAppointmentServiceImpl.java:57-59,130-132` | `carRepository` → `carItemRepository` |

### 3.6. Controller & API contract

| Method | Endpoint | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/cars` | public | **danh sách theo lô**: `availableItems`, `priceFrom/priceTo`, `status` suy ra |
| GET | `/api/cars/{id}` | public | chi tiết dòng xe + `items[]` |
| GET/PUT/DELETE | `/api/car-items/{id}` | GET public, sửa/xoá ADMIN | chi tiết/sửa/xoá 1 xe |
| POST | `/api/cars/{carId}/items` | ADMIN | thêm 1 xe vào dòng |
| GET | `/api/cars/search/stock?minQuantity=n` | public | `n` = **số xe AVAILABLE tối thiểu** |
| POST | `/api/cart/items?sessionId=&carItemId=` | USER | đổi từ `carId` |
| POST | `/api/orders` | USER | body `items: [{ carItemId }]` |
| POST/PUT | `/api/test-drives`, `/api/test-drives/{id}` | authenticated | body có `carItemId` |

### 3.7. Reports / Dashboard (SQL thuần — ảnh hưởng nặng, xem §6)

| File | Việc làm |
|---|---|
| `repository/JdbcReportRepository.java` | `findDashboardSummary:102-106` + `countStatus:242-244` → đếm trên **`car_items`**; `findInventoryByBrand:52-70`, `findModelsByBrand:118-138` → `COUNT/SUM` từ `car_items`; `findCarsByModel:141-158` → trả **danh sách xe**; `findTopSellingCars:33-50` → join `order_items → car_items → cars` |
| `dto/DashboardSummary.java` | `totalCars` = **tổng xe (item)**; thêm `totalModels`, `newItems`, `usedItems` |
| `dto/InventoryByBrand.java`, `dto/InventoryByModel.java` | `totalQuantity` = số xe; thêm `availableItems` |
| `dto/CarInventoryItem.java` | Thêm `carItemId, carId, carName, condition, status, price`; bỏ `quantity` |

*`count test_drive_appointments` (`:110`) không đổi vì chỉ đếm theo `status`.*

### 3.8. Seed (`config/DataSeeder.java`)

- Sửa helper `car(...)` (`:632-645`) → tạo `Car` + sinh **N `CarItem`**: `condition` (`NEW`, một số `USED`
  **giá thấp hơn ~15%**), gán trạng thái **theo từng chiếc** (Fortuner: 1 `RESERVED`, 3 `AVAILABLE`).
- Sửa helper `item(...)` (`:684-690`) → trỏ tới `car_item`; seed `test_drive_appointments` theo `car_item`.
- Gán `customers.user_id` cho vài user để test `PUT /api/customers/me`. Giữ tính **idempotent**.

### 3.9. Security (`config/SecurityConfig.java`)

- Thêm `.requestMatchers("/error").permitAll()` (sửa triệu chứng 401 giả ở §1.2).
- `POST/PUT/DELETE /api/car-items/**`, `/api/cars/*/items` → `hasRole(ADMIN)` (**đặt trước** rule GET public).
- `PUT /api/auth/me`, `GET/PUT /api/customers/me` → `authenticated()` (**đặt trước** `/api/customers/**` staff).

### 3.10. Test backend cần cập nhật (hiện 84/84 PASS — phải giữ xanh)

| Test | Việc làm |
|---|---|
| `service/SalesOrderServiceTests.java:48-99` | Đặt 1 xe → chỉ chiếc đó `SOLD`, các chiếc khác vẫn `AVAILABLE` |
| `service/ServiceCrudTests.java:60-90` | `Car` bỏ `quantity`; thêm test CRUD `CarItemService` |
| `controller/CarControllerMockMvcTests.java:44` | Bỏ `quantity`/`status` khỏi payload |
| `controller/CarModelControllerMockMvcTests.java:93` | Tạo `CarItem` thay vì set `quantity` |
| `repository/CarRepositorySearchTests.java:129` | Tạo `car_items` để test lọc theo `minQuantity` |
| `controller/ReportControllerMockMvcTests.java:52-74` | Seed `car_items`; sửa assert `CarInventoryItem` |
| `controller/SecurityAuthorizationTests.java` | Thêm case 401/403 cho endpoint mới |
| `controller/AuthControllerMockMvcTests.java` | Đăng ký **không `role` → 201 + `role=USER`**; gửi `role=ADMIN` → vẫn `USER` |
| **mới** | `CarItemControllerMockMvcTests`, `UserAdminControllerMockMvcTests` |

---

## 4. BACKEND (B) — Đăng ký, tài khoản user, quản trị user

### 4.1. Sửa luồng đăng ký

| File | Việc làm |
|---|---|
| `controller/AuthController.java:36-46,54-60` | `RegisterRequest` bỏ trường `role`; thêm `@Valid` |
| `service/AuthService.java:12` | `register(username, password, email)` — bỏ tham số `role` |
| `service/Impl/AuthServiceImpl.java:62-82,106-113` | Luôn `user.setRole(Role.USER)`; xoá `isValidRole` (hết NPE); kiểm tra trùng username/email |
| `dto/RegisterRequest.java` **(mới)** | record request dùng chung, có validation |

### 4.2. Xử lý lỗi xác thực cho đúng (bỏ mask 401)

| File | Việc làm |
|---|---|
| `exception/GlobalExceptionHandler.java` | Handler `UsernameNotFoundException`/`BadCredentialsException` → **401** với message rõ; thêm fallback để lỗi 500 **không bị biến thành 401** |
| `config/SecurityConfig.java` | `permitAll("/error")` |
| `service/Impl/AuthServiceImpl.java:39-54` | Message thống nhất khi login sai; `!user.isEnabled()` → "Tài khoản đã bị khoá" |

### 4.3. API admin quản lý tài khoản

| File | Việc làm |
|---|---|
| `service/UserService.java` + `Impl/UserServiceImpl.java` **(mới)** | `list()`, `updateStatus(id, enabled)`, `updateRole(id, role)`, `updateProfile(...)` |
| `controller/UserAdminController.java` **(mới)** `/api/admin/users` | `GET`; `PUT /{id}/status`; `PUT /{id}/role` |
| `dto/UserResponse.java` | + `createdAt` |
| `dto/UserStatusRequest.java`, `dto/UserRoleRequest.java` **(mới)** | có validation |

**Luật an toàn:** admin **không tự khoá chính mình**; không hạ role của user `admin` gốc; ghi `ActivityLog`.

### 4.4. API user tự cập nhật (quyết định #4)

| Method | Endpoint | Quyền | Nội dung |
|---|---|---|---|
| PUT | `/api/auth/me` | authenticated | `{ email, password? }` |
| GET/PUT | `/api/customers/me` | authenticated | đọc/cập nhật `fullName, phone, address, nationalId` |

### 4.5. Ma trận RBAC cập nhật

| Endpoint | PUBLIC | USER | EMPLOYEE | ADMIN |
|---|---|---|---|---|
| `POST /api/auth/register` (luôn USER) | ✅ | – | – | – |
| `PUT /api/auth/me`, `GET/PUT /api/customers/me` | – | ✅ | ✅ | ✅ |
| `GET /api/admin/users` | – | – | – | ✅ |
| `PUT /api/admin/users/{id}/status`, `/role` | – | – | – | ✅ |
| `GET /api/cars/*/items`, `/api/car-items/{id}` | ✅ | ✅ | ✅ | ✅ |
| `POST/PUT/DELETE` `car-items` / `cars/*/items` | – | – | – | ✅ |
| `/error` | ✅ | ✅ | ✅ | ✅ |

---

## 5. FRONTEND

### 5.1. Đăng nhập / đăng ký

| File | Việc làm |
|---|---|
| `app/login/page.tsx` | Nếu `isAuthenticated` → `useEffect` `router.replace(next)`; không render form |
| `app/register/page.tsx` | Đăng ký → tự đăng nhập → `/account`; nếu đã đăng nhập → chuyển hướng; hiện lỗi server đúng nghĩa |
| `context/AuthContext.tsx:72-79` | Giữ auto-login sau đăng ký; `refresh()` cập nhật `enabled` |
| `lib/api/auth.ts`, `types/user.ts` | `RegisterRequest` không có `role` + thêm `updateMe({email, password?})` |
| `components/layout/Header.tsx:109-181` | (đã đúng) ẩn Đăng nhập/Đăng ký khi đã đăng nhập; thêm mục **"Người dùng"** cho admin |

### 5.2. `FilterPanel` — thanh NGANG (quyết định #6)

| File | Việc làm |
|---|---|
| `components/ui/FilterPanel.tsx` | **Đổi sang thanh ngang**: `grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 gap-x-4 gap-y-3 items-end`, bỏ `flex flex-col`; nhóm "Hãng xe" (1), "Dòng xe" (1), "Khoảng giá" (2 cột, Từ–Đến), "Số xe còn hàng" (1), ô hành động ("Xóa lọc" + chấm "Đang áp dụng") (1). Giữ nguyên logic URL params |
| `app/cars/page.tsx` | Bỏ `<aside class="lg:w-72">` + `flex-col lg:flex-row`; đặt `FilterPanel` **full-width phía trên** kết quả |
| `lib/api/cars.ts` | Thêm tham số `condition`/`status` cho các hàm search |

### 5.3. Danh sách sản phẩm trọn 1 view + media xe (quyết định #7, #8)

| File | Việc làm |
|---|---|
| `app/cars/page.tsx` | Bố cục cột dọc **`min-h-[calc(100vh-4rem)] flex flex-col`**, `py-6`; breadcrumb + tiêu đề gọn; `FilterPanel` ngang; vùng kết quả `flex-1`; phân trang đáy. `size = 8` |
| `components/ui/CarGrid.tsx` | Thêm prop `compact` truyền xuống `CarCard` |
| `components/ui/CarCard.tsx` | Thêm prop `compact`: ảnh `h-32` (thay `h-48`), `p-4`, tiêu đề `text-base` → thẻ gọn để 2 hàng lọt 1 view |
| `components/car/CarGallery.tsx` **(mới)** | **Client component** gallery: `useState` ảnh đang xem, **`onClick` thumbnail đổi ảnh chính**, `onError` fallback, badge trạng thái |
| `app/cars/[id]/page.tsx` | Thay khối gallery (`:87-131`) bằng `<CarGallery …/>`; bỏ `<button>` chết |
| `app/admin/media/page.tsx` | Preview trong bảng **bấm được** (mở ảnh gốc tab mới) |

### 5.4. Admin

| File | Việc làm |
|---|---|
| `app/admin/cars/page.tsx` | Bỏ `quantity, vin, color, year, status` khỏi form; thêm cột `availableItems/totalItems`, `priceFrom` |
| `app/admin/car-items/page.tsx` **(mới)** | `ResourceManager` cho `CarItem` |
| `app/admin/users/page.tsx` **(mới)** | Danh sách user; **Khoá/Mở khoá**, đổi role; ẩn thao tác với chính mình |
| `app/admin/test-drives/page.tsx:27` | Form chọn **xe cụ thể** (`carItemId`) |
| `app/admin/orders/page.tsx:103` | Chọn **xe cụ thể** khi thêm dòng đơn |
| `components/layout/AdminSidebar.tsx:6-22` | Thêm mục **"Xe trong kho"** và **"Người dùng"** |
| `app/admin/page.tsx:180-190` | KPI đổi nhãn: "Tổng xe" = tổng **item**, thêm "Xe mới / Xe cũ"; drilldown `vin/condition/price/status` |

### 5.5. Trang tài khoản user (`app/account/page.tsx`)

- Tab **"Thông tin đăng nhập"**: đổi `email`, `password` (`PUT /api/auth/me`).
- Tab `profile`: **sửa** hồ sơ (`GET/PUT /api/customers/me`).
- Hiển thị **trạng thái tài khoản** (Đang hoạt động / Đã bị khoá).

### 5.6. Types & API modules

| File | Việc làm |
|---|---|
| `types/car.ts` | `Car` bỏ `quantity/vin/year/color` → thêm `basePrice, priceFrom, priceTo, totalItems, availableItems, status`; thêm `CarItem`, `CarCondition` |
| `types/testDrive.ts` | `carId` → **`carItemId`** |
| `types/order.ts`, `types/cart.ts` | `carId` → **`carItemId`** |
| `types/report.ts` | `CarInventoryItem` + `condition`; `DashboardSummary` + `newItems/usedItems` |
| `types/user.ts` | `UpdateMeRequest`, `UserStatusRequest` |
| `lib/api/cars.ts`, `lib/api/carItems.ts` **(mới)**, `lib/api/users.ts` **(mới)**, `lib/api/testDrives.ts`, `lib/api/index.ts` | Thêm/sửa hàm theo endpoint §3.6 & §4.3–4.4 |

---

## 6. Ảnh hưởng Dashboard & Admin (bảng theo dõi bắt buộc)

| Nơi | Hiện tại (1 bảng `cars`) | Sau khi tách | Rủi ro |
|---|---|---|---|
| `JdbcReportRepository.findDashboardSummary` | `COUNT(*) FROM cars`, `COUNT WHERE status=?` | `COUNT(*) FROM car_items` | KPI **đổi nghĩa số** → đổi nhãn UI |
| `findInventoryByBrand` / `findModelsByBrand` | `COUNT(c.id)`, `SUM(c.quantity)` | `COUNT(ci.id)`, thêm `availableItems` | Bảng drilldown hiển thị khác |
| `findCarsByModel` | `c.vin, c.status, c.quantity, c.price` | `ci.vin, ci.status, ci.condition, ci.price` | `CarInventoryItem` đổi → sửa cột |
| `findTopSellingCars` | join `order_items → cars` | join `order_items → car_items → cars` | Quên 2 tầng → sai dữ liệu |
| `/admin/cars` | 1 màn CRUD `quantity` + `status` | 2 màn: dòng xe & xe trong kho | Admin phải học luồng mới |
| `/admin/orders`, `/admin/test-drives` | chọn dòng xe | chọn **xe cụ thể** | Form phải đổi theo |
| Redis cache `carDetail`/`carSearch` | evict khi sửa `cars` | **phải evict khi sửa `car_items`** | Quên → hiển thị tồn kho cũ |
| Mongo (`reviews/specs/media`) | khoá `carId` | **không đổi** | - |

---

## 7. Thứ tự thực thi (backend-first)

1. ✅ **XONG** Frontend UI: `FilterPanel` sang **thanh ngang**, `/cars` **trọn 1 view** (8 thẻ = 2 hàng × 4 cột),
   `components/car/CarGallery.tsx` bấm thumbnail đổi ảnh, admin media preview bấm được.
2. ✅ **XONG** Backend §4.1–4.2: bỏ `role` khỏi đăng ký (luôn `USER`), `permitAll("/error")`,
   handler `AuthenticationException` → `401` kèm thông báo rõ. `./mvnw test` **86/86 PASS**.
3. ✅ **XONG** Frontend §5.1: guard `/login`, `/register` (đã đăng nhập → tự chuyển hướng, không mở form).
4. ✅ **XONG** Backend §4.3–4.4: `GET /api/admin/users`, `PUT /{id}/status`, `PUT /{id}/role`,
   `PUT /api/auth/me`, `GET/PUT /api/customers/me` (thêm cột `customers.user_id`). `./mvnw test` **101/101 PASS**.
5. ✅ **XONG** Backend §3.1–3.5: entity `CarItem` (+`CarCondition`), DTO `CarItemRequest/Response`,
   `CarMapper` (tổng hợp) + `CarItemMapper`, `CarRepository` viết lại, `CarItemRepository`, `CarService`
   + `CarItemService`, `CarController` + `CarItemController` (`/api/car-items`). Cột `condition` đổi thành
   **`car_condition`** vì `condition` là từ khoá dành riêng của MariaDB.
6. ✅ **XONG** Backend §3.7: `JdbcReportRepository` (dashboard/inventory/top-selling/drilldown) nay đếm theo
   `car_items`; `CarInventoryItem` thêm `condition/mileageKm`, bỏ `quantity`.
7. ✅ **XONG** Backend §3.5/§3.6: `order_items` & `test_drive_appointments` đổi sang **`car_item_id`**;
   `OrderItemRequest.carItemId` (+ `quantity` phải = 1), `TestDriveAppointmentRequest.carItemId`, giỏ Redis
   dùng `carItemId`. Không xoá được xe/dòng xe khi đã phát sinh giao dịch.
8. ✅ **XONG** Backend §3.8 + reset §3.2: `DataSeeder` seed 23 dòng xe + 85 chiếc (20 xe cũ giá thấp hơn);
   `ddev db-backup` → drop & tạo lại `ngoclaudb` → chạy backend (Hibernate tạo schema + seed).
9. ✅ **XONG** `./mvnw test` **110/110 PASS** (H2) **và** chạy thật trên DDEV (MariaDB + Mongo + Redis): xem §8.
10. ✅ **XONG** Frontend §5.6 types/api → §5.4 admin → §5.5 tài khoản. `npm run build` **xanh** (TypeScript pass);
    đã chạy thật backend + frontend, xác nhận tất cả route `200`. `README.md` đã cập nhật.

---

## 8. Acceptance criteria

**Frontend UI (đợt 1) — ✅ xong, đã kiểm chứng**
- [x] `npm run build` xanh, không lỗi TypeScript (`exit 0`, `✓ Compiled successfully`).
- [x] `/cars`: `FilterPanel` là **thanh NGANG** phía trên danh sách (HTML thật có class `xl:grid-cols-6`, không còn `lg:w-72`).
- [x] `/cars`: danh sách sản phẩm **nằm trọn trong 1 view** (8 thẻ/trang, thẻ compact `h-32`, `min-h-[calc(100vh-4rem)]`).
- [x] `/cars/[id]`: **bấm thumbnail đổi được ảnh chính** — HTML thật render `<button aria-label="Xem ảnh 1..3" aria-pressed>` có `onClick`.
- [x] `/admin/media`: bấm preview mở được ảnh gốc (thẻ `<a target="_blank">`).

**Backend — ✅ xong toàn bộ (đăng ký/tài khoản + `car_item`)**
- [x] `./mvnw test` xanh: **110/110** (84 gốc → +2 đăng ký, +15 quản trị user/hồ sơ, +8 car-item, +1 base price).
- [x] `POST /api/auth/register` **không cần `role`** → `201` + `role=USER` (đo thật: `{"id":6,...,"role":"USER"}`).
- [x] Gửi `role=ADMIN` khi đăng ký → vẫn `USER` (đo thật: `{"id":7,...,"role":"USER"}`, không leo quyền).
- [x] Đăng ký thiếu trường → **400** có thông báo rõ (`"password: Mật khẩu không được để trống"`) — hết 401 "cần đăng nhập".
- [x] Đăng nhập sai mật khẩu → **401** + `"Sai tên đăng nhập hoặc mật khẩu"` (trước đây là thông báo chung chung).
- [x] Đặt 1 xe trong lô 5 xe → **chỉ chiếc đó** chuyển `SOLD`; các chiếc còn `AVAILABLE` (đo thật: đơn `carItemId=2`
      → item 2 `SOLD`, `availableItems` 4; huỷ đơn → item 2 `AVAILABLE` trở lại).
- [x] Giá khác nhau giữa `NEW` và `USED` cho cùng dòng xe (đo thật: Camry 2.0G `priceFrom=808` / `priceTo=950`,
      chiếc USED `mileageKm=35000`, giá `808` = 950×0,85).
- [x] `GET /api/cars` trả số liệu **tổng hợp theo lô**; `GET /api/cars/{id}` trả `items[]` **từng xe**
      (đo thật: danh sách `items: []`, chi tiết có 5 phần tử).
- [x] Lái thử tạo được với **`carItemId`** cụ thể (đo thật: `{"carItemId":5,"carId":1,"vin":"VIN-CAM20-05"}`).
- [x] `/api/car-items`: đọc công khai, ghi cần ADMIN (đo thật: POST không token → 401, có token → 201);
      VIN trùng → 400; `PATCH /{id}/status` giữ nguyên VIN/màu/giá.
- [x] Admin khoá user → user đó **bị 401 ngay** (đo thật: khoá `employee` → login trả `401 "Tài khoản đã bị khóa: employee"`); mở khoá → đăng nhập lại được.
- [x] Admin **không tự khoá được chính mình** (đo thật: `400 "Không thể khoá tài khoản quản trị gốc"`); không hạ quyền `admin` gốc; USER gọi `/api/admin/users` → **403**.
- [x] User đổi được email/mật khẩu (`PUT /api/auth/me` → 200) **và** cập nhật hồ sơ khách (`PUT /api/customers/me` tạo mới khi chưa có, `GET` đọc lại được; chống trùng email khách → 400).
- [x] Dashboard trả số liệu đúng theo **xe vật lý** (đo thật: `totalCars=85`, `available=68`, `reserved=3`,
      `sold=13`, `maintenance=1`); drilldown theo dòng xe trả từng chiếc kèm `condition`/`mileageKm`.

**Frontend — ✅ xong toàn bộ (đợt cuối, đã kiểm chứng build + chạy thật)**
- [x] Đang đăng nhập mà vào `/login` hoặc `/register` → tự chuyển hướng, không thấy form
      (`useEffect` + `router.replace`; `isAuthenticated` → `null` thay vì render form).
- [x] Đăng ký từ UI thành công ngay, tự đăng nhập, role `USER` (đo thật: đăng ký → `id=4, role=USER` rồi login `200`).
- [x] `/` và `/cars`: **vẫn hiển thị theo lô** (`CarCard` dùng `priceFrom`..`priceTo`, `availableItems`,
      `conditions[]`; đo thật HTML `/` có cả nhãn "Mới" và "Đã dùng", `/cars/1` có "xe còn lại" + khoảng giá).
- [x] `/cars/[id]`: có **danh sách xe khả dụng** để chọn chiếc (`CarPurchasePanel` nhận `items`, `<Select>`
      "Chọn xe cụ thể"); nút "Đặt mua ngay" bật khi còn xe, khoá khi hết (đo thật HTML có "Chọn xe cụ thể").
- [x] Admin: CRUD **dòng xe** (`/admin/cars`, chỉ name/basePrice/modelId/description) và **xe trong kho**
      (`/admin/car-items`, chọn dòng xe + CRUD từng chiếc VIN/màu/năm/condition/giá/trạng thái) riêng;
      trang **Người dùng** (`/admin/users`) khoá/mở khoá + đổi vai trò được (đo thật: khoá user → login `401`,
      mở khoá → `200`; đổi `USER↔EMPLOYEE` OK; tự khoá/gốc → `400`; token USER → `403`).
- [x] `/account`: sửa được thông tin đăng nhập (`PUT /api/auth/me` — tab "Tài khoản") + hồ sơ khách
      (`GET/PUT /api/customers/me` — tab "Hồ sơ khách hàng", tạo mới khi chưa có).
- [x] Đơn hàng/lái thử ở admin dùng `carItemId` (chọn dòng xe → chọn chiếc cụ thể), khớp API mới.
- [x] `npm run build` xanh (TypeScript pass, 25 route, có `/admin/car-items` + `/admin/users`); tất cả route trả `200`.

---

## 9. Rủi ro & lưu ý

1. **Reset toàn bộ volume** (`ddev delete -O`) xoá cả **MongoDB và Redis** → phải seed lại tất cả.
2. **Không dùng `ddl-auto=create`/`create-drop`** trong `application.properties`.
3. **Cache Redis `carDetail`/`carSearch`** phải evict khi `car_items` đổi (nếu không UI hiển thị tồn kho cũ).
4. **Đổi nghĩa số liệu dashboard**: "Tổng xe" từ dòng xe → xe vật lý; phải đổi nhãn.
5. **`minQuantity`** giờ là "số xe còn hàng" (giữ tên tham số để tương thích URL cũ).
6. **Test H2 vs MariaDB**: `JdbcReportRepository` dùng SQL thuần → chạy cả `./mvnw test` **và** DDEV.
7. **`order_items` và `test_drive_appointments`** đều đổi sang `car_item_id` → cập nhật **cả hai** mapper/repo guard.
8. **Danh sách vẫn theo lô** (quyết định #5): `CarResponse` phải giữ field tổng hợp để `CarCard`/trang chủ không phải viết lại.
9. **"Trọn 1 view"** phụ thuộc kích thước màn hình: bố cục dùng `100dvh`-an toàn (`calc(100vh-4rem)`) + thẻ gọn;
   trên màn hình nhỏ vẫn có thể cuộn — đây là giới hạn vật lý, không phải bug.
10. **`/api/customers/me` cần liên kết `customers.user_id`**: khách do nhân viên tạo tại quầy
    (`POST /api/customers`) **không** gắn tài khoản → `GET /api/customers/me` trả **404** cho tới khi user gọi `PUT`
    (tự tạo hồ sơ). Vì vậy **§5.5 bắt buộc đổi trang `/account` sang `PUT /api/customers/me`** thay vì
    `POST /api/customers`. Seeder có gắn sẵn `user` ↔ khách `c1` cho DB mới; DB cũ phải `PUT` một lần.
    Ngoài ra email khách phải **không trùng** hồ sơ khác (nếu user chưa có hồ sơ mà chọn email đã tồn tại → `400`).

---

## 10. Danh sách file tóm tắt

**Frontend — mới (~5):** `app/admin/car-items/page.tsx`, `app/admin/users/page.tsx`, `components/car/CarGallery.tsx`,
`lib/api/carItems.ts`, `lib/api/users.ts`.

**Frontend — sửa (~29):** `app/{login,register,page}.tsx`, `app/cars/page.tsx`, `app/cars/[id]/page.tsx`,
`app/{cart,checkout,account}/page.tsx`, `app/admin/{page,cars,orders,test-drives,media}.tsx`,
`components/ui/{FilterPanel,CarCard,CarGrid}.tsx`, `components/car/CarPurchasePanel.tsx`,
`components/layout/{AdminSidebar,Header}.tsx`, `context/AuthContext.tsx`,
`lib/api/{auth,cars,customers,testDrives,index}.ts`,
`types/{car,carItem,testDrive,order,cart,report,user,index}.ts`.

**Backend — mới (15 + 2 test):** `entity/CarItem.java`, `entity/CarCondition.java`, `repository/CarItemRepository.java`,
`dto/CarItemRequest.java`, `dto/CarItemResponse.java`, `dto/UserStatusRequest.java`, `dto/UserRoleRequest.java`,
`dto/UpdateMeRequest.java`, `service/CarItemService.java`, `service/Impl/CarItemServiceImpl.java`,
`service/UserService.java`, `service/Impl/UserServiceImpl.java`, `mapper/UserMapper.java`,
`controller/CarItemController.java`, `controller/UserAdminController.java`,
test: `CarItemControllerMockMvcTests`, `UserAdminControllerMockMvcTests`.
(`RegisterRequest` là record lồng trong `AuthController`, không tách file.)

**Backend — sửa (~36 + 6 test):** `entity/{Car,OrderItem,TestDriveAppointment,Customer}.java`,
`controller/{AuthController,CarController,CustomerController,TestDriveAppointmentController}.java`,
`service/AuthService.java`, `service/Impl/{AuthServiceImpl,CarServiceImpl,SalesOrderServiceImpl,TestDriveAppointmentServiceImpl}.java`,
`service/{CarCatalogService,ConfiguratorCartService}.java`,
`repository/{CarRepository,OrderItemRepository,TestDriveAppointmentRepository,CustomerRepository,JdbcReportRepository}.java`,
`mapper/{CarMapper,SalesOrderMapper,TestDriveAppointmentMapper}.java`,
`dto/{CarRequest,CarResponse,OrderItemRequest,TestDriveAppointmentRequest,TestDriveAppointmentResponse,DashboardSummary,InventoryByBrand,InventoryByModel,CarInventoryItem,UserResponse}.java`,
`config/{SecurityConfig,DataSeeder}.java`, `exception/GlobalExceptionHandler.java`, + 6 test cũ.

---

## 11. Nhật ký thay đổi kế hoạch

- **Lái thử** chuyển từ `car_id` → **`car_item_id`** (quyết định #3).
- **Bỏ hoàn toàn** script SQL migration → dùng **reset + seed lại** (quyết định #2).
- **Làm rõ** quyết định #5: danh sách vẫn theo lô, chỉ chi tiết mới liệt kê từng xe.
- **Đảo yêu cầu FilterPanel**: task4 làm **thanh dọc**, task5 chuyển lại **thanh ngang** (quyết định #6).
- **Đổi mục tiêu "trọn 1 view"**: từ *trang chi tiết* → **danh sách sản phẩm `/cars`** (quyết định #7).
- **Thêm bug media** (quyết định #8): gallery chi tiết xe là server component nên thumbnail `<button>` không có `onClick`.
