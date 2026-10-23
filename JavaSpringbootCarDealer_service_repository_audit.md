# Rà soát kiến trúc Service & Repository — JavaSpringbootCarDealer

- **Repository:** https://github.com/launnguyen001/JavaSpringbootCarDealer
- **Nhánh:** `main`
- **Ngày rà soát:** 2026-10-18
- **Mục tiêu:** giải thích trách nhiệm của các service/repository, cách chúng phối hợp, và các vấn đề nghiệp vụ/kiến trúc cần xử lý.
- **Phương pháp:** code review tĩnh từ source có thể truy cập; không chạy ứng dụng, không chạy lại toàn bộ test, không kết nối DB. Vì vậy, các điểm phụ thuộc controller, entity constraint, cấu hình, frontend hoặc dữ liệu thực tế được đánh dấu là cần xác minh.

## 1. Tóm tắt điều hành

Kiến trúc hiện tại chia nghiệp vụ theo module tương đối rõ: xe/danh mục, từng chiếc xe tồn kho, đơn hàng, thanh toán, hợp đồng/hóa đơn, khách hàng/nhân viên, lịch lái thử, xác thực, báo cáo và các tài liệu linh hoạt trên MongoDB.

Các rủi ro đáng ưu tiên nhất không nằm ở việc thiếu CRUD, mà ở **tính nhất quán giữa các module**:

1. **Đơn hàng – tồn kho – thanh toán chưa được bảo vệ bằng một state machine thống nhất.** Các đường cập nhật trạng thái khác nhau có thể tạo ra kết quả khác nhau.
2. **Tính đúng đắn của thanh toán chưa được suy ra từ tổng số tiền thực thu.** Một payment `PAID` không tự động chứng minh toàn bộ đơn đã được thanh toán đủ.
3. **Tồn kho có nguy cơ bán trùng khi hai request chạy đồng thời.** Kiểm tra rồi cập nhật trong hai bước không tương đương với thao tác nguyên tử.
4. **Các chứng từ và lịch lái thử chưa có đủ quy tắc nghiệp vụ liên module.** Cần xác định rõ điều kiện tạo, cập nhật, hủy và mối quan hệ với trạng thái đơn/xe.
5. **Cache danh mục có thể không phản ánh ngay trạng thái xe sau thao tác ở module đơn hàng.**
6. **SQL và MongoDB cùng lưu dữ liệu gắn với xe nhưng không có foreign key xuyên database.** Service phải tự xác minh ID tồn tại và xử lý xóa/cập nhật liên quan.

## 2. Bản đồ kiến trúc

Luồng thường gặp:

`Client → Controller → Service → Repository → SQL/MongoDB`

- **Controller:** chuyển HTTP request thành lời gọi nghiệp vụ, xử lý tham số và định dạng HTTP response. Không nên tự chứa quy tắc nghiệp vụ cốt lõi.
- **Service:** điều phối use case, xác thực quy tắc nghiệp vụ, gọi nhiều repository/service, quản lý transaction và chuyển đổi entity/DTO khi phù hợp.
- **Repository:** truy vấn và lưu dữ liệu; nên tránh đặt quy tắc nghiệp vụ như “đơn chỉ được hoàn tất khi đã thanh toán đủ” ở đây.
- **Entity/Document:** mô hình dữ liệu lưu trữ; constraint ở DB là lớp bảo vệ cuối cùng cho uniqueness và quan hệ trong cùng database.
- **Cache:** lớp tăng tốc đọc; mọi luồng thay đổi dữ liệu liên quan phải xác định cache nào cần invalidation.

Không nên hiểu `CarCatalogService` là một “proxy” bắt buộc cho toàn bộ `CarService`. Nó là service phục vụ use case đọc/khám phá danh mục; `CarService` chịu trách nhiệm quản lý dữ liệu xe. Hai module có thể dùng chung dữ liệu nhưng có mục đích khác nhau.

## 3. Service inventory — trách nhiệm và điểm cần xem xét

### 3.1 `CarCatalogService`

**Mục đích:** phục vụ truy vấn danh mục xe cho client; trả dữ liệu đã tổng hợp/ánh xạ, có cache cho chi tiết và tìm kiếm.

**Giá trị kiến trúc:** tách read use case khỏi CRUD quản trị xe; gom dữ liệu cần hiển thị và giảm truy vấn lặp bằng cache.

**Điểm cần xem xét:**
- Cache search dùng key lowercase nhưng chưa chuẩn hóa khoảng trắng giống phần query; `" Civic"` và `"Civic"` có thể thành hai key dù query sau `trim()` tương đương.
- Nếu `name` null, biểu thức tạo cache key có thể lỗi trước khi logic method xử lý được.
- Khi `SalesOrderServiceImpl` thay đổi trạng thái `CarItem`, chưa thấy invalidation cache danh mục tương ứng; trạng thái trong detail có thể cũ cho đến khi TTL hết.
- Cần xác định chính xác dữ liệu nào được cache: dữ liệu xe mẫu (`Car`) hay từng xe thực tế (`CarItem`), vì tồn kho/trạng thái thay đổi thường xuyên hơn thông tin mẫu xe.

### 3.2 `CarServiceImpl`

**Mục đích:** quản lý thông tin xe/mẫu xe ở tầng SQL, xác thực dữ liệu và phối hợp với `CarItem` khi sửa/xóa.

**Điểm cần xem xét:**
- Kiểm tra giá `Double` cần loại `NaN`/`Infinity`, không chỉ kiểm tra âm/null.
- Mọi thay đổi ảnh hưởng danh mục phải invalidation cache.
- Xóa xe cần xét các liên kết tới `CarItem`, lịch lái thử và tài liệu MongoDB. Không nên xóa cascade dữ liệu lịch sử bán hàng một cách mù quáng.
- Cần phân biệt “mẫu xe” (`Car`) với “chiếc xe cụ thể” (`CarItem`) trong API và validation để tránh nhầm giá gốc với giá bán của từng VIN.

### 3.3 `CarItemServiceImpl`

**Mục đích:** quản lý từng chiếc xe cụ thể trong tồn kho, gồm VIN, giá và trạng thái.

**Điểm cần xem xét:**
- Cho phép cập nhật trạng thái tự do có thể tạo chuyển trạng thái bất hợp lệ, ví dụ `SOLD → AVAILABLE`.
- Kiểm tra VIN trùng trong service là chưa đủ để chống race condition; nên có unique constraint/index ở DB, đồng thời chuẩn hóa VIN nhất quán (trim/uppercase).
- Các thao tác đổi trạng thái phải được phối hợp với đơn hàng và lịch lái thử.
- Không cho xóa chiếc xe đã được bán hoặc gắn với đơn/chứng từ nếu việc xóa làm mất lịch sử kiểm toán; cân nhắc soft delete hoặc chặn xóa.
- Invalidation cache cần thực hiện cho mọi đường thay đổi trạng thái, không chỉ CRUD thông thường.

### 3.4 `SalesOrderServiceImpl`

**Mục đích:** tạo và quản lý đơn bán xe; chọn xe có sẵn, gắn khách hàng/nhân viên, cập nhật trạng thái và hoàn trả tồn kho khi hủy.

**Điểm cần xem xét — ưu tiên cao:**
- `updateStatus()` cho phép đặt trạng thái trực tiếp mà không đảm bảo các side effect tương ứng; đặt `CANCELLED` qua đường này không khôi phục tồn kho như `cancel()`.
- `cancel()` có thể hủy đơn `COMPLETED`; xe có thể được trả lại `AVAILABLE` mà không có quy trình hoàn tiền/hủy hoặc điều chỉnh hợp đồng, hóa đơn.
- Việc tìm xe `AVAILABLE` rồi đổi thành `SOLD` ở các bước riêng có thể bán trùng khi hai request đồng thời. Cần transaction + lock/conditional update nguyên tử và xử lý trường hợp không còn hàng.
- Cần định nghĩa state machine hợp lệ, ví dụ: `PENDING → ... → COMPLETED` hoặc `CANCELLED`; không cho nhảy tùy ý giữa trạng thái cuối và trạng thái trung gian.
- Khi trạng thái xe thay đổi, cần đồng bộ cache danh mục.
- Mọi nhánh lỗi giữa tạo order, giữ xe, ghi payment và ghi chứng từ phải rollback nhất quán; cần kiểm tra ranh giới `@Transactional` thực tế.

### 3.5 `PaymentServiceImpl`

**Mục đích:** tạo, cập nhật và quản lý các khoản thanh toán gắn với đơn.

**Điểm cần xem xét — ưu tiên cao:**
- Nếu một payment có trạng thái `PAID` khiến đơn thành `COMPLETED` mà không tính tổng tiền thực thu so với tổng tiền phải trả, đơn có thể hoàn tất khi mới trả thiếu.
- Cần quy định rõ ý nghĩa `UNPAID`, `PARTIAL`, `PAID`, `REFUNDED` và liệu một payment record có được đổi trạng thái sau khi đã ghi nhận hay không.
- Khi payment bị giảm trạng thái, xóa hoặc hoàn tiền, trạng thái đơn phải được tính lại hoặc có quy trình điều chỉnh rõ ràng.
- Phải ngăn tổng thu vượt quá tổng đơn, trừ khi nghiệp vụ có đặt cọc/thu thừa và xử lý hoàn trả riêng.
- Các phép tính tiền nên dùng `BigDecimal`/DECIMAL, không dùng số thực nhị phân.
- Cần ghi audit trail cho thao tác thay đổi thanh toán; việc sửa trực tiếp payment đã hoàn tất có thể làm mất dấu lịch sử.

### 3.6 `ContractServiceImpl`

**Mục đích:** tạo và quản lý hợp đồng gắn với đơn hàng.

**Điểm cần xem xét:**
- Cần xác định đơn ở trạng thái nào mới đủ điều kiện lập hợp đồng; loại trừ đơn đã hủy và xem xét trạng thái thanh toán/đặt cọc.
- Không nên cho phép hợp đồng trỏ tới order không còn hợp lệ chỉ vì order tồn tại.
- Cần unique constraint cho số hợp đồng và quan hệ một hợp đồng/đơn nếu đó là quy tắc nghiệp vụ.
- Nếu đơn bị hủy sau khi có hợp đồng, phải có quy trình hủy/điều chỉnh chứng từ thay vì chỉ đổi trạng thái order.

### 3.7 `InvoiceServiceImpl`

**Mục đích:** tạo và quản lý hóa đơn cho đơn bán xe.

**Điểm cần xem xét:**
- Cho phép tổng hóa đơn khác tổng đơn có thể tạo lệch số liệu kế toán, trừ khi có quy tắc thuế/chiết khấu được mô hình hóa rõ.
- Cần kiểm tra trạng thái order trước khi lập hóa đơn và xác định hóa đơn có thể sửa/xóa sau khi phát hành hay không.
- Số hóa đơn cần được bảo vệ bằng unique constraint ở DB.
- Cần quy trình void/cancel/credit note nếu hóa đơn đã phát hành rồi đơn bị hủy hoặc thanh toán được hoàn lại.
- Không nên coi hóa đơn là một bản sao tùy ý của order; cần định nghĩa snapshot dữ liệu và nguyên tắc không sửa lịch sử.

### 3.8 `TestDriveAppointmentServiceImpl`

**Mục đích:** quản lý lịch hẹn lái thử theo khách hàng, chiếc xe, nhân viên và thời gian.

**Điểm cần xem xét:**
- Tạo/sửa lịch cần kiểm tra xe có ở trạng thái cho phép lái thử không.
- Cần phát hiện lịch chồng lấn theo xe, nhân viên và có thể cả khách hàng; chỉ kiểm tra lịch tồn tại theo ID là chưa đủ.
- Update cần xác định khách hàng nào có quyền đổi lịch; bỏ qua `request.customerId()` có thể khiến API thể hiện hợp đồng khác với hành vi thực tế.
- Không nên cho sửa lịch đã hoàn tất/hủy nếu không có luồng mở lại được định nghĩa.
- Tạo lịch mới nên mặc định `SCHEDULED` ở server thay vì tin trạng thái do client gửi.
- Cần xác định cách xử lý khi chiếc xe được bán trong lúc vẫn còn lịch lái thử tương lai.

### 3.9 `AuthServiceImpl`

**Mục đích:** đăng ký/đăng nhập và xác thực thông tin tài khoản.

**Điểm cần xem xét:**
- Đăng ký cần kiểm tra username/password không rỗng, giới hạn độ dài và chính sách mật khẩu.
- Uniqueness username/email phải được đảm bảo ở DB để tránh hai request đồng thời vượt qua `exists...` rồi cùng tạo dữ liệu.
- Không lưu/log mật khẩu thô; mật khẩu phải được hash bằng thuật toán phù hợp.
- Cần thống nhất quy tắc chuẩn hóa email/username giữa đăng ký và đăng nhập.
- Cần xác minh quy trình liên kết `User` với `Customer` có bảo đảm một-một và rollback nếu tạo một trong hai bản ghi thất bại.

### 3.10 `UserServiceImpl`

**Mục đích:** quản lý tài khoản và hồ sơ người dùng hiện tại.

**Điểm cần xem xét:**
- Độ dài mật khẩu tối thiểu 4 ký tự là yếu; cần áp dụng chính sách thống nhất với đăng ký/đổi mật khẩu.
- Cần xác minh việc cập nhật hồ sơ chỉ sửa tài khoản đang đăng nhập, không cho client chỉ định user ID để sửa người khác.
- Khi đổi email/username, cần kiểm tra trùng và giữ chuẩn hóa nhất quán.
- Cần tránh trả hash mật khẩu hoặc thông tin nội bộ trong DTO response.

### 3.11 `CustomerServiceImpl`

**Mục đích:** CRUD hồ sơ khách hàng, tìm kiếm và liên kết khách với tài khoản đăng nhập.

**Điểm cần xem xét:**
- Email/phone cần được chuẩn hóa và có quy tắc uniqueness rõ ràng; `existsByEmail` chỉ là kiểm tra trước, không chống được race condition.
- Khi cập nhật email của khách gắn tài khoản, phải kiểm tra trùng với chính sách loại trừ bản ghi hiện tại.
- Xóa khách cần xét đơn hàng/lịch sử thanh toán; dữ liệu lịch sử thường nên được giữ lại hoặc soft delete.
- Cần bảo đảm `/me` chỉ trả hồ sơ gắn với principal hiện tại.

### 3.12 `EmployeeServiceImpl`

**Mục đích:** quản lý nhân viên và tìm kiếm theo tên.

**Điểm cần xem xét:**
- Không nên xóa nhân viên đang gắn với đơn hàng hoặc lịch lái thử nếu làm mất lịch sử; có thể dùng trạng thái hoạt động.
- Cần validation cho dữ liệu liên hệ và chính sách uniqueness nếu email/phone phải duy nhất.
- Khi nhân viên nghỉ việc, cần quy tắc cho lịch lái thử tương lai và đơn đang xử lý.

### 3.13 `BrandServiceImpl` và `CarModelServiceImpl`

**Mục đích:** quản lý hãng xe và mẫu xe; tạo cấu trúc phân loại `Brand → CarModel → Car`.

**Điểm cần xem xét:**
- Tên hãng/mẫu xe nên chuẩn hóa trước khi kiểm tra trùng (trim, case folding); kiểm tra ứng dụng cần đi kèm unique constraint phù hợp.
- Không xóa hãng đang có mẫu xe hoặc mẫu xe đang có xe nếu không có chính sách cascade rõ ràng.
- Cần tránh hai request đồng thời tạo cùng tên trong một hãng.
- Các thay đổi hãng/mẫu xe có thể ảnh hưởng kết quả tìm kiếm và cache danh mục.

### 3.14 `CarSpecificationServiceImpl`

**Mục đích:** quản lý thông số kỹ thuật linh hoạt của xe trên MongoDB.

**Điểm cần xem xét:**
- MongoDB không có foreign key tới SQL `Car`; service phải xác minh `carId` tồn tại.
- Cần quy định một xe có một hay nhiều bộ thông số; nếu một bộ, phải bảo vệ uniqueness theo `carId`.
- Nên validate cấu trúc và đơn vị đo để tránh lưu lẫn dữ liệu kiểu `"2.0L"`, `2`, `"2000 cc"` mà client không thể lọc nhất quán.
- Khi xóa xe trong SQL, cần dọn hoặc vô hiệu hóa document liên quan.

### 3.15 `CarMediaServiceImpl`

**Mục đích:** quản lý ảnh/video hoặc media của xe trên MongoDB.

**Điểm cần xem xét:**
- Phải xác minh `carId` tồn tại trước khi tạo media document.
- Cần kiểm tra URL/metadata và giới hạn kiểu, dung lượng, số lượng media nếu có upload.
- Cần định nghĩa media chính/thumbnail và thứ tự hiển thị ổn định.
- Xóa xe SQL cần có chính sách dọn media document; nếu không sẽ sinh orphan documents.
- Nếu URL trỏ tới storage bên ngoài, cần xử lý vòng đời file để tránh record còn nhưng file mất, hoặc file rác sau khi xóa record.

### 3.16 `CarReviewServiceImpl`

**Mục đích:** quản lý đánh giá xe trên MongoDB.

**Điểm cần xem xét:**
- Phải xác minh xe và người đánh giá tồn tại.
- Nếu chỉ khách đã mua mới được đánh giá, service phải kiểm tra đơn đã hoàn tất và quyền sở hữu đơn; nếu chưa có quy tắc này, cần xác định chủ đích nghiệp vụ.
- Cần quy định mỗi user được đánh giá một lần hay nhiều lần; nếu một lần, cần unique index phù hợp.
- Cần validate rating trong khoảng cho phép, nội dung và trạng thái duyệt.
- Cần chống giả mạo `userId` từ request body; lấy danh tính từ principal đã xác thực.

### 3.17 `ActivityLogServiceImpl`

**Mục đích:** ghi nhận lịch sử thao tác/audit log.

**Điểm cần xem xét:**
- Log quan trọng nên ghi actor, hành động, đối tượng, thời điểm và kết quả; không ghi password, token hoặc dữ liệu nhạy cảm.
- Cần xác định thao tác nào bắt buộc có log, đặc biệt cập nhật trạng thái đơn, payment, hợp đồng và hóa đơn.
- Nếu log thất bại, cần quy định có rollback nghiệp vụ hay log theo cơ chế riêng; tránh vừa thành công nhưng không có dấu vết khi audit là yêu cầu bắt buộc.
- Không nên cho sửa/xóa audit log như dữ liệu CRUD thông thường.

### 3.18 `ReportServiceImpl`

**Mục đích:** tổng hợp báo cáo bằng JDBC/truy vấn SQL, có thể khác đường truy xuất JPA thông thường.

**Điểm cần xem xét:**
- Báo cáo doanh thu phải định nghĩa rõ “doanh thu”: tổng đơn hoàn tất, tổng payment đã thu, hay tiền thu ròng sau refund; các chỉ số này không thể dùng thay thế nhau.
- Truy vấn cần lọc trạng thái payment/order nhất quán với nghiệp vụ.
- Cần kiểm tra ranh giới ngày/giờ, timezone, ngày kết thúc có inclusive/exclusive không.
- Dữ liệu JDBC phải tương thích với schema thật và H2 test profile; SQL chạy trên H2 chưa chắc có cùng hành vi với MariaDB/MySQL.
- Cần kiểm tra truy vấn có tham số hóa, tránh ghép chuỗi từ input vào SQL.

## 4. Repository inventory — nhiệm vụ và đánh giá

Repository nên là lớp truy cập dữ liệu, không phải nơi tự quyết định trạng thái nghiệp vụ. Dưới đây là các repository SQL đã xác minh qua source và vai trò chính.

| Repository | Trách nhiệm chính | Điểm cần chú ý |
|---|---|---|
| `CarRepository` | Truy vấn `Car` theo tên, hãng, model; tìm xe theo giá và số lượng trạng thái `CarItem` | Các truy vấn giá/tồn kho phải thống nhất định nghĩa giá và trạng thái “còn hàng”; điều kiện `COUNT >= minQuantity` cần xử lý `minQuantity <= 0` ở tầng validation |
| `CarItemRepository` | Truy vấn từng chiếc xe theo `carId`, status, VIN; đếm tồn kho; xóa theo xe | Cần unique index cho VIN; xóa theo `carId` phải được bảo vệ bởi quy tắc lịch sử và quan hệ |
| `SalesOrderRepository` | Tra cứu đơn theo code, khách hàng, trạng thái; cung cấp options cho hóa đơn/hợp đồng/payment | `findActiveOrderOptions()` chỉ loại `CANCELLED`; cần quyết định có cho chọn `COMPLETED`/đơn không đủ điều kiện hay không |
| `PaymentRepository` | Lấy payment theo order, kiểm tra order có payment | Chưa thấy phương thức tổng hợp tổng tiền theo trạng thái; có thể cần query tổng tiền hợp lệ để tránh tính toán rải rác ở service |
| `InvoiceRepository` | Tra cứu invoice number/order; kiểm tra invoice đã tồn tại; liệt kê order ID có invoice | Cần unique constraint cho invoice number và order ID nếu quan hệ 1:1 |
| `ContractRepository` | Tra cứu contract number/order; kiểm tra đã có contract; liệt kê order ID có contract | Cần unique constraint cho contract number và order ID nếu quan hệ 1:1 |
| `BrandRepository` | Interface CRUD hãng xe và kiểm tra tên | Đây là custom interface, không trực tiếp kế thừa `JpaRepository`; cần kiểm tra implementation thực tế và bảo đảm uniqueness ở DB |
| `CarModelRepository` | Tìm model theo tên/hãng; kiểm tra tên model trong một hãng; kiểm tra hãng đang có model | `existsByNameAndBrand_Id` nên đi cùng unique constraint theo cặp (brand_id, normalized_name) |
| `CustomerRepository` | Tìm khách theo tên/phone; kiểm tra email; tìm theo user ID | `existsByEmail`/`findByEmail` không thay thế unique constraint; chuẩn hóa email |
| `EmployeeRepository` | Tìm nhân viên theo tên; CRUD JPA mặc định | Cần policy xóa/disable nhân viên có lịch sử |
| `TestDriveAppointmentRepository` | Truy vấn lịch theo khách/status/thời gian; kiểm tra liên kết xe, khách, nhân viên | `exists...` chỉ kiểm tra sự tồn tại liên kết, không phát hiện lịch chồng giờ; cần query overlap hoặc logic service có đầy đủ |
| `UserRepository` | Tìm user theo username/email và kiểm tra tồn tại | Username/email cần unique constraint; `exists...` không chống được concurrent registration |

### Repository MongoDB / document store

Các service `CarSpecificationServiceImpl`, `CarMediaServiceImpl`, `CarReviewServiceImpl` dùng document MongoDB. Các repository tương ứng cần được xác minh theo package/tên chính xác trong toàn bộ tree trước khi kết luận từng method. Về thiết kế, cần kiểm tra:

- index cho `carId`, `userId`, và các khóa nghiệp vụ;
- quy tắc uniqueness;
- pagination/sorting cho danh sách review/media;
- dọn document khi entity SQL liên quan bị xóa;
- kiểm tra tham chiếu SQL từ service vì MongoDB không thể áp dụng foreign key tới MySQL;
- chính sách xử lý lỗi khi cập nhật SQL và MongoDB không thể nằm trong một transaction chung.

**Lưu ý:** repository interface đơn thuần thường không phải nơi có bug nghiệp vụ trực tiếp. Rủi ro thường nằm ở query semantics, thiếu index/constraint, transaction boundary và cách service sử dụng repository.

## 5. Các vấn đề xuyên module cần thiết kế thống nhất

### 5.1 State machine cho đơn hàng

Không nên cho nhiều method tự cập nhật `OrderStatus` theo cách riêng. Nên có một hàm/use case chuyển trạng thái trung tâm, ví dụ:

- xác thực trạng thái hiện tại → trạng thái đích;
- kiểm tra điều kiện payment;
- giữ/trả tồn kho đúng một lần;
- cập nhật order và car item trong cùng transaction SQL;
- invalidation cache sau commit;
- ghi audit log;
- xử lý hợp đồng/hóa đơn theo chính sách.

Tên trạng thái thực tế phải lấy từ enum của dự án; ví dụ trên chỉ mô tả nguyên tắc.

### 5.2 Một nguồn sự thật cho trạng thái thanh toán

Cần tính tiền đã thu từ các payment được xem là hợp lệ theo quy ước nghiệp vụ, thay vì suy luận “có một payment PAID = đã thanh toán đủ”. Xác định rõ:
- `amountDue`
- `amountPaid`
- `amountRefunded`
- `balanceDue`
- điều kiện chuyển order sang trạng thái hoàn tất.

Nên dùng `BigDecimal`, scale/rounding policy rõ ràng và kiểm tra tổng thu không vượt mức cho phép.

### 5.3 Bảo vệ tồn kho trước concurrent requests

Một transaction đơn thuần chưa chắc ngăn được race condition nếu hai request cùng đọc `AVAILABLE`. Các hướng có thể cân nhắc:
- optimistic locking (`@Version`) và retry/409 khi conflict;
- pessimistic row lock;
- conditional update kiểu `UPDATE ... WHERE status = 'AVAILABLE'` rồi xác nhận đúng một dòng được cập nhật;
- unique constraint/quan hệ dữ liệu phù hợp để một `CarItem` không gắn với nhiều đơn bán đang hiệu lực.

Chọn một chiến lược phù hợp và kiểm thử bằng hai request đồng thời.

### 5.4 SQL + MongoDB không có transaction chung mặc định

Nếu thao tác tạo/xóa xe phải cập nhật cả SQL lẫn MongoDB, không giả định `@Transactional` của JPA sẽ rollback được document MongoDB. Cần:
- thứ tự thao tác có chủ đích;
- cơ chế retry/compensation;
- job dọn orphan;
- hoặc mô hình lifecycle/soft-delete phù hợp.

### 5.5 Cache invalidation

Nên có một quy ước rõ: sau khi commit bất kỳ thay đổi nào ảnh hưởng `CarCatalogService` (xe, giá, trạng thái từng chiếc, media/specification nếu response chứa chúng), phải xóa cache tương ứng. Tránh invalidate trước commit nếu có thể khiến cache được nạp lại từ dữ liệu cũ.

## 6. Thứ tự xử lý khuyến nghị

### P0/P1 — tính toàn vẹn giao dịch
1. Xây dựng state machine và gom logic chuyển trạng thái order.
2. Chặn hoàn tất order nếu chưa thanh toán đủ theo quy tắc.
3. Chặn hủy order đã hoàn tất nếu chưa có quy trình refund/cancel chứng từ.
4. Bảo vệ chọn/giữ xe bằng thao tác nguyên tử hoặc locking.
5. Bảo đảm `CarItem` không thể chuyển tự do từ `SOLD` về `AVAILABLE`.

### P1/P2 — liên kết nghiệp vụ
6. Ràng buộc trạng thái và điều kiện tạo payment/contract/invoice.
7. Kiểm tra lịch lái thử chồng lấn và khả năng lái thử của xe.
8. Chuẩn hóa dữ liệu VIN, username, email, brand/model; thêm unique constraints.
9. Xác minh `carId`/`userId` cho Mongo documents và xử lý orphan.

### P2 — dữ liệu hiển thị và báo cáo
10. Chuẩn hóa cache key và invalidation.
11. Định nghĩa doanh thu, tiền thực thu, tiền hoàn và số dư còn phải thu.
12. Kiểm tra query JDBC giữa production DB và H2 test profile.
13. Bổ sung audit trail và regression tests cho luồng trạng thái.

## 7. Bộ test hồi quy tối thiểu

- Hai request đồng thời mua cùng một `CarItem`: chỉ một request thành công.
- Hủy đơn đang xử lý: xe được trả về đúng một lần.
- Gọi `updateStatus(CANCELLED)` và `cancel()`: cùng một chính sách tồn kho.
- Hủy đơn đã `COMPLETED`: bị từ chối hoặc chạy đầy đủ quy trình refund/chứng từ.
- Một payment `PAID` nhưng số tiền thấp hơn tổng đơn: order không được đánh dấu đã thanh toán đủ.
- Sửa/xóa payment sau khi order hoàn tất: trạng thái order và báo cáo được xử lý nhất quán.
- `SOLD → AVAILABLE`: bị chặn trừ khi có quy trình hoàn tác được phê duyệt.
- Tạo hai lịch lái thử chồng giờ cho cùng xe hoặc nhân viên: request xung đột bị từ chối.
- Tạo review/specification/media với `carId` không tồn tại: bị từ chối.
- Tạo hai VIN/username/email giống nhau đồng thời: DB chỉ chấp nhận một bản ghi.
- Sửa trạng thái `CarItem`: chi tiết danh mục sau invalidation phải phản ánh dữ liệu mới.
- Báo cáo doanh thu với đơn hủy, payment chưa trả và refund: số liệu đúng theo định nghĩa đã thống nhất.

## 8. Giới hạn của lần rà soát

- Đây là **code review tĩnh**, không phải chứng nhận hệ thống chạy đúng.
- Đã đọc/đối chiếu các service được liệt kê trong phần service inventory và các repository SQL được xác minh ở trên; không khẳng định mọi repository/document repository trong toàn bộ repository tree đã được duyệt từng dòng.
- Chưa xác minh toàn bộ controller, entity annotations, DB migrations/schema thực tế, frontend, mọi test, và các luồng cart/session Redis.
- Các điểm đánh dấu “cần xem xét” cần được đối chiếu với yêu cầu nghiệp vụ chính thức trước khi coi là bug chắc chắn.

