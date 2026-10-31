# Car Dealer Management System

Hệ thống quản lý hoạt động kinh doanh đại lý ô tô, gồm catalog xe, quản lý từng xe vật lý trong kho, khách hàng, đơn bán hàng, hợp đồng, hóa đơn, thanh toán, lịch lái thử, báo cáo quản trị và chat realtime.

Dự án được xây dựng theo mô hình **REST API + WebSocket**, với Spring Boot ở backend và Next.js ở frontend. Dữ liệu giao dịch được quản lý bằng MariaDB/MySQL; MongoDB lưu dữ liệu phi quan hệ; Redis phục vụ cache, session và giỏ cấu hình.

---

## Mục lục

- [1. Công nghệ](#1-công-nghệ)
- [2. Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
- [3. Mô hình dữ liệu cốt lõi](#3-mô-hình-dữ-liệu-cốt-lõi)
- [4. Vòng đời xe và nghiệp vụ bán hàng](#4-vòng-đời-xe-và-nghiệp-vụ-bán-hàng)
- [5. Dashboard và nguyên tắc báo cáo](#5-dashboard-và-nguyên-tắc-báo-cáo)
- [6. Các phân hệ chức năng](#6-các-phân-hệ-chức-năng)
- [7. REST API](#7-rest-api)
- [8. Chat realtime](#8-chat-realtime)
- [9. Thông báo và chứng từ](#9-thông-báo-và-chứng-từ)
- [10. Cài đặt và chạy dự án](#10-cài-đặt-và-chạy-dự-án)
- [11. Kiểm thử](#11-kiểm-thử)
- [12. Cấu trúc thư mục](#12-cấu-trúc-thư-mục)

---

## 1. Công nghệ

### Backend

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build tool | Maven Wrapper |
| REST API | Spring MVC |
| ORM / SQL | Spring Data JPA, Hibernate |
| Truy vấn báo cáo | JdbcTemplate |
| Cơ sở dữ liệu giao dịch | MariaDB / MySQL |
| Cơ sở dữ liệu NoSQL | MongoDB |
| Cache, session, cart | Redis |
| Xác thực | Spring Security, JWT |
| Realtime | WebSocket, STOMP, SockJS |
| API documentation | Springdoc OpenAPI, Swagger UI |
| Email | Spring Mail |
| PDF | OpenPDF |
| Excel | Apache POI |
| Kiểm thử | JUnit 5, Mockito, MockMvc, H2 |

### Frontend

| Thành phần | Công nghệ |
|---|---|
| Framework | Next.js 16 (App Router) |
| UI library | React 19 |
| Ngôn ngữ | TypeScript |
| CSS | Tailwind CSS v4 |
| REST client | Fetch API |
| Realtime client | STOMP.js, SockJS |
| Lint | ESLint |

---

## 2. Kiến trúc hệ thống

### 2.1. Sơ đồ tổng quan

```text
                  Browser
                     |
          +----------+-----------+
          |                      |
      Next.js UI             WebSocket
          |                   /ws
       REST /api                |
          |                     |
          +----------+----------+
                     |
              Spring Boot API
                     |
       +-------------+-------------+
       |             |             |
   Spring MVC    WebSocket      Security
   Controller    STOMP          JWT / RBAC
       |             |
       +------ Service Layer ------+
                     |
          +----------+----------+
          |          |          |
       JPA / SQL   MongoDB    Redis
          |          |          |
      MariaDB     Documents   Cache /
       MySQL                  Session /
                              Cart
                     |
               JdbcTemplate
                     |
             Dashboard / Reports
```

### 2.2. Phân chia dữ liệu

| Nhóm dữ liệu | Nơi lưu | Lý do |
|---|---|---|
| Hãng xe, dòng xe, phiên bản xe, xe vật lý | MariaDB / MySQL | Dữ liệu có quan hệ và ràng buộc nghiệp vụ |
| Khách hàng, nhân viên, tài khoản | MariaDB / MySQL | Dữ liệu giao dịch và phân quyền |
| Đơn hàng, dòng đơn, hợp đồng, hóa đơn, thanh toán | MariaDB / MySQL | Cần transaction và tính nhất quán |
| Lịch lái thử | MariaDB / MySQL | Có quan hệ với khách hàng, xe và nhân viên |
| Thông số kỹ thuật, đánh giá, media, activity log | MongoDB | Dữ liệu linh hoạt, cấu trúc có thể thay đổi |
| Cache catalog, session, giỏ cấu hình | Redis | Truy xuất nhanh và dữ liệu có vòng đời ngắn |
| Báo cáo tổng hợp | JdbcTemplate trên SQL | Truy vấn JOIN, GROUP BY và tổng hợp dữ liệu |

### 2.3. Luồng xử lý backend

Các nghiệp vụ chính được tổ chức theo luồng:

```text
Controller → Service → Repository → Database
                  |
             Business rules
             Validation
             Transaction
             State transition
             Domain events
```

- **Controller** tiếp nhận HTTP request, kiểm tra đầu vào ở tầng API và trả response.
- **Service** xử lý quy tắc nghiệp vụ, điều phối repository và transaction.
- **Repository** thao tác dữ liệu bằng Spring Data JPA hoặc JdbcTemplate.
- **State service / state machine** kiểm soát chuyển trạng thái hợp lệ.
- **Event listener** xử lý thông báo sau khi transaction nghiệp vụ commit.

---

## 3. Mô hình dữ liệu cốt lõi

### 3.1. Phân biệt dòng xe và xe vật lý

Hệ thống tách ba cấp dữ liệu:

```text
Brand
  └── CarModel
        └── Car
              └── CarItem
```

| Entity | Ý nghĩa | Ví dụ dữ liệu |
|---|---|---|
| `Brand` | Hãng sản xuất | Toyota, BMW, Ford |
| `CarModel` | Dòng xe thuộc hãng | Camry, Corolla Cross |
| `Car` | Sản phẩm / phiên bản thuộc dòng xe | Camry 2.0Q |
| `CarItem` | Một chiếc xe vật lý cụ thể | VIN, màu, năm sản xuất, tình trạng, giá, trạng thái |

**Điểm quan trọng:** `Car` không đại diện cho một chiếc xe đang nằm trong kho. `CarItem` mới là đơn vị tồn kho thực tế và là đối tượng được đặt trước, bán hoặc đưa đi bảo dưỡng.

### 3.2. Đơn hàng và dòng đơn hàng

- `SalesOrder` là phần đầu đơn hàng: mã đơn, khách hàng, nhân viên phụ trách, ngày tạo, trạng thái và tổng tiền.
- `OrderItem` là từng xe được đưa vào đơn.
- Mỗi `OrderItem` tham chiếu đến một `CarItem` cụ thể.
- Mỗi dòng đơn tương ứng một chiếc xe, do đó `quantity = 1`.
- `unitPrice` lưu giá tại thời điểm đưa xe vào đơn, không phụ thuộc vào thay đổi giá catalog sau này.

### 3.3. Nhóm dữ liệu tài chính

```text
SalesOrder
   |
   +---- OrderItem
   |
   +---- Contract
   |
   +---- Invoice
             |
             +---- Payment
             +---- Payment
             +---- Payment ...
```

- Một đơn hàng có thể có hợp đồng và hóa đơn tương ứng.
- Một hóa đơn có thể được thanh toán qua nhiều lần thu tiền.
- Mỗi `Payment` lưu một giao dịch thu tiền đã được xác nhận.
- `Invoice` theo dõi tổng tiền, số tiền đã thu, số tiền còn lại và trạng thái thanh toán.

---

## 4. Vòng đời xe và nghiệp vụ bán hàng

Đây là quy tắc nghiệp vụ quan trọng nhất của phân hệ kho và bán xe. Trạng thái của một chiếc xe vật lý (`CarItem`) độc lập với trạng thái đơn hàng (`SalesOrder`).

### 4.1. Vòng đời của một chiếc xe

| Trạng thái | Ý nghĩa |
|---|---|
| `AVAILABLE` | Xe sẵn sàng để bán hoặc được chọn vào đơn hàng |
| `RESERVED` | Xe đã được giữ cho một đơn hàng, không còn khả dụng cho đơn khác |
| `SOLD` | Xe đã được bán thông qua quy trình lập hợp đồng |
| `MAINTENANCE` | Xe đang trong trạng thái bảo dưỡng / không sẵn sàng bán |

Luồng bán hàng thông thường:

```text
                  +----------------+
                  |   AVAILABLE    |
                  +----------------+
                          |
                    Tạo đơn hàng
                    và giữ xe
                          |
                          v
                  +----------------+
                  |    RESERVED    |
                  +----------------+
                          |
                 Đơn hàng CONFIRMED
                          |
                    Lập hợp đồng
                    thành công
                          |
                          v
                  +----------------+
                  |      SOLD      |
                  +----------------+
```

Nhánh bảo dưỡng được quản lý riêng: xe có thể chuyển giữa `AVAILABLE` và `MAINTENANCE` theo thao tác quản lý trạng thái xe. Không được tùy ý chuyển xe đang `RESERVED` hoặc `SOLD` sang trạng thái khác bằng thao tác cập nhật trạng thái thông thường.

### 4.2. Vòng đời đơn hàng

Các trạng thái đơn hàng:

- `PENDING`: đơn vừa được tạo.
- `CONFIRMED`: đơn đã được xác nhận.
- `COMPLETED`: đơn đã hoàn tất điều kiện hợp đồng và thanh toán.
- `CANCELLED`: đơn đã bị hủy theo điều kiện nghiệp vụ.

Luồng tổng quát:

```text
             PENDING
                |
             CONFIRMED
                |
       +--------+---------+
       |                  |
   Hợp đồng            Hóa đơn
       |                  |
   Xe thành SOLD      Thu tiền nhiều lần
                          |
                   Đã thu đủ hóa đơn
                          |
                          v
                      COMPLETED

PENDING / CONFIRMED ───────► CANCELLED
       (chỉ khi thỏa điều kiện hủy)
```

Việc chuyển trạng thái đơn hàng đi qua `OrderStateService` và state machine, đồng thời ghi nhận lịch sử chuyển trạng thái.

### 4.3. Quy tắc tạo và chỉnh sửa đơn

1. Khi tạo đơn, hệ thống kiểm tra các xe được chọn còn `AVAILABLE`.
2. Các xe được giữ cho đơn chuyển sang `RESERVED` trong transaction tạo đơn.
3. Đơn mới bắt đầu ở trạng thái `PENDING`.
4. Khi xác nhận đơn, xe vẫn ở `RESERVED`; xác nhận đơn không đồng nghĩa với đã bán xe.
5. Nếu chỉnh sửa đơn `PENDING` khi chưa phát sinh giao dịch tài chính, các xe cũ được giải phóng về `AVAILABLE`, sau đó hệ thống giữ các xe thay thế.
6. Khi hủy đơn đủ điều kiện, các xe đang được giữ cho đơn được trả về `AVAILABLE`.
7. Không cho phép hủy đơn đã phát sinh hợp đồng, hóa đơn hoặc thanh toán theo luồng hủy thông thường.

### 4.4. Quy tắc lập hợp đồng, hóa đơn và thu tiền

**Lập hợp đồng**
- Chỉ lập hợp đồng cho đơn `CONFIRMED`.
- Đơn phải có ít nhất một xe.
- Tất cả xe trong đơn phải vẫn ở trạng thái `RESERVED`.
- Khi hợp đồng được tạo thành công, các xe trong đơn chuyển sang `SOLD`.

**Lập hóa đơn và thu tiền**
- Hóa đơn ghi nhận tổng giá trị cần thu và theo dõi công nợ.
- Có thể ghi nhận nhiều lần thanh toán cho cùng một hóa đơn.
- Chỉ ghi nhận số tiền đã thực sự nhận và xác minh.
- Không cho phép thu vượt số công nợ còn lại.
- Không cho phép ghi nhận thanh toán nếu đơn chưa `CONFIRMED`, chưa có hợp đồng hoặc chưa có hóa đơn.
- Đơn chỉ chuyển sang `COMPLETED` khi hợp đồng đã tồn tại và hóa đơn đã được thanh toán đủ. Điều kiện này được kiểm tra dù hợp đồng hay lần thanh toán cuối cùng được tạo trước.

**Lưu ý:** Trạng thái `SOLD` được xác lập tại thời điểm lập hợp đồng thành công; không chờ đến khi khách thanh toán đủ. Trạng thái `COMPLETED` phản ánh việc hoàn tất nghĩa vụ hợp đồng và thanh toán.

---

## 5. Dashboard và nguyên tắc báo cáo

Dashboard không chỉ hiển thị số lượng bản ghi. Các chỉ số được tổng hợp theo ý nghĩa nghiệp vụ của xe, đơn hàng và dòng tiền.

### 5.1. Báo cáo tồn kho

Tồn kho được đếm theo số bản ghi `CarItem` — tức số chiếc xe vật lý, không phải số dòng xe `Car`.

| Chỉ số | Cách hiểu |
|---|---|
| Tổng xe trong kho (`IN_STOCK`) | Xe `AVAILABLE` + xe `MAINTENANCE` |
| `AVAILABLE` | Xe sẵn sàng bán |
| `RESERVED` | Xe đang được giữ trong đơn hàng |
| `SOLD` | Xe đã được bán |
| `MAINTENANCE` | Xe đang bảo dưỡng |
| Xe mới / xe đã qua sử dụng | Phân loại theo `CarItem.condition` |

Dashboard hỗ trợ drilldown theo cấu trúc:

```text
Hãng xe (Brand)
   └── Dòng xe (CarModel)
         └── Phiên bản (Car)
               └── Xe vật lý (CarItem)
```

Người quản trị có thể đi từ số liệu tổng hợp đến danh sách xe cụ thể.

### 5.2. Báo cáo doanh thu và công nợ

Hệ thống phân biệt ba khái niệm:

| Nhóm | Ý nghĩa |
|---|---|
| Doanh thu chờ ghi nhận (`PENDING`) | Giá trị đơn chưa đủ điều kiện ghi nhận doanh thu |
| Doanh thu ghi nhận (`RECOGNIZED`) | Giá trị đơn `CONFIRMED` / `COMPLETED` đã có cả hợp đồng và hóa đơn |
| Thực thu (`ACTUAL`) | Tổng các khoản thanh toán có trạng thái `PAID` hoặc `PARTIAL` thuộc nhóm đơn đủ điều kiện ghi nhận |

Điều kiện ghi nhận doanh thu:
- Đơn ở trạng thái `CONFIRMED` hoặc `COMPLETED`.
- Đơn đã có hợp đồng.
- Đơn đã có hóa đơn.

Đơn `PENDING`, hoặc đơn `CONFIRMED` nhưng chưa có đủ hợp đồng và hóa đơn, được xếp vào nhóm doanh thu chờ ghi nhận. Đơn `CANCELLED` không được tính vào tổng doanh thu.

### 5.3. Ngày ghi nhận doanh thu

Báo cáo tháng quy doanh thu và thực thu về **kỳ phát sinh của đơn hàng**, không gom theo ngày nhân viên thực hiện thao tác thu tiền:

- Đơn `PENDING`: dùng `order_date`.
- Đơn `CONFIRMED` / `COMPLETED`: dùng `confirmed_at`; nếu chưa có thì fallback về `order_date`.
- Các khoản thanh toán của đơn đủ điều kiện được phân bổ về kỳ doanh thu của chính đơn đó, không dùng `paid_at` làm kỳ doanh thu.

Nhờ đó, khi xem một tháng, phần chênh lệch giữa doanh thu ghi nhận và thực thu phản ánh công nợ còn tồn của các đơn thuộc kỳ đó, thay vì bị dịch chuyển sang tháng khách thực hiện lần thanh toán tiếp theo.

Khi lọc theo hãng hoặc dòng xe, số tiền thực thu được phân bổ theo tỷ trọng giá trị các dòng `OrderItem` được chọn trong đơn; đây là phân bổ báo cáo, không phải quan hệ thanh toán trực tiếp tới từng chiếc xe.

### 5.4. Bộ lọc và drilldown

Dashboard hỗ trợ lọc theo:
- Khoảng ngày.
- Hãng xe.
- Dòng xe.

Các vùng dữ liệu có thể drilldown gồm:
- Trạng thái đơn hàng → danh sách đơn tương ứng.
- Hãng xe → dòng xe → xe vật lý.
- Chỉ số bán xe → các đơn hàng chứa xe.
- Nhóm doanh thu → danh sách đơn cấu thành chỉ số.

---

## 6. Các phân hệ chức năng

| Phân hệ | Chức năng chính |
|---|---|
| Catalog | Quản lý hãng, dòng xe, phiên bản xe |
| Kho xe | Quản lý từng chiếc xe, VIN, màu, năm, tình trạng, giá và trạng thái |
| Khách hàng | Hồ sơ khách hàng, tìm kiếm và liên kết tài khoản |
| Nhân viên | Quản lý nhân viên và thông tin phụ trách |
| Tài khoản | Đăng ký, đăng nhập, JWT, phân quyền, khóa/mở tài khoản |
| Đơn hàng | Tạo đơn, quản lý dòng xe, xác nhận, hủy và lịch sử trạng thái |
| Hợp đồng | Lập và quản lý hợp đồng bán xe |
| Hóa đơn | Quản lý tổng tiền, đã thu, còn nợ và trạng thái thanh toán |
| Thanh toán | Ghi nhận nhiều lần thu tiền cho một đơn |
| Lái thử | Đặt lịch, xác nhận, hoàn thành hoặc hủy lịch |
| Đánh giá | Đánh giá xe và phản hồi |
| Thông số kỹ thuật | Lưu thông số linh hoạt theo xe |
| Media | Quản lý hình ảnh và phương tiện của xe |
| Activity log | Theo dõi hoạt động hệ thống |
| Thông báo | Thông báo trong ứng dụng và email |
| Chat | Trao đổi realtime qua WebSocket |
| Dashboard | KPI, biểu đồ, lọc dữ liệu, drilldown |
| Xuất báo cáo | Xuất CSV và Excel |
| Chứng từ | Xuất PDF hợp đồng và hóa đơn |

---

## 7. REST API

Backend mặc định chạy tại `http://localhost:8080`.

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 7.1. Các nhóm endpoint

| Nhóm | Base endpoint |
|---|---|
| Xác thực | `/api/auth` |
| Quản trị tài khoản | `/api/admin/users` |
| Hãng xe | `/api/brands` |
| Dòng xe | `/api/car-models` |
| Phiên bản xe | `/api/cars` |
| Xe vật lý | `/api/car-items` |
| Khách hàng | `/api/customers` |
| Nhân viên | `/api/employees` |
| Đơn hàng | `/api/orders` |
| Hợp đồng | `/api/contracts` |
| Hóa đơn | `/api/invoices` |
| Thanh toán | `/api/payments` |
| Lịch lái thử | `/api/test-drives` |
| Thông báo | `/api/notifications` |
| Thông số kỹ thuật | `/api/car-specs` |
| Đánh giá | `/api/car-reviews` |
| Media | `/api/car-media` |
| Activity log | `/api/activity-logs` |
| Giỏ cấu hình | `/api/cart` |
| Session | `/api/sessions` |
| Báo cáo | `/api/reports` |

### 7.2. Endpoint báo cáo và dashboard

Một số endpoint chính:

| Endpoint | Mục đích |
|---|---|
| `GET /api/reports/dashboard-summary` | Tổng hợp KPI dashboard |
| `GET /api/reports/dashboard` | Dashboard theo bộ lọc ngày / hãng / dòng xe |
| `GET /api/reports/revenue-by-month` | Báo cáo doanh thu theo tháng |
| `GET /api/reports/revenue-orders` | Drilldown đơn theo nhóm doanh thu |
| `GET /api/reports/inventory-by-brand` | Tồn kho theo hãng |
| `GET /api/reports/inventory-by-brand/{brandId}/models` | Dòng xe theo hãng |
| `GET /api/reports/inventory-by-model/{modelId}/cars` | Xe vật lý theo dòng xe |
| `GET /api/reports/top-selling-cars` | Xe bán chạy |
| `GET /api/reports/top-selling-cars/{carId}/orders` | Đơn hàng chứa xe |
| `GET /api/reports/orders-by-status/{status}` | Đơn theo trạng thái |
| `GET /api/reports/export` | Xuất báo cáo CSV / XLSX |

Tham số lọc chi tiết và response schema được mô tả trong Swagger UI.

### 7.3. Xác thực và phân quyền

API sử dụng JWT Bearer:

```http
Authorization: Bearer <access_token>
```

Các vai trò chính:
- `USER`: tài khoản khách hàng.
- `EMPLOYEE`: nhân viên xử lý nghiệp vụ.
- `ADMIN`: quản trị hệ thống.

Quyền truy cập được cấu hình tập trung trong Spring Security. Các API quản trị, báo cáo và thay đổi catalog được giới hạn theo vai trò. API hồ sơ cá nhân và dữ liệu khách hàng cần được xử lý theo tài khoản đang đăng nhập.

---

## 8. Chat realtime

Chat sử dụng WebSocket với STOMP và SockJS.

| Thành phần | Địa chỉ / prefix |
|---|---|
| STOMP endpoint | `/ws` |
| Client gửi message | `/app` |
| Public topic broker | `/topic` |
| Private queue broker | `/queue` |
| User destination | `/user` |

JWT được xác thực tại bước STOMP CONNECT thông qua inbound channel interceptor. Broker hiện tại là **simple broker chạy trong bộ nhớ**, phù hợp môi trường phát triển và quy mô triển khai nhỏ; khi cần scale nhiều backend instance nên cân nhắc external message broker.

Frontend sử dụng `@stomp/stompjs` và `sockjs-client` để duy trì kết nối, subscribe và gửi nhận message.

---

## 9. Thông báo và chứng từ

### 9.1. Thông báo

Các sự kiện nghiệp vụ như tạo đơn, thay đổi trạng thái, thanh toán, lập hợp đồng / hóa đơn và lịch lái thử có thể phát sinh thông báo.

Event được phát trong transaction; listener xử lý theo cơ chế **AFTER_COMMIT** và **@Async**, tránh gửi thông báo trước khi dữ liệu nghiệp vụ được commit thành công.

Email được cấu hình qua biến môi trường. Không đưa Gmail App Password hoặc secret vào source code.

### 9.2. PDF

- `GET /api/contracts/{id}/pdf`: xuất PDF hợp đồng.
- `GET /api/invoices/{id}/pdf`: xuất PDF hóa đơn.

PDF sử dụng font Unicode để hiển thị tiếng Việt.

### 9.3. CSV / Excel

Endpoint xuất báo cáo:

```http
GET /api/reports/export?type=<report-type>&format=xlsx
GET /api/reports/export?type=<report-type>&format=csv
```

Các nhóm báo cáo được hỗ trợ được mô tả trong Swagger. Excel được tạo bằng Apache POI; CSV sử dụng UTF-8 BOM để tương thích tốt hơn với Excel.

---

## 10. Cài đặt và chạy dự án

### 10.1. Yêu cầu

- Java JDK 21.
- Git.
- Node.js và npm tương thích với Next.js 16.
- Maven Wrapper có sẵn trong thư mục backend.
- MariaDB / MySQL.
- MongoDB và Redis nếu sử dụng các phân hệ tương ứng.

### 10.2. Khởi động database bằng DDEV

Repository có cấu hình DDEV cho MariaDB, MongoDB và Redis.

```bash
# Tại thư mục gốc repository
ddev start
ddev describe
```

Các dịch vụ được cấu hình để backend chạy trên host có thể kết nối qua các port mặc định:
- MariaDB: `127.0.0.1:3306`
- MongoDB: `127.0.0.1:27017`
- Redis: `127.0.0.1:6379`

Nếu thay đổi port hoặc cấu hình dịch vụ, kiểm tra lại `.ddev/config.yaml` và các file compose trong `.ddev/`.

### 10.3. Cấu hình backend

Kiểm tra cấu hình tại:

```text
backend/src/main/resources/application.properties
backend/src/main/resources/application-test.properties
```

Các thông tin kết nối database, JWT secret, email và các thông số môi trường nên được cung cấp qua environment variables hoặc cấu hình riêng của môi trường triển khai. Không commit secret thực tế lên repository.

Chạy backend trên Linux / macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend mặc định: [http://localhost:8080](http://localhost:8080).

### 10.4. Chạy frontend

Mở terminal khác:

```bash
cd frontend
npm install
npm run dev
```

Frontend mặc định: [http://localhost:3000](http://localhost:3000).

Các lệnh thường dùng:

```bash
npm run dev
npm run lint
npm run build
npm run start
```

Frontend gọi API qua đường dẫn tương đối `/api` ở browser và sử dụng rewrite của Next.js để chuyển request đến backend. Với Server Components, cấu hình `NEXT_PUBLIC_API_BASE_URL` để trỏ tới backend phù hợp với môi trường chạy.

### 10.5. Chạy ứng dụng khi thiếu MongoDB hoặc Redis

Các phân hệ SQL cốt lõi có thể được phát triển và kiểm thử độc lập với MongoDB / Redis nếu cấu hình các dịch vụ tương ứng phù hợp.

- Redis được dùng cho cache, session và cart.
- MongoDB được dùng cho thông số kỹ thuật, đánh giá, media và activity log.
- Khi tắt hoặc không kết nối các dịch vụ này, các API phụ thuộc trực tiếp vào chúng sẽ không hoạt động đầy đủ.
- Không nên xem chế độ thiếu dịch vụ là cấu hình production.

### 10.6. Seed dữ liệu

Dữ liệu mẫu được cấu hình qua `app.seed.enabled` và lớp seeder trong backend. Kiểm tra cấu hình môi trường trước khi bật seed trên database có dữ liệu thật. Nếu cần làm mới dữ liệu mẫu, hãy sao lưu trước và chỉ xóa dữ liệu trong môi trường phát triển.

### 10.7. Sao lưu dữ liệu DDEV

Repository có các lệnh hỗ trợ backup / restore trong `.ddev/commands/host/`. Có thể kiểm tra hướng dẫn trực tiếp từ script tương ứng và sử dụng:

```bash
ddev db-backup
ddev db-restore <backup-file>
```

Không xóa Docker volume hoặc chạy lệnh xóa database khi chưa có bản sao lưu cần thiết.

---

## 11. Kiểm thử

Backend sử dụng Maven và JUnit 5. Một số nhóm kiểm thử có sử dụng Mockito, MockMvc và H2 in-memory.

Chạy toàn bộ backend tests:

Linux / macOS:

```bash
cd backend
./mvnw test
```

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

Kiểm tra frontend:

```bash
cd frontend
npm run lint
npm run build
```

Các lệnh trên là hướng dẫn chạy kiểm thử; kết quả cần được xác nhận trên commit và môi trường hiện tại trước khi phát hành.

---

## 12. Cấu trúc thư mục

```text
JavaSpringbootCarDealer/
├── backend/
│   ├── src/main/java/com/example/lab5/
│   │   ├── config/          # Security, Redis, WebSocket, async, seed
│   │   ├── controller/      # REST controllers
│   │   ├── document/        # MongoDB documents
│   │   ├── dto/             # Request / response DTO
│   │   ├── entity/          # JPA entities
│   │   ├── event/           # Business events và listeners
│   │   ├── exception/       # Exception handling
│   │   ├── mapper/
│   │   ├── mongorepository/ # MongoDB repositories
│   │   ├── repository/      # JPA và JdbcTemplate repositories
│   │   ├── service/         # Service interfaces
│   │   │   └── Impl/        # Service implementations
│   │   ├── statemachine/    # State machine
│   │   └── util/
│   └── src/test/
├── frontend/
│   └── src/
│       ├── app/             # App Router pages
│       ├── components/      # UI, admin, car, layout
│       ├── context/         # React contexts
│       ├── lib/             # API clients, auth, utilities
│       └── types/
├── .ddev/                   # Cấu hình database và tiện ích DDEV
├── plan.md
├── task*.md
├── report.md
└── README.md
```

---

## Ghi chú nghiệp vụ quan trọng

1. `Car` là phiên bản / sản phẩm; `CarItem` là chiếc xe vật lý.
2. Tồn kho phải đếm theo `CarItem`, không đếm theo `Car`.
3. Tạo đơn giữ xe ở `RESERVED`; không đánh dấu `SOLD` ngay khi tạo đơn.
4. Lập hợp đồng thành công mới chuyển xe trong đơn sang `SOLD`.
5. Một hóa đơn có thể được thanh toán nhiều lần; không được thu vượt công nợ.
6. Đơn chỉ `COMPLETED` khi có hợp đồng và hóa đơn đã thanh toán đủ.
7. Doanh thu tháng và thực thu được quy về kỳ phát sinh của đơn hàng; không lấy tháng `paid_at` làm kỳ doanh thu.
8. Dashboard cần phân biệt xe sẵn sàng bán, xe đang giữ, xe đã bán và xe bảo dưỡng.

