# Task 4 — Hoàn thiện Full Frontend + Phân quyền RBAC & Dashboard Drilldown (Backend)

> **Mục tiêu:** hoàn thiện **toàn bộ frontend** (public + user + admin) cho website đại lý xe, đồng thời
> bổ sung ở backend: **phân quyền đầy đủ cho các API CRUD** và **các API dashboard hỗ trợ drilldown**.
>
> Khác với Buổi 3/4 (chỉ build frontend, không động backend), Buổi 4 **được phép sửa backend** vì có
> yêu cầu thêm RBAC + API mới. Backend vẫn phải **`.\mvnw.cmd test` xanh**; frontend vẫn
> **`npm run build` xanh**.
>
> **Trạng thái: CHƯA CODE** — tài liệu này là danh sách thay đổi chốt trước khi viết code.

---

## 0. Phạm vi & quyết định thiết kế (chốt)

### 0.1. Backend
1. **RBAC (Role-Based Access Control)** thay cho `anyRequest().permitAll()` hiện tại.
   Roles: `ADMIN`, `EMPLOYEE`, `USER` (đã có sẵn trong `entity/Role.java`).
2. Thực thi phân quyền **tập trung tại `config/SecurityConfig.java`** bằng `requestMatchers` theo
   HTTP method (không rải `@PreAuthorize` khắp controllers → dễ đọc, dễ dạy, một chỗ duy nhất).
3. **API dashboard + drilldown** bổ sung vào `ReportController` / `ReportService` / `JdbcReportRepository`
   (SQL thuần `JdbcTemplate`) + DTO mới trong `dto/`.
4. **Cập nhật test hiện có**: các `*ControllerMockMvcTests` (Brand/CarModel/Car/Customer) đang gọi
   `POST/PUT/DELETE` **không auth** và mong đợi 2xx. Sau khi bật RBAC, các write này là ADMIN/staff →
   phải **đăng nhập lấy JWT admin** rồi gắn header `Authorization: Bearer`. Bổ sung **test RBAC mới**.
5. **Không đổi** DTO/entity/service nghiệp vụ hiện có (chỉ thêm mới), không đổi response shape cũ.

### 0.2. Frontend
6. **Không cần thư viện mới** (không thêm React Query/SWR/Chart lib). Dùng:
   - `fetch` + rewrite `/api/*` (đã có) cho client; URL tuyệt đối (`NEXT_PUBLIC_API_BASE_URL`) cho RSC.
   - **Client-side auth**: JWT lưu `localStorage`, `AuthProvider` (React Context).
   - Biểu đồ dashboard = **CSS bar chart** (div) → không thêm dependency, build nhẹ.
7. **Trang cần auth (user/admin) là Client Components** (vì token nằm ở `localStorage`, RSC không đọc được).
   Trang public (home, /cars, /cars/[id]) vẫn **RSC-first** như hiện tại.
8. **Definition-of-done:** `frontend` build xanh; backend `test` xanh (≥ 59 test cũ + test mới).

---

## 1. Hiện trạng (đã khảo sát)

| Hạng mục | Trạng thái |
| --- | --- |
| Backend controllers | **19** controller, **0** annotation method-security |
| Bảo mật | Chỉ `/api/admin/**` = ADMIN và `/api/auth/me` = authenticated; **còn lại `permitAll`** |
| Backend test | **59** `@Test` (10 class); RBAC chỉ test ở `AuthControllerMockMvcTests` (7 test bảo mật) |
| Reports hiện có | `top-selling-cars`, `inventory-by-brand`, `revenue-summary`, `orders-by-status` (chưa drilldown) |
| Frontend | 38 file; chỉ có **home / cars / cars/[id] / 404**; **không có** auth, cart, order, admin, dashboard |
| Frontend data | `src/lib/api/*` chỉ đọc catalog; `http.ts` chưa gắn `Authorization` |
| Tài khoản seed | `admin/admin` (ADMIN), `user/user` (USER), `employee/employee` (EMPLOYEE) |
| Bảng DB (đối chiếu SQL drilldown) | `cars` (cột năm = **`model_year`**), `car_models`, `brands`, `customers`, `employees`, `sales_orders`, `order_items`, `users` |

---

## 2. BACKEND — Ma trận phân quyền (RBAC) chốt

Thứ tự matcher trong `SecurityConfig` **quan trọng** (first-match-wins). Ký hiệu:
🟢 public · 🔵 authenticated (mọi role) · 🟠 EMPLOYEE|ADMIN (staff) · 🔴 ADMIN.

| # | Method + path | Quyền | Ghi chú |
| --- | --- | --- | --- |
| 1 | `POST /api/auth/login`, `/register`, `/logout` | 🟢 | Giữ nguyên |
| 2 | `GET /api/auth/me` | 🔵 | Giữ nguyên |
| 3 | `GET /api/cars/**` | 🟢 | Xem xe |
| 4 | `POST/PUT/DELETE /api/cars/**` | 🔴 | CRUD xe = admin |
| 5 | `GET /api/brands/**` | 🟢 | |
| 6 | `POST/PUT/DELETE /api/brands/**` | 🔴 | CRUD hãng = admin |
| 7 | `GET /api/car-models/**` | 🟢 | |
| 8 | `POST/PUT/DELETE /api/car-models/**` | 🔴 | CRUD dòng xe = admin |
| 9 | `GET /api/car-media/**`, `GET /api/car-specs/**` | 🟢 | Xem media/spec |
| 10 | `PUT/DELETE /api/car-media/**`, `PUT/DELETE /api/car-specs/**` | 🟠 | Quản lý nội dung = staff |
| 11 | `GET /api/car-reviews/**` | 🟢 | Xem đánh giá |
| 12 | `POST /api/car-reviews/**` (tạo + reply) | 🔵 | User gửi đánh giá |
| 13 | `PUT/DELETE /api/car-reviews/**` | 🟠 | Kiểm duyệt = staff |
| 14 | `POST /api/customers` | 🔵 | **Tự tạo hồ sơ khách** (checkout/lái thử/đánh giá) |
| 15 | `GET/PUT/DELETE /api/customers/**` | 🟠 | Quản lý khách = staff |
| 16 | `POST /api/orders` | 🔵 | User đặt hàng |
| 17 | `GET/PATCH/PUT/DELETE /api/orders/**` (gồm items) | 🟠 | Xử lý đơn = staff |
| 18 | `POST /api/test-drives` | 🔵 | User đăng ký lái thử |
| 19 | `GET/PATCH/PUT/DELETE /api/test-drives/**` | 🟠 | Xử lý lịch = staff |
| 20 | `/api/payments/**`, `/api/contracts/**`, `/api/invoices/**` | 🟠 | Nghiệp vụ = staff |
| 21 | `GET /api/employees/**` | 🟠 | Xem nhân viên = staff |
| 22 | `POST/PUT/DELETE /api/employees/**` | 🔴 | CRUD nhân viên = admin |
| 23 | `/api/cart/**` | 🔵 | Giỏ cấu hình (Redis) |
| 24 | `GET /api/activity-logs/**`, `POST /api/activity-logs` | 🟠 | Log = staff |
| 25 | `/api/reports/**` | 🔴 | **Chuyển từ public → admin** (dashboard) |
| 26 | `/api/admin/**` | 🔴 | Giữ nguyên |
| 27 | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | 🟢 | Giữ nguyên |
| 28 | `anyRequest()` | 🔵 | **Đổi từ `permitAll` → `authenticated`** (an toàn mặc định) |

> Lý do cho phép 🔵 ở #14/#16/#18: entity `User` (auth) và `Customer` (nghiệp vụ) **không có liên kết**,
> nên để user tự tạo `Customer` rồi dùng `customerId` cho đơn/lái thử/đánh giá. Không thêm FK để tránh
> đổi schema/DB.

### 2.1. File backend sửa/ thêm

| File | Thay đổi |
| --- | --- |
| `config/SecurityConfig.java` | **Sửa**: thay khối `authorizeHttpRequests` bằng ma trận §2; thêm `import org.springframework.http.HttpMethod;` |
| `controller/ReportController.java` | **Sửa**: thêm 6 endpoint drilldown/dashboard (§3) |
| `service/ReportService.java` | **Sửa**: khai báo 6 method mới |
| `service/Impl/ReportServiceImpl.java` | **Sửa**: cài đặt 6 method mới + validate (status enum, id tồn tại) |
| `repository/JdbcReportRepository.java` | **Sửa**: thêm 6 truy vấn SQL (§3.3) |
| `dto/InventoryByModel.java` | **Thêm** record |
| `dto/CarInventoryItem.java` | **Thêm** record |
| `dto/OrderSummary.java` | **Thêm** record |
| `dto/CarSalesOrderLine.java` | **Thêm** record |
| `dto/RevenueByMonth.java` | **Thêm** record |
| `dto/DashboardSummary.java` | **Thêm** record |
| `controller/AdminController.java` | **Sửa** (tùy chọn): trả `DashboardSummary` + thông tin user thay vì chuỗi, hoặc giữ nguyên + thêm `/api/admin/me`. Giữ `/api/admin/dashboard` cũ để không phá test → **thêm mới** `/api/admin/profile` trả `UserResponse`. |

### 2.2. Test backend sửa/ thêm

| File | Thay đổi |
| --- | --- |
| `test/.../controller/AbstractAdminMockMvcTest.java` | **Thêm**: base class `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`; `@BeforeEach` seed `admin/admin` (ADMIN) + login lấy `adminToken`; helper `asAdmin(RequestBuilder)`. |
| `BrandControllerMockMvcTests.java` | **Sửa**: kế thừa base; bỏ `@Autowired MockMvc` trùng; gắn `asAdmin(...)` cho PUT/DELETE. |
| `CarModelControllerMockMvcTests.java` | **Sửa**: tương tự. |
| `CarControllerMockMvcTests.java` | **Sửa**: giữ GET public; gắn `asAdmin(...)` cho POST/PUT/DELETE. |
| `CustomerControllerMockMvcTests.java` | **Sửa**: gắn `asAdmin(...)` cho PUT/DELETE; (POST nếu có). |
| `test/.../controller/SecurityAuthorizationTests.java` | **Thêm**: test RBAC — 401 khi thiếu token, 403 khi USER gọi admin write, 200 khi ADMIN; EMPLOYEE được staff endpoint; `/api/reports/**` chặn USER. |
| `test/.../controller/ReportControllerMockMvcTests.java` | **Thêm**: test 6 endpoint mới (drilldown + summary) với token admin. |
| `test/.../repository/ReportRepositoryDrilldownTests.java` | **Thêm** (tùy chọn): test SQL thuần trên H2 với dữ liệu seed. |

---

## 3. BACKEND — API Dashboard & Drilldown (mới)

Tiền tố `/api/reports` (ADMIN). Drilldown dạng "click card → chi tiết".

| # | Method + path | Trả về | Drilldown từ |
| --- | --- | --- | --- |
| D1 | `GET /api/reports/dashboard-summary` | `DashboardSummary` | KPI tổng |
| D2 | `GET /api/reports/inventory-by-brand/{brandId}/models` | `List<InventoryByModel>` | card `inventory-by-brand` |
| D3 | `GET /api/reports/inventory-by-model/{modelId}/cars` | `List<CarInventoryItem>` | màn hình D2 |
| D4 | `GET /api/reports/top-selling-cars/{carId}/orders` | `List<CarSalesOrderLine>` | card `top-selling-cars` |
| D5 | `GET /api/reports/orders-by-status/{status}` | `List<OrderSummary>` | card `orders-by-status` |
| D6 | `GET /api/reports/revenue-by-month` | `List<RevenueByMonth>` | KPI doanh thu |

### 3.1. DTO mới (records)

```java
public record DashboardSummary(
    long totalCars, long availableCars, long reservedCars, long soldCars, long maintenanceCars,
    long totalCustomers, long totalEmployees, long totalOrders, long pendingTestDrives,
    double totalRevenue) {}

public record InventoryByModel(Long modelId, String modelName, String bodyType,
    long totalCars, long totalQuantity) {}

public record CarInventoryItem(Long carId, String carName, String vin, String color,
    Integer year, String status, Integer quantity, Double price) {}

public record OrderSummary(Long orderId, String orderCode, LocalDateTime orderDate,
    String status, Double totalAmount, String customerName, String employeeName) {}

public record CarSalesOrderLine(Long orderId, String orderCode, LocalDateTime orderDate,
    String status, String customerName, Integer quantity, Double unitPrice, Double lineTotal) {}

public record RevenueByMonth(String period, long totalOrders, double totalRevenue) {}
```

### 3.2. Service mới

- `DashboardSummary getDashboardSummary()`
- `List<InventoryByModel> getModelsByBrand(Long brandId)` — `brandId` không tồn tại → `ResourceNotFoundException`.
- `List<CarInventoryItem> getCarsByModel(Long modelId)` — `modelId` không tồn tại → 404.
- `List<CarSalesOrderLine> getOrdersByCar(Long carId)` — `carId` không tồn tại → 404.
- `List<OrderSummary> getOrdersByStatus(String status)` — `status` sai → `BadRequestException`.
- `List<RevenueByMonth> getRevenueByMonth()`

### 3.3. SQL mới (JdbcTemplate — chạy cả MySQL & H2)

Lưu ý cột: `cars.model_year` (không phải `year`), `sales_orders.order_code/order_date/total_amount`,
`order_items.unit_price`, `customers.full_name`, `employees.full_name`.

```sql
-- D2: models của 1 brand
SELECT m.id AS model_id, m.name AS model_name, m.body_type,
       COUNT(c.id) AS total_cars, COALESCE(SUM(c.quantity), 0) AS total_quantity
FROM car_models m LEFT JOIN cars c ON c.model_id = m.id
WHERE m.brand_id = ? GROUP BY m.id, m.name, m.body_type ORDER BY m.id;

-- D3: cars của 1 model
SELECT c.id, c.name, c.vin, c.color, c.model_year, c.status, c.quantity, c.price
FROM cars c WHERE c.model_id = ? ORDER BY c.id;

-- D4: các dòng đơn chứa 1 car (top-selling drilldown)
SELECT o.id, o.order_code, o.order_date, o.status, cu.full_name AS customer_name,
       oi.quantity, oi.unit_price, (oi.quantity * oi.unit_price) AS line_total
FROM order_items oi
JOIN sales_orders o ON o.id = oi.order_id
LEFT JOIN customers cu ON cu.id = o.customer_id
WHERE oi.car_id = ? ORDER BY o.order_date DESC, o.id DESC;

-- D5: đơn theo status
SELECT o.id, o.order_code, o.order_date, o.status, o.total_amount,
       cu.full_name AS customer_name, e.full_name AS employee_name
FROM sales_orders o
LEFT JOIN customers cu ON cu.id = o.customer_id
LEFT JOIN employees e ON e.id = o.employee_id
WHERE o.status = ? ORDER BY o.order_date DESC, o.id DESC;

-- D6: doanh thu theo tháng (EXTRACT + LPAD dùng chung được cho MySQL & H2)
SELECT CONCAT(CAST(EXTRACT(YEAR FROM order_date) AS INTEGER), '-',
              LPAD(CAST(EXTRACT(MONTH FROM order_date) AS VARCHAR), 2, '0')) AS period,
       COUNT(*) AS total_orders, COALESCE(SUM(total_amount), 0) AS total_revenue
FROM sales_orders WHERE status <> 'CANCELLED'
GROUP BY EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date)
ORDER BY EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date);

-- D1: từng chỉ số (queryForObject)
SELECT COUNT(*) FROM cars;
SELECT COUNT(*) FROM cars WHERE status = ?;                 -- AVAILABLE/RESERVED/SOLD/MAINTENANCE
SELECT COUNT(*) FROM customers;
SELECT COUNT(*) FROM employees;
SELECT COUNT(*) FROM sales_orders;
SELECT COUNT(*) FROM test_drive_appointments WHERE status = 'PENDING';
SELECT COALESCE(SUM(total_amount),0) FROM sales_orders WHERE status <> 'CANCELLED';
```

> Cần xác nhận tên bảng lịch lái thử khi code (dự kiến `test_drive_appointments`) — đọc `entity/TestDriveAppointment.java`.

---

## 4. FRONTEND — Kiến trúc & danh sách thay đổi

### 4.1. Lớp API / hạ tầng

| File | Loại | Nội dung |
| --- | --- | --- |
| `src/lib/api/http.ts` | Sửa | Thêm `apiRequest(path,{method,body,auth})`; base = `/api` khi chạy browser (qua rewrite), `NEXT_PUBLIC_API_BASE_URL` khi RSC; tự gắn `Authorization: Bearer` từ token store khi `auth:true`; parse `ApiError`. Giữ `fetchJson`/`fetchOptional` cho RSC. |
| `src/lib/auth/token.ts` | Thêm | `getToken/setToken/clearToken/getStoredUser/setStoredUser` (localStorage, SSR-safe). |
| `src/context/AuthContext.tsx` | Thêm | `'use client'`; `AuthProvider` + `useAuth()`; `login/register/logout`; khôi phục phiên bằng `/api/auth/me`. |
| `src/lib/api/auth.ts` | Thêm | `login`, `register`, `logout`, `me`. |
| `src/lib/api/cart.ts` | Thêm | `getCart`, `addItem`, `removeItem`, `clearCart` (sessionId). |
| `src/lib/api/customers.ts` | Thêm | `createCustomer`, `listCustomers`, `getCustomer`, `updateCustomer`, `deleteCustomer`, `searchCustomers`. |
| `src/lib/api/orders.ts` | Thêm | CRUD + `byCustomer`, `updateStatus`, `cancel`, items CRUD. |
| `src/lib/api/testDrives.ts` | Thêm | CRUD + `byCustomer`, `updateStatus`, `cancel`. |
| `src/lib/api/employees.ts` | Thêm | CRUD + search. |
| `src/lib/api/payments.ts`, `contracts.ts`, `invoices.ts` | Thêm | CRUD + byOrder. |
| `src/lib/api/activityLogs.ts` | Thêm | list, byEntity. |
| `src/lib/api/reports.ts` | Thêm | 4 report cũ + 6 API drilldown mới. |
| `src/lib/api/index.ts` | Sửa | export tất cả module mới. |
| `src/lib/auth/profile.ts` | Thêm | Quản lý `customerId` đã lưu (localStorage) cho checkout/lái thử/đánh giá. |

### 4.2. Layout & UI dùng chung

| File | Loại | Nội dung |
| --- | --- | --- |
| `src/app/layout.tsx` | Sửa | Bọc `<AuthProvider>`; thêm `<Toaster>` (tự viết) |
| `src/components/layout/Header.tsx` | Sửa | Auth-aware: Đăng nhập/Đăng ký hoặc menu user (Tài khoản, Đơn của tôi, Admin nếu ADMIN, Đăng xuất); link Giỏ hàng; badge số lượng |
| `src/components/layout/MobileNav.tsx` | Sửa | Triển khai thật (menu mobile) thay vì `return null` |
| `src/components/layout/AdminSidebar.tsx` | Thêm | Menu điều hướng admin |
| `src/components/ui/*` | Thêm | `Button`, `Input`, `Select`, `Textarea`, `Modal`, `ConfirmDialog`, `Table`, `Toast`, `BarChart`, `KpiCard`, `Tabs`, `Badge`, `Skeleton` |
| `src/components/admin/ResourceManager.tsx` | Thêm | **CRUD tổng quát** (config: endpoint + columns + form fields) → tái dùng cho mọi trang admin |
| `src/components/admin/ResourceForm.tsx` | Thêm | Form động theo field config |
| `src/components/car/CarGallery.tsx` | Thêm | Gallery tách khỏi page (client, chọn ảnh) |
| `src/components/car/ReviewSection.tsx` + `ReviewForm.tsx` | Thêm | Danh sách + gửi đánh giá/reply |
| `src/components/car/TestDriveForm.tsx` | Thêm | Form đặt lái thử |
| `src/components/car/AddToCartButton.tsx` | Thêm | Thêm vào giỏ |

### 4.3. Trang Public / User

| Route | File | Loại | Nội dung |
| --- | --- | --- | --- |
| `/` | `app/page.tsx` | RSC (sửa) | Bỏ `searchCars` cục bộ (dùng `lib/api`); giữ hero/featured |
| `/cars` | `app/cars/page.tsx` | RSC (sửa) | Bỏ import chết `getCarsByBrand/Model` |
| `/cars/[id]` | `app/cars/[id]/page.tsx` | RSC (sửa) | Dùng `<CarGallery>` + `<ReviewSection>` + nút **Thêm vào giỏ / Đặt mua / Lái thử** hoạt động |
| `/login` | `app/login/page.tsx` | Client | Form đăng nhập |
| `/register` | `app/register/page.tsx` | Client | Form đăng ký (mặc định role USER) |
| `/cart` | `app/cart/page.tsx` | Client | Giỏ, đổi số lượng, xóa; nút Thanh toán |
| `/checkout` | `app/checkout/page.tsx` | Client | Tạo/tái dùng hồ sơ khách → tạo đơn từ giỏ → xóa giỏ |
| `/account` | `app/account/page.tsx` | Client | Thông tin user; **Đơn của tôi**; **Lịch lái thử của tôi** |
| `not-found` / `loading` | (sửa/thêm) | RSC | Giữ + skeleton |

### 4.4. Trang Admin (đều là Client, guard role ADMIN)

| Route | File | Nội dung |
| --- | --- | --- |
| `/admin` | `app/admin/page.tsx` | **Dashboard**: KPI cards + bar chart theo trạng thái đơn + **drilldown** (click → bảng chi tiết qua D2–D6) |
| `/admin/cars` | `app/admin/cars/page.tsx` | CRUD xe (`ResourceManager`) |
| `/admin/brands` | `app/admin/brands/page.tsx` | CRUD hãng |
| `/admin/car-models` | `app/admin/car-models/page.tsx` | CRUD dòng xe |
| `/admin/customers` | `app/admin/customers/page.tsx` | CRUD khách |
| `/admin/employees` | `app/admin/employees/page.tsx` | CRUD nhân viên |
| `/admin/orders` | `app/admin/orders/page.tsx` | Danh sách + đổi trạng thái + hủy |
| `/admin/orders/[id]` | `app/admin/orders/[id]/page.tsx` | Chi tiết đơn + CRUD dòng xe |
| `/admin/payments` | `app/admin/payments/page.tsx` | CRUD thanh toán |
| `/admin/contracts` | `app/admin/contracts/page.tsx` | CRUD hợp đồng |
| `/admin/invoices` | `app/admin/invoices/page.tsx` | CRUD hóa đơn |
| `/admin/test-drives` | `app/admin/test-drives/page.tsx` | Danh sách + đổi trạng thái + hủy |
| `/admin/reviews` | `app/admin/reviews/page.tsx` | Duyệt/xóa đánh giá |
| `/admin/activity-logs` | `app/admin/activity-logs/page.tsx` | Xem log |
| `/admin/media` | `app/admin/media/page.tsx` | CRUD car-media |
| `/admin/specs` | `app/admin/specs/page.tsx` | CRUD car-specs |
| `app/admin/layout.tsx` | Thêm | Sidebar + guard ADMIN |

### 4.5. Tài liệu cập nhật

- `README.md`: thêm mục RBAC matrix + danh sách API dashboard drilldown + hướng dẫn tài khoản demo.
- `plan.md`: đánh dấu Buổi 4.
- `task4.md`: cập nhật trạng thái `[HOÀN THÀNH]` + ghi chú.

---

## 5. Thứ tự thực thi

1. **Backend RBAC**: sửa `SecurityConfig` → sửa 4 test class hiện có → thêm `SecurityAuthorizationTests` → `.\mvnw.cmd test` xanh.
2. **Backend dashboard**: DTO + repository SQL + service + controller → thêm `ReportControllerMockMvcTests` → test xanh.
3. **Frontend hạ tầng**: `http.ts`, token store, `AuthProvider`, `lib/api/*` mới, UI kit, `Toaster`.
4. **Frontend public/user**: auth pages, header, giỏ, checkout, account, nâng cấp car detail.
5. **Frontend admin**: `ResourceManager` + sidebar/layout + dashboard drilldown + các trang CRUD.
6. **Hoàn thiện**: `npm run build` xanh; cập nhật docs; smoke test thủ công (nếu chạy được MySQL/Mongo/Redis).

---

## 6. Acceptance criteria

- [ ] Backend: `.\mvnw.cmd test` xanh (59 test cũ đã cập nhật + test RBAC + test report mới).
- [ ] RBAC: thiếu token → 401; sai quyền → 403; đúng quyền → 2xx (có test chứng minh).
- [ ] 6 API dashboard/drilldown trả dữ liệu đúng, chạy trên H2 khi test.
- [ ] Frontend: `npm run build` xanh, không lỗi TypeScript/lint.
- [ ] Public: home, danh sách, chi tiết, tìm kiếm/lọc/phân trang hoạt động.
- [ ] User: đăng nhập/đăng ký/đăng xuất; thêm giỏ; thanh toán tạo đơn; đặt lái thử; gửi đánh giá; xem "Đơn của tôi".
- [ ] Admin: dashboard + drilldown; CRUD đủ các tài nguyên; đổi trạng thái đơn/lịch; duyệt/xóa đánh giá.
- [ ] UI tiếng Việt, luxury black/gold, responsive, Tailwind áp dụng đúng.

---

## 7. Rủi ro & lưu ý

- **Sửa test cũ**: bắt buộc, vì RBAC chặn write khi không auth. Dùng base class để tránh lặp.
- **`User` ↔ `Customer` không liên kết**: "Đơn của tôi" dựa vào `customerId` lưu ở client; không có ownership check server-side (chấp nhận cho đồ án; ghi chú Buổi 5 nếu cần).
- **SQL cross-DB**: tránh hàm riêng MySQL (dùng `EXTRACT` + `LPAD`); `LIMIT ?` đã dùng được ở H2.
- **JWT ở client**: lưu `localStorage` (đơn giản cho đồ án) — ghi chú bảo mật nâng cao (httpOnly cookie) cho tương lai.
- **Khối lượng frontend lớn**: dùng `ResourceManager` tổng quát để giảm code lặp cho ~15 trang admin.
- **Không có thư viện biểu đồ**: dùng CSS bar chart để tránh thêm dependency/nặng build.

---

## 8. Danh sách file tóm tắt

**Backend — sửa (7):** `config/SecurityConfig.java`, `controller/ReportController.java`, `controller/AdminController.java`,
`service/ReportService.java`, `service/Impl/ReportServiceImpl.java`, `repository/JdbcReportRepository.java`,
4 × `*ControllerMockMvcTests.java`.
**Backend — thêm (6 DTO + 3 test):** `dto/{DashboardSummary,InventoryByModel,CarInventoryItem,OrderSummary,CarSalesOrderLine,RevenueByMonth}.java`,
`test/.../AbstractAdminMockMvcTest.java`, `test/.../SecurityAuthorizationTests.java`, `test/.../ReportControllerMockMvcTests.java`.

**Frontend — sửa (~8):** `layout.tsx`, `Header.tsx`, `MobileNav.tsx`, `page.tsx`, `cars/page.tsx`, `cars/[id]/page.tsx`, `lib/api/http.ts`, `lib/api/index.ts`.
**Frontend — thêm (~45):** 8 file `lib/api/*` mới + `lib/auth/*` (2) + `context/AuthContext.tsx`,
~12 UI components + 2 admin components + 4 car components + `AdminSidebar`,
~10 trang public/user + `admin/layout.tsx` + ~16 trang admin.

---

## 9. Trạng thái triển khai — `[HOÀN THÀNH]`

### Backend
- ✅ `SecurityConfig` viết lại theo ma trận RBAC (§2), thứ tự first-match-wins.
- ✅ Thêm `AbstractAdminMockMvcTest` (seed `admin/admin`, helper `asAdmin(...)`), cập nhật 4 test class cũ.
- ✅ Thêm `SecurityAuthorizationTests` (16 test: 401/403/2xx, reports ADMIN-only, staff list, user tạo customer…).
- ✅ 6 DTO mới + 6 truy vấn SQL trong `JdbcReportRepository` + 6 method service + 6 endpoint `/api/reports/**`.
- ✅ Thêm `ReportControllerMockMvcTests` (9 test, chạy trên H2).
- ✅ **`.\mvnw.cmd test` → 84/84 PASS** (59 cũ + 16 RBAC + 9 dashboard).

### Frontend
- ✅ Hạ tầng: `http.ts` (JWT + base URL tương đối/ tuyệt đối), `lib/auth/token.ts`, `context/AuthContext.tsx`,
  `ToastProvider`, UI kit (`Button`, `Field`, `Modal`, `DataTable`, `BarChart`, `KpiCard`, `StatusPill`…).
- ✅ API modules: `auth`, `cart`, `customers`, `orders`, `testDrives`, `employees`, `payments`, `contracts`,
  `invoices`, `activityLogs`, `reports` + mở rộng CRUD cho catalog.
- ✅ Public/User: `/login`, `/register`, `/cart`, `/checkout`, `/account`; Header auth-aware + link giỏ;
  car detail có *Đặt mua ngay*, *Đăng ký lái thử*, *Viết đánh giá*.
- ✅ Admin: `admin/layout.tsx` (guard ADMIN) + `AdminSidebar` + `ResourceManager` (CRUD tổng quát) +
  dashboard **drilldown** + 15 trang tài nguyên.
- ✅ **`npm run build` → thành công**, 24 route (chỉ còn cảnh báo WASM SWC vô hại).

### Khác biệt so với kế hoạch ban đầu
- Gộp `ConfirmDialog`/`Tabs`/`Badge`/`Skeleton` vào các component hiện có (`Modal`, tab tự viết, `StatusPill`)
  thay vì tách file riêng — giảm số file.
- Trang `/admin/orders/[id]` (chi tiết đơn + CRUD dòng xe) **chưa tách riêng**; thao tác dòng xe thực hiện
  qua modal *Tạo đơn* và các thao tác trạng thái/hủy/xóa ở `/admin/orders`.
- `MobileNav.tsx` giữ nguyên `return null` (menu mobile đã gộp trong `Header`).
- Gallery ở car detail giữ nguyên bản RSC (không tách `CarGallery` client).

### Acceptance criteria
- [x] Backend: `.\mvnw.cmd test` xanh — **84 test PASS**.
- [x] RBAC: 401 khi thiếu token, 403 khi sai quyền, 2xx khi đúng quyền (có test).
- [x] 6 API dashboard/drilldown trả dữ liệu đúng, chạy trên H2 khi test.
- [x] Frontend: `npm run build` xanh, không lỗi TypeScript.
- [x] Public: home, danh sách, chi tiết, tìm kiếm/lọc/phân trang.
- [x] User: đăng nhập/đăng ký/đăng xuất; giỏ; thanh toán tạo đơn; đặt lái thử; gửi đánh giá; xem "Đơn của tôi".
- [x] Admin: dashboard + drilldown; CRUD tài nguyên; đổi trạng thái/hủy đơn & lịch; xóa đánh giá.
- [x] UI tiếng Việt, luxury black/gold, responsive.

### Sửa lỗi UI: FilterPanel & Header active — `[ĐÃ SỬA]`

- **`FilterPanel` hiển thị lỗi trên màn hình lớn:** panel nằm trong sidebar `lg:w-72` (288px) nhưng lại dùng
  `grid md:grid-cols-2 lg:grid-cols-4` → ở ≥1024px nó cố chia **4 cột trong 288px** nên input bị bóp méo/tràn.
  Tailwind breakpoint tính theo *viewport* chứ không theo container, nên đã viết lại thành **dòng dọc 1 cột**
  (`flex flex-col gap-5`) + khoảng giá Từ/Đến nằm cạnh nhau; sửa luôn mũi tên `select` bị lệch (dùng wrapper
  `relative` chỉ bao quanh select + `top-1/2 -translate-y-1/2`). Thêm `htmlFor/id`, trạng thái nút "Xóa lọc"
  (disabled khi không có lọc), `params.delete('page')` khi đổi lọc, và tự bỏ `model` khi đổi sang hãng khác.
- **Header chưa có hiệu ứng mục đang chọn:** đã dùng `usePathname()` (trước đó import nhưng không dùng) để tô
  **vàng + gạch chân** cho mục hiện tại trên nav desktop, **nền xám + chữ vàng** cho nav mobile và dropdown
  tài khoản, kèm `aria-current="page"`.
- Kiểm chứng: `npx tsc --noEmit` exit 0; `next build` **thành công** (24 route, chỉ còn cảnh báo WASM SWC vô hại)
  — build vào `NEXT_DIST_DIR=.next-verify` để không ảnh hưởng dev server đang chạy; các trang `/`, `/cars`,
  `/cars?brand=1&minPrice=500`, `/cart`, `/login` đều trả `200`; markup sidebar không còn `lg:grid-cols-4`.

### Sửa lỗi CORS khi test admin — `[ĐÃ KIỂM CHỨNG]`

Triệu chứng: ở trang admin, các API `cars` / `car-models` / `brands` báo *"blocked by CORS policy"* /
*"failed to fetch"* vì trình duyệt gọi thẳng `http://localhost:8080/api/...` từ origin `http://localhost:3000`.

Nguyên nhân: `lib/api/http.ts` có 2 helper — `apiRequest` (browser dùng `/api` tương đối → qua rewrite của Next,
không CORS) và `fetchJson` (chỉ dùng URL tuyệt đối, vốn dành cho RSC). Các hàm **đọc** trong
`cars.ts` / `carModels.ts` / `brands.ts` (và `carSpecs.getSpecByCar`, `carReviews.getReviewsByCar`) dùng
`fetchJson` nhưng lại được gọi từ **client component** của admin → trình duyệt fetch tuyệt đối → CORS.

Cách sửa (2 lớp):

1. **Frontend (chính):** `fetchJson` giờ dùng chung `baseUrl()` — ở browser trả về `/api` (đi qua rewrite,
   *same-origin*), ở server (RSC) trả về URL tuyệt đối `NEXT_PUBLIC_API_BASE_URL`.
2. **Backend (lưới an toàn):** thêm `WebCorsConfig` (bean `CorsConfigurationSource`, cấu hình
   `app.cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001,...`)
   và bật `.cors(Customizer.withDefaults())` trong `SecurityConfig`.

Kiểm chứng: preflight `OPTIONS /api/brands` từ origin `:3000` → `200` + `Access-Control-Allow-Origin`;
origin lạ → `403`; qua proxy Next `:3000` → `GET /api/{brands,cars,car-models}` = 9/23/22, `POST /api/brands`
bằng JWT ADMIN thành công; `npx tsc --noEmit` exit 0; `.\mvnw.cmd test` vẫn **84/84 PASS**.

### Chạy thực tế chỉ với MySQL (MongoDB/Redis chưa cài) — `[ĐÃ KIỂM CHỨNG]`

Môi trường hiện tại chỉ có MySQL 3306. Đã điều chỉnh cấu hình để app **khởi động và dùng được**:

| Thay đổi | File | Lý do |
| --- | --- | --- |
| `spring.cache.type=simple` | `application.properties` | Redis chưa cài → dùng cache in-memory |
| `@ConditionalOnProperty(spring.cache.type=redis)` | `CacheConfig` | Chỉ tạo `RedisCacheManager` khi thật sự bật Redis |
| `spring.data.mongodb.auto-index-creation=false` | `application.properties` | Không tạo index lúc khởi động (tránh bắt buộc kết nối Mongo) |
| Kiểm tra TCP trước khi seed NoSQL (≤1.5s) | `DataSeeder.seedMongoData` | Không treo ~30s chờ driver khi Mongo chưa chạy |

Kết quả kiểm chứng thực tế (`spring-boot:run`, chỉ MySQL):

- ✅ Khởi động **3.6s**, không lỗi; log `Đã seed SQL: 9 hãng, 22 dòng xe, 23 xe, 6 khách hàng, 4 nhân viên, 14 đơn hàng, 8 lịch lái thử`.
- ✅ NoSQL bỏ qua kèm cảnh báo `MongoDB không phản hồi tại localhost:27017` (không ảnh hưởng SQL).
- ✅ `GET /api/cars` → **23 xe**; `GET /api/cars/{id}`, tìm kiếm/lọc chạy bình thường (cache no-op).
- ✅ Đăng nhập `admin/admin` → JWT role `ADMIN`; thiếu token/sai token → **401**.
- ✅ CRUD admin thực tế: Brand + Car *Create → Update → Get → Delete → 404*, số xe trở lại 23.
- ✅ `GET /api/reports/dashboard-summary` → `cars=23, availableCars=18, reservedCars=3, soldCars=1, maintenanceCars=1, customers=6, employees=4, orders=14, scheduledTestDrives=3, revenue=31040.0`.
- ✅ `revenue-by-month` có 9 tháng (03 bị loại vì đơn `CANCELLED`), `inventory-by-brand` đủ 9 hãng.
- ✅ Khởi động lần 2 → `Bỏ qua seed SQL: dữ liệu đã tồn tại` (idempotent).
- ✅ `.\mvnw.cmd test` sau thay đổi → **84/84 PASS**.

> Chưa kiểm chứng trong môi trường thực tế: cart/session (cần Redis), reviews/media/specs/activity-logs (cần MongoDB),
> và smoke test UI trình duyệt — sẽ kiểm tra sau khi cài Redis/MongoDB.

