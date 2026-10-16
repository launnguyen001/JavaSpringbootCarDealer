# Task 6 — API "đơn hàng đủ điều kiện" + dropdown chọn đơn thay vì nhập id

> **Mục tiêu:** Bỏ hoàn toàn việc admin phải gõ `id` số khi thêm Hợp đồng / Hóa đơn / Thanh toán.
> Thay vào đó, form "Thêm mới" hiển thị **dropdown chọn đơn hàng** với nhãn dễ đọc
> (`ORD-2026-0009 — Trần Thị Bảy — 950.000.000 ₫`) và **chỉ liệt kê những đơn còn đủ điều kiện**
> cho từng loại chứng từ. Muốn vậy cần một **API mới ở backend** trả về danh sách đơn "chưa có ràng buộc".
>
> **Nguyên tắc làm việc:** *backend trước — API xong mới làm frontend*. Giữ backend **`./mvnw test` xanh**
> và frontend **`npm run build` xanh**.
>
> **Trạng thái: HOÀN TẤT** — API backend + dropdown 3 form + auto-fill tổng tiền hóa đơn + test đã xong.
> Bằng chứng: `./mvnw test` **118/118 PASS** (110 → 118) · `npx tsc --noEmit` sạch · `npx eslint` không phát sinh lỗi mới
> (vẫn 18 lỗi tồn đọng) · `npm run build` xanh · server :3000 restart phục vụ bản build mới.
> Kiểm thử thật qua API: `GET /api/orders/options?target=invoice` loại đúng các đơn đã có hóa đơn; target sai → 400; ẩn danh → 401.

---

## 1. Bối cảnh & vấn đề

Cả 3 bảng con (Hợp đồng, Hóa đơn, Thanh toán) đều trỏ FK `order_id` → bảng `sales_orders` (đơn hàng),
chứ **không** trỏ vào hóa đơn. Vì vậy "danh sách chưa có ràng buộc" chính là **danh sách đơn hàng**:

| Form thêm mới | Đơn hàng được phép chọn |
|---|---|
| Hóa đơn | Đơn **không bị hủy** và **chưa có hóa đơn** |
| Hợp đồng | Đơn **không bị hủy** và **chưa có hợp đồng** |
| Thanh toán | Đơn **không bị hủy** (kèm số tiền còn thiếu để admin cân nhắc) |

Trước đây form dùng `{ name: 'orderId', type: 'number' }` → bắt admin gõ id → không khả thi trên thực tế.

---

## 2. Backend (Spring Boot)

### 2.1. DTO mới — `dto/OrderOptionResponse.java`

Record trả về cho dropdown:

```
orderId · orderCode · orderDate · status · customerName
totalAmount · paidAmount · remainingAmount · hasContract · hasInvoice
```

- `remainingAmount = max(totalAmount − paidAmount, 0)`.
- `status` là enum `OrderStatus` (serialize thành chuỗi như các API khác).
- `hasContract` / `hasInvoice` cho biết đơn đã có chứng từ tương ứng chưa (UI có thể tận dụng sau này).

### 2.2. Repository

`SalesOrderRepository` — thêm 3 truy vấn JPQL (dùng `NOT EXISTS`, lọc `status <> CANCELLED`,
`JOIN FETCH o.customer` để lấy tên khách, sắp xếp mới nhất trước):

- `findOptionsForInvoice()` — đơn chưa có hóa đơn.
- `findOptionsForContract()` — đơn chưa có hợp đồng.
- `findActiveOrderOptions()` — mọi đơn không bị hủy.

`InvoiceRepository` / `ContractRepository` — thêm `findAllOrderIds()` (`SELECT i.order.id FROM Invoice i`)
để tra cứu nhanh đơn nào đã có chứng từ.

### 2.3. Service — `SalesOrderService.getOrderOptions(String target)`

- `target ∈ { invoice, contract, payment }` (chuẩn hóa chữ thường, trim).
- target không hợp lệ → `BadRequestException` (400) kèm thông báo tiếng Việt.
- Tính `paidAmount` bằng cách cộng dồn `paymentRepository.findAll()` theo `order_id` (dữ liệu nhỏ, tránh
  truy vấn aggregate phức tạp).
- Trả `List<OrderOptionResponse>` đã dựng sẵn trong transaction (tránh lazy-init ngoài session).

### 2.4. Controller — `SalesOrderController`

```
GET /api/orders/options?target=invoice|contract|payment
```

- Khớp literal `/options` nên **không xung đột** với `GET /{id}` (Spring ưu tiên path literal).
- Phân quyền kế thừa rule sẵn có của `/api/orders/**` → `EMPLOYEE` hoặc `ADMIN`.

### 2.5. Test

- `SalesOrderServiceTests`: thêm case cho từng target + target sai ném `BadRequestException`.
- MockMvc: `GET /api/orders/options` với ADMIN → 200; ẩn danh → 401/403.

---

## 3. Frontend

### 3.1. Types & API

- `types/order.ts`: thêm `OrderOptionTarget` và `OrderOption` (khớp DTO backend).
- `types/index.ts`: export 2 type mới.
- `lib/api/orders.ts`: `getOrderOptions(target)` → `GET /orders/options?target=…` (auth).

### 3.2. `ResourceManager.tsx` — hỗ trợ select nạp động

Mở rộng `FieldConfig`:

| Thuộc tính | Ý nghĩa |
|---|---|
| `optionsLoader?: () => Promise<FieldOption[]>` | Nạp lựa chọn bất đồng bộ khi mở form "Thêm mới". |
| `onPick?: (option) => Record<string, unknown> \| void` | Hook khi chọn 1 lựa chọn — dùng để auto-fill các ô khác. |
| `hideOnEdit?: boolean` | Ẩn ô khi **Sửa** (vì PUT của 3 API không cho đổi đơn hàng). |

- `FieldOption = { value, label, meta? }` (`meta` mang dữ liệu phụ như `totalAmount`).
- Nạp lựa chọn trong **click handler** (`openCreate`) → setState diễn ra trong sự kiện, không dính lint
  `react-hooks/set-state-in-effect`.
- Hiển thị "Đang tải…" khi đang nạp và "Không có lựa chọn phù hợp" khi danh sách rỗng.
- Ô `hideOnEdit` vẫn giữ giá trị trong form (từ `toForm`) nên payload gửi lên vẫn đủ `orderId`.

### 3.3. Ba trang admin

`admin/invoices`, `admin/contracts`, `admin/payments`: đổi ô `orderId` từ `type:'number'` sang
`type:'select'` + `optionsLoader` + `hideOnEdit`.

- Nhãn option: `Mã đơn — Khách hàng — Tổng tiền` (form Thanh toán thêm `· còn thiếu …`).
- `placeholder` gợi ý theo ngữ cảnh (ví dụ `— Chọn đơn chưa có hóa đơn —`).
- **Hóa đơn**: `onPick` tự điền `total` = `totalAmount` của đơn vừa chọn (vẫn sửa tay được).

---

## 4. Nghiệm thu

1. `./mvnw test` xanh (toàn bộ test cũ + test mới cho endpoint `options`).
2. `npx tsc --noEmit` sạch, `npm run build` xanh.
3. Trên UI: form "Thêm mới" của Hóa đơn / Hợp đồng / Thanh toán **không còn ô nhập id**;
   danh sách xổ xuống hiện đúng các đơn đủ điều kiện, không chọn được đơn đã có chứng từ.
4. Form Hóa đơn chọn đơn xong tự điền `Tổng tiền`.
5. Không đụng tới: form Tạo đơn hàng, sidebar, popup chi tiết đơn.
