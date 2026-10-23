# Audit nghiệp vụ Backend --- JavaSpringbootCarDealer

-   **Repository:**
    https://github.com/launnguyen001/JavaSpringbootCarDealer
-   **Nhánh được rà soát:** `main`
-   **Ngày rà soát:** 2026-10-17
-   **Phạm vi:** các service Java có thể đọc trực tiếp từ source hiện
    tại; ưu tiên tính đúng đắn nghiệp vụ, trạng thái đơn hàng, tồn kho,
    thanh toán, lịch lái thử, cache và dữ liệu liên hệ giữa SQL/MongoDB.
-   **Cách đọc kết quả:** đây là code review tĩnh. Chưa chạy ứng dụng,
    chưa chạy test mới và chưa kiểm tra dữ liệu thực tế trong DB. "Bug
    xác nhận từ code" nghĩa là có đường thực thi rõ ràng theo code đã
    đọc; "nguy cơ/cần xác minh" nghĩa là còn phụ thuộc quy ước nghiệp
    vụ, controller, constraint hoặc cấu hình khác.

## Tóm tắt điều hành

Các vấn đề nên ưu tiên xử lý:

1.  **P1 --- Trạng thái đơn hàng có thể bị đổi trực tiếp mà không đồng
    bộ tồn kho.** `updateStatus()` cho phép đặt trạng thái tùy ý; đặt
    `CANCELLED` qua đường này không gọi `restoreStock()`.
2.  **P1 --- Có thể hủy đơn đã hoàn tất/thanh toán.** `cancel()` chỉ
    chặn đơn đã `CANCELLED`, không chặn `COMPLETED`; nó trả xe về
    `AVAILABLE` nhưng không xử lý hoàn tiền/hủy chứng từ.
3.  **P1 --- Đánh dấu đơn `COMPLETED` chỉ dựa vào một payment có trạng
    thái `PAID`, không kiểm tra tổng tiền đã thu.** Có thể hoàn tất đơn
    dù payment nhỏ hơn tổng đơn; cũng có thể thu vượt tổng đơn.
4.  **P1 --- Có thể bán trùng một chiếc xe khi có hai request đồng
    thời.** Kiểm tra `AVAILABLE` rồi đổi sang `SOLD` không dùng
    khóa/atomic conditional update; transaction đơn thuần chưa đủ chống
    race condition.
5.  **P1 --- `CarItemServiceImpl` cho phép sửa trạng thái xe tùy ý**, kể
    cả chuyển `SOLD` về `AVAILABLE`, có thể phá vỡ liên kết giữa đơn
    hàng và tồn kho.
6.  **P1/P2 --- Lịch lái thử không kiểm tra trạng thái có thể lái và
    xung đột lịch.** Có thể tạo lịch cho xe `SOLD`/`MAINTENANCE`; chưa
    thấy kiểm tra trùng giờ theo xe/nhân viên/khách.
7.  **P2 --- Cache catalog không được xóa khi trạng thái từng chiếc xe
    đổi thông qua nghiệp vụ đơn hàng.** Cache chi tiết có `items[]`; sau
    bán/hủy đơn có thể trả dữ liệu trạng thái cũ cho đến khi TTL hết
    hạn.
8.  **P2 --- `getOrderOptions()` cộng toàn bộ payment mà không lọc trạng
    thái.** Giá trị "đã thanh toán" có thể tính cả `UNPAID`, `PARTIAL`
    hoặc `REFUNDED`.
9.  **P2 --- Hóa đơn có thể ghi tổng tiền khác tổng đơn**, do
    `InvoiceServiceImpl` nhận `request.total()` độc lập với
    `order.totalAmount`.
10. **P2 --- Service MongoDB không kiểm tra xe tồn tại trong SQL trước
    khi lưu thông số/media/review.** Có nguy cơ tạo tài liệu mồ côi nếu
    `carId` không hợp lệ hoặc xe đã bị xóa.

------------------------------------------------------------------------

## Danh sách vấn đề

### BUG-01 --- Đổi trạng thái đơn hàng bỏ qua quy trình nghiệp vụ

-   **Mức độ:** P1 --- Cao
-   **File:**
    [`SalesOrderServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/SalesOrderServiceImpl.java),
    phương thức `updateStatus()`
-   **Bằng chứng:** phương thức lấy đơn, gọi `order.setStatus(status)`
    và lưu; không kiểm tra trạng thái cũ/mới, không gọi
    `restoreStock()`, không xác minh payment.
-   **Kịch bản:** đơn đang `CONFIRMED` có một xe đã được đánh dấu
    `SOLD`. Admin gọi `PATCH /api/orders/{id}/status` để đặt
    `CANCELLED`. Đơn thành `CANCELLED` nhưng chiếc xe vẫn `SOLD`.
-   **Hệ quả:** báo cáo trạng thái đơn không khớp tồn kho; xe có thể bị
    kẹt không bán được.
-   **Khuyến nghị:** định nghĩa state transition hợp lệ; không cho đặt
    `CANCELLED` qua `updateStatus()` nếu không dùng chung quy trình hủy;
    mọi chuyển trạng thái phải gọi domain method tương ứng. Cần test
    từng cặp trạng thái hợp lệ/không hợp lệ.

### BUG-02 --- Có thể hủy đơn đã `COMPLETED` và trả xe về kho

-   **Mức độ:** P1 --- Cao
-   **File:** `SalesOrderServiceImpl.java`, `cancel()`
-   **Bằng chứng:** chỉ từ chối khi trạng thái hiện tại đã `CANCELLED`;
    sau đó gọi `restoreStock(order)` rồi đặt `CANCELLED`. Không có nhánh
    từ chối `COMPLETED`, không hoàn/đối soát payment, invoice hay
    contract.
-   **Kịch bản:** đơn đã thanh toán và hoàn tất; người có quyền gọi
    endpoint hủy. Xe chuyển về `AVAILABLE` nhưng payment vẫn tồn tại và
    chứng từ vẫn tồn tại.
-   **Hệ quả:** tồn kho có thể bán lại chiếc xe đã bán; số liệu doanh
    thu và chứng từ không nhất quán.
-   **Khuyến nghị:** chỉ cho hủy ở trạng thái cho phép theo quy định (ví
    dụ `PENDING`/`CONFIRMED`). Với đơn hoàn tất, cần quy trình hủy giao
    dịch/hoàn tiền riêng, có audit trail và cập nhật tồn kho có kiểm
    soát.

### BUG-03 --- Một payment `PAID` làm đơn hoàn tất mà không đối chiếu số tiền

-   **Mức độ:** P1 --- Cao
-   **File:**
    [`PaymentServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/PaymentServiceImpl.java),
    `create()` và `update()`
-   **Bằng chứng:** khi payment có status `PAID`, code lập tức đặt order
    thành `COMPLETED`; không so sánh tổng các payment hợp lệ với
    `order.totalAmount`.
-   **Kịch bản:** đơn 1.000.000.000 đồng, tạo payment `PAID` 1.000 đồng;
    đơn vẫn thành `COMPLETED`. Có thể tạo payment tiếp theo khiến tổng
    tiền thu vượt tổng đơn.
-   **Hệ quả:** sai trạng thái hoàn tất và doanh thu; dễ tạo chênh lệch
    kế toán.
-   **Khuyến nghị:** tính tổng tiền đã thu theo các trạng thái được xem
    là tiền thực thu; chỉ hoàn tất khi số tiền hợp lệ đạt tổng phải thu
    (có quy tắc rõ cho đặt cọc/thanh toán từng phần). Dùng `BigDecimal`
    thay cho `double` cho tiền.

### BUG-04 --- Sửa hoặc xóa payment không đồng bộ trạng thái đơn

-   **Mức độ:** P1/P2 --- Cao nếu dữ liệu thanh toán dùng cho báo cáo/kế
    toán
-   **File:** `PaymentServiceImpl.java`, `update()` và `delete()`
-   **Bằng chứng:** `update()` chỉ đặt order `COMPLETED` khi status mới
    là `PAID` và `paidAt == null`; không tính lại trạng thái khi payment
    bị đổi từ `PAID` sang `PARTIAL`/`REFUNDED`/`UNPAID`. `delete()` xóa
    payment nhưng không tính lại trạng thái order.
-   **Kịch bản:** payment `PAID` đã làm đơn `COMPLETED`; sau đó payment
    bị xóa hoặc đổi thành `REFUNDED`. Đơn vẫn `COMPLETED`.
-   **Khuyến nghị:** không sửa/xóa payment đã ghi nhận như một bản ghi
    thông thường; ưu tiên tạo bản ghi hoàn tiền/điều chỉnh. Nếu cho phép
    chỉnh sửa, phải đối soát toàn bộ payment và tính lại trạng thái đơn
    trong cùng transaction.

### BUG-05 --- "Số tiền đã thanh toán" cộng cả payment chưa trả hoặc đã hoàn

-   **Mức độ:** P2 --- Trung bình/Cao
-   **File:** `SalesOrderServiceImpl.java`, `getOrderOptions()`
-   **Bằng chứng:** vòng lặp `paymentRepository.findAll()` cộng
    `payment.getAmount()` vào `paidByOrder` mà không kiểm tra
    `payment.getStatus()`.
-   **Kịch bản:** một đơn có payment `PAID` 100, `UNPAID` 200 và
    `REFUNDED` 50; giá trị `paid` trả ra là 350 thay vì số tiền thực thu
    ròng theo quy tắc kế toán.
-   **Hệ quả:** dropdown/chọn đơn cho thanh toán, hợp đồng hoặc hóa đơn
    có thể hiển thị số còn phải thu sai.
-   **Khuyến nghị:** định nghĩa rõ status nào được tính vào `paid`; xử
    lý hoàn tiền bằng quy tắc riêng thay vì cộng mọi amount.

### BUG-06 --- Race condition khi bán xe

-   **Mức độ:** P1 --- Cao trong môi trường nhiều request đồng thời
-   **File:** `SalesOrderServiceImpl.java`, `findAvailableItem()` và
    `markItemSold()`
-   **Bằng chứng:** code đọc trạng thái, kiểm tra `AVAILABLE`, rồi mới
    đặt `SOLD`; không thấy pessimistic lock, optimistic version check
    hoặc câu lệnh cập nhật có điều kiện kiểu
    `UPDATE ... WHERE status='AVAILABLE'`.
-   **Kịch bản:** hai request đồng thời đều đọc cùng chiếc xe là
    `AVAILABLE`, cả hai cùng đi qua kiểm tra trước khi transaction còn
    lại commit.
-   **Hệ quả:** cùng một `car_item_id` có thể xuất hiện ở hai đơn hàng
    nếu DB không có unique constraint phù hợp.
-   **Khuyến nghị:** khóa bản ghi khi đọc (`PESSIMISTIC_WRITE`) hoặc cập
    nhật trạng thái atomically và kiểm tra số dòng bị ảnh hưởng; thêm
    unique constraint cho `order_items.car_item_id` nếu mô hình đảm bảo
    một xe chỉ thuộc tối đa một order item lịch sử. Test tích hợp bằng
    hai transaction đồng thời.

### BUG-07 --- API cập nhật xe có thể tự đưa xe đã bán trở lại trạng thái bán được

-   **Mức độ:** P1 --- Cao
-   **File:**
    [`CarItemServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/CarItemServiceImpl.java),
    `update()` và `updateStatus()`
-   **Bằng chứng:** `update()` nhận `data.status` và gán thẳng nếu khác
    null; `updateStatus()` gán trực tiếp status mới. Không kiểm tra
    trạng thái hiện tại, payment/order hay quy trình hủy đơn.
-   **Kịch bản:** chiếc xe thuộc đơn `COMPLETED` đang `SOLD`; admin đổi
    status thành `AVAILABLE`. Xe lại xuất hiện trong danh sách có thể
    bán dù đơn cũ và chứng từ vẫn tồn tại.
-   **Khuyến nghị:** cấm sửa status của xe đang gắn với đơn chưa hủy/đã
    hoàn tất; trạng thái tồn kho chỉ được thay đổi qua service nghiệp vụ
    chuyên trách. Xây ma trận chuyển trạng thái hợp lệ và test từng
    trường hợp.

### BUG-08 --- Hủy đơn không làm mới cache catalog

-   **Mức độ:** P2 --- Trung bình
-   **File:** `SalesOrderServiceImpl.java` so với
    [`CarCatalogService.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/CarCatalogService.java)
-   **Bằng chứng:** `CarCatalogService.getCarDetail()` cache
    `CarResponse` với chi tiết và `items[]`; `evictCatalog()` xóa cache.
    Các thao tác thay trạng thái `CarItem` trong `SalesOrderServiceImpl`
    không gọi `evictCatalog()`.
-   **Kịch bản:** xem chi tiết khi xe `AVAILABLE`, cache response; tạo
    đơn làm xe `SOLD`; đọc lại chi tiết trước khi TTL hết hạn có thể trả
    trạng thái cũ.
-   **Khuyến nghị:** phát sự kiện/domain event sau khi trạng thái xe
    thay đổi và invalidate cache tương ứng; hoặc đưa thao tác đổi trạng
    thái vào một service tồn kho duy nhất có trách nhiệm xóa cache. Thêm
    integration test kiểm tra đọc lại sau bán/hủy.

### BUG-09 --- Tạo lịch lái thử không kiểm tra trạng thái xe và xung đột thời gian

-   **Mức độ:** P1/P2 --- Cao tùy quy trình đại lý
-   **File:**
    [`TestDriveAppointmentServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/TestDriveAppointmentServiceImpl.java),
    `create()` / `update()` / `findCarItem()`
-   **Bằng chứng:** `findCarItem()` chỉ tìm theo ID; không kiểm tra
    `CarStatus`. Không thấy kiểm tra lịch trùng theo xe, nhân viên hoặc
    khách hàng.
-   **Kịch bản:** tạo lịch cho xe `SOLD`/`MAINTENANCE`, hoặc đặt cùng
    chiếc xe cho hai khách ở cùng thời điểm.
-   **Hệ quả:** lịch không thực hiện được hoặc nhân viên/xe bị đặt
    trùng.
-   **Khuyến nghị:** chỉ cho đặt xe đủ điều kiện; kiểm tra overlap theo
    khoảng thời gian và trạng thái lịch chưa hủy. Xác định rõ độ dài một
    buổi lái thử và múi giờ dùng cho lịch.

### BUG-10 --- Cập nhật lịch lái thử bỏ qua `customerId` và không bảo vệ trạng thái cuối

-   **Mức độ:** P2 --- Trung bình
-   **File:** `TestDriveAppointmentServiceImpl.java`, `update()`
-   **Bằng chứng:** phương thức kiểm tra `scheduledAt` và `carItemId`,
    nhưng không cập nhật hoặc xác minh `request.customerId()`; cũng
    không chặn sửa lịch đã `CANCELLED`/đã hoàn tất.
-   **Kịch bản:** client gửi `customerId` mới nhưng API âm thầm giữ
    khách cũ; hoặc lịch đã hủy vẫn được đổi xe/giờ nhưng trạng thái vẫn
    `CANCELLED`.
-   **Khuyến nghị:** nếu khách là trường bất biến, loại bỏ `customerId`
    khỏi hợp đồng update và ghi rõ; nếu được phép đổi, validate ID và
    cập nhật có chủ đích. Chặn cập nhật lịch ở trạng thái cuối hoặc tạo
    thao tác mở lại riêng.

### BUG-11 --- Có thể tạo lịch lái thử với status do client tự chỉ định

-   **Mức độ:** P2 --- Trung bình
-   **File:** `TestDriveAppointmentServiceImpl.java`, `create()`
-   **Bằng chứng:** `request.status()` được dùng trực tiếp nếu khác
    null; chỉ mặc định `SCHEDULED` khi null.
-   **Kịch bản:** client gửi status `COMPLETED` hoặc `CANCELLED` ngay
    khi tạo lịch, bỏ qua quy trình lên lịch.
-   **Khuyến nghị:** tạo mới luôn bắt đầu ở `SCHEDULED`; status do hệ
    thống quản lý. Nếu admin cần tạo lịch ở trạng thái đặc biệt, tạo use
    case riêng với phân quyền và kiểm tra hợp lệ.

### BUG-12 --- Tổng tiền hóa đơn không ràng buộc với tổng tiền đơn hàng

-   **Mức độ:** P2 --- Trung bình/Cao
-   **File:**
    [`InvoiceServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/InvoiceServiceImpl.java),
    `create()` / `update()`
-   **Bằng chứng:** service kiểm tra `total` không null và không âm
    nhưng lưu `request.total()`; không so sánh với `order.totalAmount`
    và không kiểm tra trạng thái đơn.
-   **Kịch bản:** đơn có tổng 900 triệu nhưng invoice được tạo với 90
    triệu hoặc 1,2 tỷ.
-   **Khuyến nghị:** nếu hóa đơn bán hàng phải khớp đơn, tính tổng từ
    server-side order thay vì tin giá trị client; nếu cho phép điều
    chỉnh/thuế/phí, mô hình hóa từng khoản và quy tắc chênh lệch.

### BUG-13 --- Tạo hợp đồng/hóa đơn chưa kiểm tra trạng thái đơn hợp lệ

-   **Mức độ:** P2 --- Trung bình
-   **File:** `ContractServiceImpl.java` và `InvoiceServiceImpl.java`
-   **Bằng chứng:** cả hai kiểm tra order tồn tại và kiểm tra một chứng
    từ trên mỗi order, nhưng không kiểm tra order có bị `CANCELLED` hay
    đã đủ điều kiện phát hành.
-   **Kịch bản:** tạo hợp đồng/hóa đơn cho đơn đã hủy hoặc còn ở trạng
    thái chưa xác nhận.
-   **Khuyến nghị:** xác định state transition nghiệp vụ cho hợp đồng và
    hóa đơn; từ chối tạo chứng từ với trạng thái không hợp lệ, trừ khi
    có luồng ngoại lệ được định nghĩa rõ.

### BUG-14 --- Service MongoDB có thể lưu tài liệu tham chiếu đến xe không tồn tại

-   **Mức độ:** P2 --- Trung bình
-   **File:**
    [`CarSpecificationServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/CarSpecificationServiceImpl.java),
    [`CarMediaServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/CarMediaServiceImpl.java),
    [`CarReviewServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/CarReviewServiceImpl.java)
-   **Bằng chứng:** service kiểm tra `carId` không null (và rating trong
    review) nhưng không tra `CarRepository` để xác minh xe tồn tại.
-   **Kịch bản:** gửi `carId` không có trong SQL nhưng vẫn lưu được
    document MongoDB; khi xe bị xóa, document liên quan có thể còn lại.
-   **Khuyến nghị:** kiểm tra xe tồn tại trước khi ghi; thiết kế chiến
    lược xóa đồng bộ (xóa document, soft delete, hoặc chấp nhận document
    mồ côi và có job dọn dẹp). Vì SQL và MongoDB không chung
    transaction, cần xử lý lỗi từng phần.

### BUG-15 --- Tìm kiếm catalog dùng cache key chưa chuẩn hóa đầy đủ

-   **Mức độ:** P3 --- Thấp/Trung bình
-   **File:** `CarCatalogService.java`, `searchByName()`
-   **Bằng chứng:** cache key là `#name.toLowerCase()`; phần truy vấn
    dùng `name.trim()`. `null` có thể lỗi ngay ở biểu thức key trước khi
    validate; chuỗi có khoảng trắng đầu/cuối tạo key khác nhau.
-   **Kịch bản:** `"Toyota"` và `" Toyota "` truy vấn cùng nội dung
    logic nhưng dùng hai cache key; `name == null` có thể phát sinh lỗi
    không đúng thông điệp validation.
-   **Khuyến nghị:** chuẩn hóa input trước khi vào phương thức có
    `@Cacheable`, dùng `trim().toLowerCase(Locale.ROOT)` và xử lý null
    trước khi tạo key.

### BUG-16 --- Đăng ký tài khoản thiếu validation nghiệp vụ đầu vào

-   **Mức độ:** P2 --- Trung bình
-   **File:**
    [`AuthServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/AuthServiceImpl.java),
    `register()`
-   **Bằng chứng:** service kiểm tra trùng username/email nhưng không
    kiểm tra username/password null, rỗng, độ dài hoặc chính sách mật
    khẩu trước khi encode/lưu.
-   **Kịch bản:** gửi username/password rỗng hoặc null; kết quả có thể
    là lỗi kỹ thuật hoặc dữ liệu không đạt chính sách, thay vì lỗi
    validation nhất quán.
-   **Khuyến nghị:** validate tại DTO bằng Bean Validation và kiểm tra
    lại quy tắc nghiệp vụ ở service; chuẩn hóa username/email; dùng
    thông báo lỗi an toàn và status HTTP nhất quán.

### BUG-17 --- Cập nhật hồ sơ người dùng cho phép mật khẩu chỉ 4 ký tự

-   **Mức độ:** P2 về bảo mật
-   **File:**
    [`UserServiceImpl.java`](https://github.com/launnguyen001/JavaSpringbootCarDealer/blob/main/backend/src/main/java/com/example/lab5/service/Impl/UserServiceImpl.java),
    `updateMe()`
-   **Bằng chứng:** code chỉ từ chối mật khẩu mới ngắn hơn 4 ký tự.
-   **Hệ quả:** mật khẩu quá yếu có thể được chấp nhận dù tài khoản có
    quyền truy cập dữ liệu đơn hàng/hồ sơ.
-   **Khuyến nghị:** thống nhất chính sách mật khẩu giữa đăng ký và đổi
    mật khẩu; chọn độ dài tối thiểu phù hợp và bổ sung rate
    limiting/chống dò mật khẩu ở tầng xác thực.

### BUG-18 --- Validation giá xe chấp nhận một số giá trị `double` không hữu hạn

-   **Mức độ:** P3 --- Thấp
-   **File:** `CarServiceImpl.java`, `validateCar()`
-   **Bằng chứng:** chỉ kiểm tra `basePrice == null` hoặc `< 0`; `NaN`
    và `Infinity` không bị loại bởi phép so sánh `< 0`.
-   **Khuyến nghị:** thêm `Double.isFinite(basePrice)` nếu tiếp tục dùng
    `Double`; tốt hơn là dùng `BigDecimal` cho giá tiền.

### BUG-19 --- Kiểm tra trùng VIN chỉ ở tầng ứng dụng, có thể race condition

-   **Mức độ:** P2/P3 --- Trung bình
-   **File:** `CarItemServiceImpl.java`, `validate()`
-   **Bằng chứng:** gọi `findByVin()` rồi mới lưu; chưa thấy bảo đảm
    atomic trong chính service.
-   **Kịch bản:** hai request cùng kiểm tra một VIN chưa tồn tại rồi
    cùng lưu.
-   **Khuyến nghị:** tạo unique constraint/index ở DB (nếu VIN bắt buộc
    duy nhất), giữ validation ở service để trả thông báo thân thiện và
    bắt lỗi unique violation.

------------------------------------------------------------------------

## Các điểm cần quyết định theo quy tắc nghiệp vụ

Không tự động coi các mục này là bug nếu chưa chốt yêu cầu:

-   **Hủy đơn đã thanh toán:** nếu hệ thống hỗ trợ hủy sau thanh toán,
    cần mô hình hoàn tiền, chứng từ điều chỉnh và kiểm kê riêng; không
    nên chỉ đặt `CANCELLED`.
-   **Thanh toán nhiều đợt:** cần định nghĩa `PARTIAL`, `PAID`,
    `REFUNDED` và quy tắc chuyển order status. Không nên suy trạng thái
    đơn từ một payment đơn lẻ.
-   **Hợp đồng/hóa đơn:** có bắt buộc chỉ phát hành sau khi đơn xác nhận
    hoặc hoàn tất không? Invoice total có bao gồm thuế/phí/chiết khấu
    không?
-   **Lịch lái thử:** cần quy định thời lượng buổi lái thử, buffer giữa
    lịch, status nào giữ chỗ và có chặn bán xe đang có lịch hay không.
-   **Tài liệu MongoDB:** quyết định dùng hard delete, soft delete hay
    cho phép dữ liệu lịch sử tồn tại sau khi xóa dòng xe.

## Danh sách service đã đọc

-   `AuthServiceImpl`
-   `ActivityLogServiceImpl`
-   `BrandServiceImpl`
-   `CarCatalogService`
-   `CarItemServiceImpl`
-   `CarMediaServiceImpl`
-   `CarModelServiceImpl`
-   `CarReviewServiceImpl`
-   `CarServiceImpl`
-   `CarSpecificationServiceImpl`
-   `ContractServiceImpl`
-   `CustomerServiceImpl`
-   `EmployeeServiceImpl`
-   `InvoiceServiceImpl`
-   `PaymentServiceImpl`
-   `ReportServiceImpl`
-   `SalesOrderServiceImpl`
-   `TestDriveAppointmentServiceImpl`
-   `UserServiceImpl`

**Giới hạn phạm vi:** các file service liên quan giỏ hàng/session Redis
chưa được xác minh trong lần rà soát này do không truy cập được đường
dẫn source tương ứng. Chưa audit đầy đủ controller, repository/JDBC SQL,
entity constraints, frontend, cấu hình bảo mật, và test hiện có. Do đó
không nên hiểu đây là bảo đảm đã tìm hết mọi bug trong toàn bộ hệ thống.

## Thứ tự đề xuất xử lý

1.  Sửa state machine của `SalesOrder` và hợp nhất luồng hủy đơn
    (`BUG-01`, `BUG-02`).
2.  Sửa quy tắc đối soát payment, cập nhật/xóa payment và tổng tiền đã
    thu (`BUG-03`, `BUG-04`, `BUG-05`).
3.  Chống bán trùng xe đồng thời và chặn cập nhật trạng thái xe phá vỡ
    đơn hàng (`BUG-06`, `BUG-07`, `BUG-19`).
4.  Hoàn thiện lịch lái thử và đồng bộ cache (`BUG-08`, `BUG-09`,
    `BUG-10`, `BUG-11`).
5.  Ràng buộc hóa đơn/hợp đồng với trạng thái và số tiền của đơn
    (`BUG-12`, `BUG-13`).
6.  Hoàn thiện validation, dữ liệu MongoDB và các vấn đề chất lượng thấp
    hơn (`BUG-14` đến `BUG-18`).

## Checklist kiểm thử hồi quy tối thiểu

-   [ ] Không thể đổi `CONFIRMED -> CANCELLED` qua endpoint status mà
    không trả xe về kho.
-   [ ] Không thể hủy `COMPLETED` bằng luồng hủy đơn thông thường.
-   [ ] Payment `PAID` nhỏ hơn tổng đơn không tự làm đơn `COMPLETED`.
-   [ ] Xóa/hoàn payment không để order status và số tiền đã thu sai.
-   [ ] Hai request đồng thời đặt cùng `carItemId` chỉ có tối đa một
    request thành công.
-   [ ] Không thể đổi xe đã thuộc đơn hoàn tất từ `SOLD` sang
    `AVAILABLE` bằng CRUD xe kho.
-   [ ] Bán/hủy xe làm mới cache catalog ngay.
-   [ ] Không tạo lịch lái thử cho xe không đủ điều kiện hoặc trùng
    lịch.
-   [ ] Không thể phát hành invoice sai tổng tiền nếu nghiệp vụ yêu cầu
    khớp order.
-   [ ] Không lưu Mongo document với `carId` không tồn tại.
-   [ ] Đăng ký từ chối username/password rỗng hoặc không đạt chính
    sách.
-   [ ] VIN duy nhất được bảo đảm ở DB, không chỉ qua bước kiểm tra
    trước khi lưu.
