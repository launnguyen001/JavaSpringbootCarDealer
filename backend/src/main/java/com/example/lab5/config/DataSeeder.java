package com.example.lab5.config;

import com.example.lab5.document.ActivityLog;
import com.example.lab5.document.CarMedia;
import com.example.lab5.document.CarReview;
import com.example.lab5.document.CarSpecification;
import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentMethod;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.entity.TestDriveAppointment;
import com.example.lab5.entity.User;
import com.example.lab5.mongorepository.ActivityLogRepository;
import com.example.lab5.mongorepository.CarMediaRepository;
import com.example.lab5.mongorepository.CarReviewRepository;
import com.example.lab5.mongorepository.CarSpecificationRepository;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarItemRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed dữ liệu mẫu cho môi trường chạy thật (không chạy khi test).
 *
 * <p>Dữ liệu gồm 2 phần độc lập:
 * <ul>
 *   <li><b>SQL (MySQL):</b> hãng xe, dòng xe, xe, khách hàng, nhân viên, tài khoản,
 *       đơn hàng + dòng đơn, thanh toán, hợp đồng, hóa đơn, lịch lái thử.</li>
 *   <li><b>MongoDB:</b> thông số kỹ thuật, media, đánh giá, nhật ký hoạt động.</li>
 * </ul>
 *
 * <p>Seeder có tính <b>idempotent</b> (bỏ qua nếu dữ liệu đã tồn tại) và phần MongoDB được
 * bọc try/catch để ứng dụng vẫn khởi động được khi MongoDB chưa chạy.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] IMAGE_POOL = {
            "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1200&q=80",
            "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1200&q=80",
            "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=1200&q=80",
            "https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=1200&q=80",
            "https://images.unsplash.com/photo-1542362567-b07e54358753?w=1200&q=80",
            "https://images.unsplash.com/photo-1493238792000-8113da705763?w=1200&q=80",
            "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=1200&q=80",
            "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=1200&q=80",
    };

    private static final String[] VIDEO_POOL = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
    };

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CarRepository carRepository;
    private final CarItemRepository carItemRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TestDriveAppointmentRepository testDriveAppointmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final CarSpecificationRepository carSpecificationRepository;
    private final CarMediaRepository carMediaRepository;
    private final CarReviewRepository carReviewRepository;
    private final ActivityLogRepository activityLogRepository;

    private final String mongoUri;

    public DataSeeder(
            BrandRepository brandRepository,
            CarModelRepository carModelRepository,
            CarRepository carRepository,
            CarItemRepository carItemRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            SalesOrderRepository salesOrderRepository,
            PaymentRepository paymentRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            TestDriveAppointmentRepository testDriveAppointmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CarSpecificationRepository carSpecificationRepository,
            CarMediaRepository carMediaRepository,
            CarReviewRepository carReviewRepository,
            ActivityLogRepository activityLogRepository,
            @Value("${spring.mongodb.uri}") String mongoUri
    ) {
        this.brandRepository = brandRepository;
        this.carModelRepository = carModelRepository;
        this.carRepository = carRepository;
        this.carItemRepository = carItemRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.testDriveAppointmentRepository = testDriveAppointmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.carSpecificationRepository = carSpecificationRepository;
        this.carMediaRepository = carMediaRepository;
        this.carReviewRepository = carReviewRepository;
        this.activityLogRepository = activityLogRepository;
        this.mongoUri = mongoUri;
    }

    @Override
    public void run(String... args) {
        seedSqlData();
        seedMongoData();
    }

    // ============================================================
    // 1. SQL (MySQL)
    // ============================================================

    private void seedSqlData() {
        if (!brandRepository.findAll().isEmpty()) {
            log.info("Bỏ qua seed SQL: dữ liệu đã tồn tại");
            return;
        }
        log.info("Bắt đầu seed dữ liệu SQL (MySQL)…");

        // ---------- Hãng xe ----------
        Brand toyota = brandRepository.save(brand("Toyota", "Nhật Bản",
                "Hãng xe phổ thông hàng đầu Nhật Bản, bền bỉ và tiết kiệm."));
        Brand honda = brandRepository.save(brand("Honda", "Nhật Bản",
                "Thương hiệu Nhật Bản nổi tiếng với động cơ bền bỉ và công nghệ an toàn."));
        Brand ford = brandRepository.save(brand("Ford", "Hoa Kỳ",
                "Hãng xe Mỹ mạnh mẽ, nổi bật ở dòng bán tải và SUV."));
        Brand mercedes = brandRepository.save(brand("Mercedes-Benz", "Đức",
                "Thương hiệu xe sang Đức với thiết kế lịch lãm và công nghệ dẫn đầu."));
        Brand bmw = brandRepository.save(brand("BMW", "Đức",
                "Xe sang Đức với triết lý lái 'Ultimate Driving Machine'."));
        Brand audi = brandRepository.save(brand("Audi", "Đức",
                "Xe sang Đức đề cao công nghệ và hệ dẫn động quattro."));
        Brand lexus = brandRepository.save(brand("Lexus", "Nhật Bản",
                "Thương hiệu xe sang của Toyota, tập trung vào sự êm ái và tinh tế."));
        Brand porsche = brandRepository.save(brand("Porsche", "Đức",
                "Biểu tượng xe thể thao hiệu năng cao."));
        Brand vinfast = brandRepository.save(brand("VinFast", "Việt Nam",
                "Hãng xe Việt Nam tiên phong xe điện và xe thông minh."));

        // ---------- Dòng xe ----------
        CarModel camry = carModelRepository.save(model("Camry", "Sedan",
                "Sedan hạng D sang trọng, vận hành êm ái.", toyota));
        CarModel corolla = carModelRepository.save(model("Corolla Cross", "SUV",
                "Crossover đô thị tiết kiệm nhiên liệu.", toyota));
        CarModel fortuner = carModelRepository.save(model("Fortuner", "SUV",
                "SUV 7 chỗ khung gầm rời, đa dụng.", toyota));

        CarModel civic = carModelRepository.save(model("Civic", "Sedan",
                "Sedan thể thao hướng tới người lái trẻ.", honda));
        CarModel crv = carModelRepository.save(model("CR-V", "SUV",
                "SUV 7 chỗ êm ái, an toàn.", honda));
        CarModel city = carModelRepository.save(model("City", "Sedan",
                "Sedan hạng B tiết kiệm, phù hợp đô thị.", honda));

        CarModel ranger = carModelRepository.save(model("Ranger", "Bán tải",
                "Bán tải mạnh mẽ, đa năng.", ford));
        CarModel everest = carModelRepository.save(model("Everest", "SUV",
                "SUV 7 chỗ khung gầm rời cho gia đình.", ford));

        CarModel cClass = carModelRepository.save(model("C-Class", "Sedan",
                "Sedan sang trọng cỡ trung.", mercedes));
        CarModel eClass = carModelRepository.save(model("E-Class", "Sedan",
                "Sedan sang trọng hạng sang cỡ lớn.", mercedes));
        CarModel glc = carModelRepository.save(model("GLC", "SUV",
                "SUV sang trọng đô thị.", mercedes));

        CarModel bmw3 = carModelRepository.save(model("3 Series", "Sedan",
                "Sedan thể thao cỡ trung.", bmw));
        CarModel bmw5 = carModelRepository.save(model("5 Series", "Sedan",
                "Sedan sang trọng cỡ lớn.", bmw));
        CarModel bmwX5 = carModelRepository.save(model("X5", "SUV",
                "SUV sang trọng cỡ lớn.", bmw));

        CarModel a4 = carModelRepository.save(model("A4", "Sedan",
                "Sedan sang trọng cỡ trung.", audi));
        CarModel q5 = carModelRepository.save(model("Q5", "SUV",
                "SUV sang trọng cỡ trung.", audi));

        CarModel es = carModelRepository.save(model("ES", "Sedan",
                "Sedan sang trọng êm ái.", lexus));
        CarModel rx = carModelRepository.save(model("RX", "SUV",
                "SUV sang trọng cỡ lớn.", lexus));

        CarModel macan = carModelRepository.save(model("Macan", "SUV",
                "SUV thể thao hiệu năng cao.", porsche));
        CarModel p911 = carModelRepository.save(model("911 Carrera", "Coupe",
                "Xe thể thao huyền thoại.", porsche));

        CarModel vf8 = carModelRepository.save(model("VF 8", "SUV",
                "SUV điện thông minh.", vinfast));
        CarModel vf9 = carModelRepository.save(model("VF 9", "SUV",
                "SUV điện 7 chỗ cỡ lớn.", vinfast));

        // ---------- Dòng xe + các chiếc xe trong kho ----------
        // Mỗi dòng xe (cars) có nhiều chiếc vật lý (car_items). Biến giữ CHIẾC ĐẦU TIÊN
        // để gắn vào đơn hàng/lịch lái thử bên dưới (tên biến giống trước đây).
        Car trimCamry = carRepository.save(car("Toyota Camry 2.0G", 950.0,
                "Camry 2.0G động cơ 2.0L, nội thất da, màn hình 9 inch.", camry));
        CarItem camry20 = stock(trimCamry, "VIN-CAM20-", 950.0, 5, 2024, "Đen", CarStatus.AVAILABLE);

        Car trimCamry25 = carRepository.save(car("Toyota Camry 2.5Q", 1200.0,
                "Camry 2.5Q bản cao cấp, cửa sổ trời, ghế chỉnh điện.", camry));
        CarItem camry25 = stock(trimCamry25, "VIN-CAM25-", 1200.0, 3, 2024, "Trắng", CarStatus.AVAILABLE);

        Car trimCorolla = carRepository.save(car("Toyota Corolla Cross 1.8V", 800.0,
                "Crossover đô thị, gói an toàn Toyota Safety Sense.", corolla));
        CarItem corollaCross = stock(trimCorolla, "VIN-COR-", 800.0, 8, 2023, "Bạc", CarStatus.AVAILABLE);

        Car trimFortuner = carRepository.save(car("Toyota Fortuner 2.4G", 1150.0,
                "SUV 7 chỗ khung gầm rời, máy dầu 2.4L.", fortuner));
        CarItem fortunerCar = stock(trimFortuner, "VIN-FOR-", 1150.0, 4, 2024, "Xám", CarStatus.RESERVED);

        Car trimCivic = carRepository.save(car("Honda Civic 1.5 RS", 780.0,
                "Civic RS thể thao, động cơ turbo 1.5L.", civic));
        CarItem civicCar = stock(trimCivic, "VIN-CIV-", 780.0, 6, 2024, "Xanh", CarStatus.AVAILABLE);

        Car trimCrv = carRepository.save(car("Honda CR-V L", 1050.0,
                "CR-V bản L, Honda Sensing, 7 chỗ.", crv));
        CarItem crvCar = stock(trimCrv, "VIN-CRV-", 1050.0, 5, 2024, "Đỏ", CarStatus.AVAILABLE);

        Car trimCity = carRepository.save(car("Honda City RS", 560.0,
                "Sedan hạng B tiết kiệm nhiên liệu.", city));
        CarItem cityCar = stock(trimCity, "VIN-CIT-", 560.0, 10, 2023, "Trắng", CarStatus.AVAILABLE);

        Car trimRanger = carRepository.save(car("Ford Ranger Wildtrak", 1100.0,
                "Bán tải Wildtrak 2.0L Bi-Turbo, 4x4.", ranger));
        CarItem rangerCar = stock(trimRanger, "VIN-RAN-", 1100.0, 4, 2023, "Cam", CarStatus.AVAILABLE);

        Car trimEverest = carRepository.save(car("Ford Everest Titanium", 1300.0,
                "SUV 7 chỗ Titanium, động cơ dầu 2.0L.", everest));
        CarItem everestCar = stock(trimEverest, "VIN-EVE-", 1300.0, 3, 2024, "Đen", CarStatus.RESERVED);

        Car trimC300 = carRepository.save(car("Mercedes-Benz C300 AMG", 1900.0,
                "C300 AMG Line, động cơ 2.0L tăng áp.", cClass));
        CarItem c300 = stock(trimC300, "VIN-MERC-", 1900.0, 3, 2024, "Trắng", CarStatus.AVAILABLE);

        Car trimE300 = carRepository.save(car("Mercedes-Benz E300 AMG", 2600.0,
                "E300 AMG, nội thất cao cấp, MBUX.", eClass));
        CarItem e300 = stock(trimE300, "VIN-MERE-", 2600.0, 2, 2024, "Đen", CarStatus.AVAILABLE);

        Car trimGlc = carRepository.save(car("Mercedes-Benz GLC 300 4MATIC", 2200.0,
                "GLC 300 4MATIC, SUV sang đô thị.", glc));
        CarItem glc300 = stock(trimGlc, "VIN-MERG-", 2200.0, 4, 2024, "Xám", CarStatus.AVAILABLE);

        Car trimBmw3 = carRepository.save(car("BMW 320i Sport Line", 1800.0,
                "320i Sport Line, động cơ 2.0L TwinPower.", bmw3));
        CarItem bmw320i = stock(trimBmw3, "VIN-BMW3-", 1800.0, 3, 2024, "Xanh đen", CarStatus.AVAILABLE);

        Car trimBmw5 = carRepository.save(car("BMW 520i M Sport", 2500.0,
                "520i M Sport, sedan sang cỡ lớn.", bmw5));
        CarItem bmw520i = stock(trimBmw5, "VIN-BMW5-", 2500.0, 2, 2024, "Đen", CarStatus.AVAILABLE);

        Car trimBmwX5 = carRepository.save(car("BMW X5 xDrive40i", 4200.0,
                "X5 xDrive40i, SUV sang cỡ lớn.", bmwX5));
        CarItem bmwx5Car = stock(trimBmwX5, "VIN-BMWX-", 4200.0, 2, 2024, "Trắng", CarStatus.MAINTENANCE);

        Car trimA4 = carRepository.save(car("Audi A4 40 TFSI", 1700.0,
                "A4 40 TFSI, thiết kế tối giản sang trọng.", a4));
        CarItem a4Car = stock(trimA4, "VIN-AUD4-", 1700.0, 3, 2023, "Trắng", CarStatus.AVAILABLE);

        Car trimQ5 = carRepository.save(car("Audi Q5 45 TFSI quattro", 2300.0,
                "Q5 quattro, SUV sang cỡ trung.", q5));
        CarItem q5Car = stock(trimQ5, "VIN-AUD5-", 2300.0, 2, 2024, "Đen", CarStatus.AVAILABLE);

        Car trimEs = carRepository.save(car("Lexus ES 250", 2700.0,
                "ES 250 êm ái, nội thất tinh tế.", es));
        CarItem es250 = stock(trimEs, "VIN-LEXE-", 2700.0, 2, 2024, "Trắng", CarStatus.AVAILABLE);

        Car trimRx = carRepository.save(car("Lexus RX 350", 3500.0,
                "RX 350 SUV sang cỡ lớn.", rx));
        CarItem rx350 = stock(trimRx, "VIN-LEXR-", 3500.0, 1, 2024, "Đen", CarStatus.SOLD);

        Car trimMacan = carRepository.save(car("Porsche Macan S", 4300.0,
                "Macan S, SUV thể thao hiệu năng cao.", macan));
        CarItem macanCar = stock(trimMacan, "VIN-POR-", 4300.0, 2, 2024, "Đỏ", CarStatus.AVAILABLE);

        Car trimP911 = carRepository.save(car("Porsche 911 Carrera", 8000.0,
                "911 Carrera, xe thể thao huyền thoại.", p911));
        CarItem p911Car = stock(trimP911, "VIN-POR9-", 8000.0, 1, 2024, "Vàng", CarStatus.RESERVED);

        Car trimVf8 = carRepository.save(car("VinFast VF 8 Plus", 1100.0,
                "VF 8 bản Plus, SUV điện thông minh.", vf8));
        CarItem vf8Car = stock(trimVf8, "VIN-VF8-", 1100.0, 6, 2024, "Xanh", CarStatus.AVAILABLE);

        Car trimVf9 = carRepository.save(car("VinFast VF 9 Plus", 1500.0,
                "VF 9 bản Plus, SUV điện 7 chỗ.", vf9));
        CarItem vf9Car = stock(trimVf9, "VIN-VF9-", 1500.0, 4, 2024, "Trắng", CarStatus.AVAILABLE);

        // ---------- Khách hàng ----------
        Customer c1 = customerRepository.save(customer("Nguyễn Văn An", "an.nguyen@example.com",
                "0901234567", "Số 12 Lê Lợi, Quận 1, TP. Hồ Chí Minh", "001090012345"));
        Customer c2 = customerRepository.save(customer("Trần Thị Bích", "bich.tran@example.com",
                "0912345678", "45 Nguyễn Huệ, Hà Nội", "079090098765"));
        Customer c3 = customerRepository.save(customer("Lê Hoàng Cường", "cuong.le@example.com",
                "0923456789", "88 Bạch Đằng, Đà Nẵng", "048090045678"));
        Customer c4 = customerRepository.save(customer("Phạm Thu Dung", "dung.pham@example.com",
                "0934567890", "22 Hòa Bình, Cần Thơ", "096090076543"));
        Customer c5 = customerRepository.save(customer("Vũ Minh Đức", "duc.vu@example.com",
                "0945678901", "17 Lạch Tray, Hải Phòng", "031090023456"));
        Customer c6 = customerRepository.save(customer("Đặng Ngọc Hà", "ha.dang@example.com",
                "0956789012", "9 Hùng Vương, Huế", "033090054321"));

        // ---------- Nhân viên ----------
        Employee e1 = employeeRepository.save(employee("Lê Quang Huy", "huy.le@cardealer.vn",
                "0911000001", "Nhân viên kinh doanh"));
        Employee e2 = employeeRepository.save(employee("Phạm Thị Mai", "mai.pham@cardealer.vn",
                "0911000002", "Quản lý kinh doanh"));
        Employee e3 = employeeRepository.save(employee("Trương Văn Nam", "nam.truong@cardealer.vn",
                "0911000003", "Nhân viên kinh doanh"));
        Employee e4 = employeeRepository.save(employee("Ngô Thị Lan", "lan.ngo@cardealer.vn",
                "0911000004", "Cố vấn dịch vụ"));

        // ---------- Đơn hàng ----------
        SalesOrder o1 = salesOrderRepository.save(order("ORD-2026-0001", dt(2026, 1, 15, 9, 30),
                OrderStatus.COMPLETED, c1, e1, List.of(item(corollaCross, 1))));
        SalesOrder o2 = salesOrderRepository.save(order("ORD-2026-0002", dt(2026, 2, 8, 14, 0),
                OrderStatus.COMPLETED, c2, e3, List.of(item(civicCar, 1))));
        SalesOrder o3 = salesOrderRepository.save(order("ORD-2026-0003", dt(2026, 3, 22, 10, 15),
                OrderStatus.CANCELLED, c3, e1, List.of(item(rangerCar, 1))));
        SalesOrder o4 = salesOrderRepository.save(order("ORD-2026-0004", dt(2026, 4, 5, 16, 45),
                OrderStatus.COMPLETED, c4, e2, List.of(item(c300, 1))));
        SalesOrder o5 = salesOrderRepository.save(order("ORD-2026-0005", dt(2026, 5, 19, 11, 0),
                OrderStatus.COMPLETED, c5, e3, List.of(item(crvCar, 1), item(cityCar, 1))));
        SalesOrder o6 = salesOrderRepository.save(order("ORD-2026-0006", dt(2026, 6, 11, 8, 45),
                OrderStatus.CONFIRMED, c6, e1, List.of(item(vf8Car, 1))));
        SalesOrder o7 = salesOrderRepository.save(order("ORD-2026-0007", dt(2026, 7, 2, 13, 20),
                OrderStatus.COMPLETED, c1, e2, List.of(item(bmw320i, 1))));
        SalesOrder o8 = salesOrderRepository.save(order("ORD-2026-0008", dt(2026, 7, 27, 15, 10),
                OrderStatus.COMPLETED, c2, e3, List.of(item(camry20, 1))));
        SalesOrder o9 = salesOrderRepository.save(order("ORD-2026-0009", dt(2026, 8, 14, 9, 0),
                OrderStatus.PENDING, c3, e1, List.of(item(macanCar, 1))));
        SalesOrder o10 = salesOrderRepository.save(order("ORD-2026-0010", dt(2026, 9, 9, 10, 30),
                OrderStatus.COMPLETED, c4, e2, List.of(item(glc300, 1))));
        SalesOrder o11 = salesOrderRepository.save(order("ORD-2026-0011", dt(2026, 9, 28, 17, 15),
                OrderStatus.CONFIRMED, c5, e3, List.of(item(vf9Car, 1))));
        SalesOrder o12 = salesOrderRepository.save(order("ORD-2026-0012", dt(2026, 10, 6, 9, 45),
                OrderStatus.COMPLETED, c6, e1, List.of(item(rx350, 1))));
        SalesOrder o13 = salesOrderRepository.save(order("ORD-2026-0013", dt(2026, 10, 12, 14, 30),
                OrderStatus.COMPLETED, c1, e2, List.of(item(e300, 1))));
        SalesOrder o14 = salesOrderRepository.save(order("ORD-2026-0014", dt(2026, 10, 15, 16, 0),
                OrderStatus.PENDING, c2, e3, List.of(item(p911Car, 1))));

        // Đánh dấu SOLD cho những chiếc đã nằm trong đơn (trừ đơn đã huỷ)
        for (SalesOrder o : List.of(o1, o2, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14)) {
            for (OrderItem oi : o.getItems()) {
                CarItem ci = oi.getCarItem();
                if (ci != null && ci.getStatus() == CarStatus.AVAILABLE) {
                    ci.setStatus(CarStatus.SOLD);
                    carItemRepository.save(ci);
                }
            }
        }

        // ---------- Thanh toán ----------
        paymentRepository.save(payment(o1, o1.getTotalAmount(), PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, dt(2026, 1, 16, 10, 0)));
        paymentRepository.save(payment(o2, o2.getTotalAmount(), PaymentMethod.CASH, PaymentStatus.PAID, dt(2026, 2, 8, 15, 0)));
        paymentRepository.save(payment(o3, o3.getTotalAmount(), PaymentMethod.CARD, PaymentStatus.REFUNDED, dt(2026, 3, 23, 9, 0)));
        paymentRepository.save(payment(o4, o4.getTotalAmount(), PaymentMethod.INSTALLMENT, PaymentStatus.PAID, dt(2026, 4, 6, 10, 0)));
        paymentRepository.save(payment(o5, o5.getTotalAmount(), PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, dt(2026, 5, 20, 11, 0)));
        paymentRepository.save(payment(o6, o6.getTotalAmount() * 0.3, PaymentMethod.BANK_TRANSFER, PaymentStatus.PARTIAL, dt(2026, 6, 11, 10, 0)));
        paymentRepository.save(payment(o7, o7.getTotalAmount(), PaymentMethod.CARD, PaymentStatus.PAID, dt(2026, 7, 3, 9, 30)));
        paymentRepository.save(payment(o8, o8.getTotalAmount(), PaymentMethod.CASH, PaymentStatus.PAID, dt(2026, 7, 27, 16, 0)));
        paymentRepository.save(payment(o9, o9.getTotalAmount(), PaymentMethod.BANK_TRANSFER, PaymentStatus.UNPAID, null));
        paymentRepository.save(payment(o10, o10.getTotalAmount(), PaymentMethod.INSTALLMENT, PaymentStatus.PAID, dt(2026, 9, 10, 14, 0)));
        paymentRepository.save(payment(o11, o11.getTotalAmount() * 0.5, PaymentMethod.BANK_TRANSFER, PaymentStatus.PARTIAL, dt(2026, 9, 29, 10, 0)));
        paymentRepository.save(payment(o12, o12.getTotalAmount(), PaymentMethod.CASH, PaymentStatus.PAID, dt(2026, 10, 6, 11, 0)));
        paymentRepository.save(payment(o13, o13.getTotalAmount(), PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, dt(2026, 10, 13, 9, 0)));
        paymentRepository.save(payment(o14, o14.getTotalAmount(), PaymentMethod.BANK_TRANSFER, PaymentStatus.UNPAID, null));

        // ---------- Hợp đồng & Hóa đơn (cho đơn COMPLETED) ----------
        List<SalesOrder> completed = List.of(o1, o2, o4, o5, o7, o8, o10, o12, o13);
        int contractSeq = 1;
        for (SalesOrder o : completed) {
            contractRepository.save(contract(o,
                    String.format("HD-2026-%04d", contractSeq),
                    o.getOrderDate().plusDays(1),
                    "Hợp đồng mua bán xe " + o.getOrderCode()
                            + " giữa " + o.getCustomer().getFullName()
                            + " và Car Dealer. Tổng giá trị " + o.getTotalAmount() + " triệu đồng."));
            invoiceRepository.save(invoice(o,
                    String.format("INV-2026-%04d", contractSeq),
                    o.getOrderDate().plusDays(2),
                    o.getTotalAmount()));
            contractSeq++;
        }

        // ---------- Lịch lái thử ----------
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 20, 9, 0), AppointmentStatus.SCHEDULED,
                "Muốn trải nghiệm vào buổi sáng.", c1, camry20, e1));
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 21, 14, 0), AppointmentStatus.SCHEDULED,
                null, c2, civicCar, e3));
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 25, 8, 30), AppointmentStatus.SCHEDULED,
                "Ưu tiên lái thử đường cao tốc.", c2, e300, e2));
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 22, 10, 0), AppointmentStatus.CONFIRMED,
                "Đã xác nhận qua điện thoại.", c3, macanCar, e1));
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 5, 15, 0), AppointmentStatus.COMPLETED,
                "Khách hài lòng, cân nhắc đặt cọc.", c4, glc300, e2));
        testDriveAppointmentRepository.save(appointment(dt(2026, 9, 30, 9, 30), AppointmentStatus.COMPLETED,
                null, c5, vf9Car, e3));
        testDriveAppointmentRepository.save(appointment(dt(2026, 10, 6, 11, 0), AppointmentStatus.COMPLETED,
                "Khách đã mua xe.", c6, rx350, e1));
        testDriveAppointmentRepository.save(appointment(dt(2026, 9, 15, 16, 0), AppointmentStatus.CANCELLED,
                "Khách bận, xin dời lịch.", c1, bmw520i, e3));

        // ---------- Tài khoản JWT ----------
        seedUser("admin", "admin", "admin@cardealer.vn", Role.ADMIN);
        seedUser("user", "user", "user@cardealer.vn", Role.USER);
        seedUser("employee", "employee", "employee@cardealer.vn", Role.EMPLOYEE);

        // Gắn tài khoản "user" với hồ sơ khách c1 để thử ngay API /api/customers/me
        linkCustomerToUser(c1, "user");

        log.info("Đã seed SQL: {} hãng, {} dòng xe, {} chiếc xe, {} khách hàng, {} nhân viên, {} đơn hàng, {} lịch lái thử",
                brandRepository.findAll().size(), carModelRepository.count(), carItemRepository.count(),
                customerRepository.count(), employeeRepository.count(), salesOrderRepository.count(),
                testDriveAppointmentRepository.count());
    }

    // ============================================================
    // 2. MongoDB
    // ============================================================

    private void seedMongoData() {
        // Kiểm tra nhanh trước khi gọi driver: driver mặc định chờ ~30s nếu không có server.
        if (!isMongoReachable()) {
            log.warn("MongoDB không phản hồi tại {} — bỏ qua seed dữ liệu NoSQL "
                    + "(thông số kỹ thuật, media, đánh giá, nhật ký)", describeMongoTarget());
            return;
        }
        try {
            if (carSpecificationRepository.count() > 0) {
                log.info("Bỏ qua seed MongoDB: dữ liệu đã tồn tại");
                return;
            }
            List<Car> cars = carRepository.findAll();
            if (cars.isEmpty()) {
                return;
            }
            log.info("Bắt đầu seed dữ liệu MongoDB (specs, media, reviews, activity logs)…");

            seedSpecifications(cars);
            seedMedia(cars);
            seedReviews(cars);
            seedActivityLogs(cars);

            log.info("Đã seed MongoDB cho {} xe", cars.size());
        } catch (Exception ex) {
            log.warn("Bỏ qua seed MongoDB (MongoDB có thể chưa chạy): {}", ex.getMessage());
        }
    }

    private InetSocketAddress mongoAddress() {
        try {
            String authority = URI.create(mongoUri.trim()).getAuthority();
            if (authority == null || authority.isBlank()) {
                return new InetSocketAddress("localhost", 27017);
            }
            // bỏ qua thông tin đăng nhập nếu có (mongodb://user:pass@host:port/db)
            String hostPort = authority.substring(authority.lastIndexOf('@') + 1).split(",")[0].trim();
            int colon = hostPort.indexOf(':');
            if (colon < 0) {
                return new InetSocketAddress(hostPort, 27017);
            }
            String host = hostPort.substring(0, colon);
            int port;
            try {
                port = Integer.parseInt(hostPort.substring(colon + 1));
            } catch (NumberFormatException ex) {
                port = 27017;
            }
            return new InetSocketAddress(host, port);
        } catch (Exception ex) {
            return new InetSocketAddress("localhost", 27017);
        }
    }

    private boolean isMongoReachable() {
        InetSocketAddress target = mongoAddress();
        try (Socket socket = new Socket()) {
            socket.connect(target, 1500);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String describeMongoTarget() {
        InetSocketAddress target = mongoAddress();
        return target.getHostString() + ":" + target.getPort();
    }

    private void seedSpecifications(List<Car> cars) {
        String[] engines = {"2.0L I4 Turbo", "2.5L I4", "3.0L V6", "2.0L Bi-Turbo Diesel"};
        String[] transmissions = {"Số tự động 8 cấp", "Số tự động 10 cấp", "Số tự động ly hợp kép 7 cấp"};
        String[] fuels = {"Xăng", "Dầu", "Điện"};
        String[] dimensions = {"4885 x 1840 x 1445 mm", "4725 x 1890 x 1685 mm", "5362 x 1860 x 1848 mm"};
        List<String> safety = List.of("ABS", "EBD", "Cân bằng điện tử ESP", "Hỗ trợ khởi hành ngang dốc",
                "Camera lùi", "6 túi khí");

        List<CarSpecification> specs = new ArrayList<>();
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            CarSpecification s = new CarSpecification();
            s.setCarId(car.getId());
            s.setEngine(engines[i % engines.length]);
            s.setTransmission(transmissions[i % transmissions.length]);
            s.setFuelType(fuels[i % fuels.length]);
            s.setSeats(5);
            s.setDimensions(dimensions[i % dimensions.length]);
            s.setWeightKg(1500.0 + (i % 5) * 150);
            s.setSafetyFeatures(new ArrayList<>(safety));
            Map<String, Object> extra = new HashMap<>();
            extra.put("warranty", "3 năm / 100.000 km");
            extra.put("origin", "Chính hãng");
            extra.put("airbags", 6);
            s.setExtra(extra);
            s.setUpdatedAt(LocalDateTime.now());
            specs.add(s);
        }
        carSpecificationRepository.saveAll(specs);
    }

    private void seedMedia(List<Car> cars) {
        List<CarMedia> media = new ArrayList<>();
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            CarMedia m = new CarMedia();
            m.setCarId(car.getId());
            m.setThumbnail(IMAGE_POOL[i % IMAGE_POOL.length]);
            List<String> images = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                images.add(IMAGE_POOL[(i + j) % IMAGE_POOL.length]);
            }
            m.setImages(images);
            m.setVideos(List.of(VIDEO_POOL[i % VIDEO_POOL.length]));
            m.setUpdatedAt(LocalDateTime.now());
            media.add(m);
        }
        carMediaRepository.saveAll(media);
    }

    private void seedReviews(List<Car> cars) {
        if (cars.isEmpty()) {
            return;
        }
        List<CarReview> reviews = new ArrayList<>();

        CarReview r1 = review(carId(cars, 0), 1L, "Nguyễn Văn An", 5,
                "Xe rất êm và tiết kiệm", "Camry chạy êm, cách âm tốt, phù hợp đi đường dài.",
                dt(2026, 2, 1, 10, 0),
                List.of(reply("Car Dealer", "Cảm ơn anh An đã tin tưởng!", dt(2026, 2, 2, 9, 0))));
        CarReview r2 = review(carId(cars, 0), 3L, "Lê Hoàng Cường", 4,
                "Nội thất rộng rãi", "Không gian rộng, nhưng giá hơi cao so với phân khúc.",
                dt(2026, 3, 5, 14, 30), List.of());
        CarReview r3 = review(carId(cars, 4), 2L, "Trần Thị Bích", 5,
                "Civic lái rất thích", "Cảm giác lái phấn khích, động cơ turbo mạnh mẽ.",
                dt(2026, 2, 20, 16, 0),
                List.of(reply("Car Dealer", "Cảm ơn chị Bích!", dt(2026, 2, 21, 8, 0)),
                        reply("Nguyễn Văn An", "Mình cũng đang cân nhắc mẫu này.", dt(2026, 2, 22, 11, 0))));
        CarReview r4 = review(carId(cars, 5), 4L, "Phạm Thu Dung", 4,
                "SUV đáng mua", "CR-V rộng rãi, an toàn, phù hợp gia đình.",
                dt(2026, 5, 25, 9, 0), List.of());
        CarReview r5 = review(carId(cars, 9), 5L, "Vũ Minh Đức", 5,
                "Xe sang đúng tầm", "Mercedes C300 nội thất đẹp, cách âm tuyệt vời.",
                dt(2026, 4, 10, 15, 0),
                List.of(reply("Car Dealer", "Rất vui vì anh hài lòng.", dt(2026, 4, 11, 9, 30))));
        CarReview r6 = review(carId(cars, 12), 6L, "Đặng Ngọc Hà", 5,
                "BMW 320i đáng đồng tiền", "Khả năng vận hành xuất sắc, nội thất hiện đại.",
                dt(2026, 7, 5, 10, 0), List.of());
        CarReview r7 = review(carId(cars, 7), 1L, "Nguyễn Văn An", 4,
                "Bán tải mạnh mẽ", "Ranger Wildtrak chạy địa hình rất tốt.",
                dt(2026, 8, 1, 13, 0), List.of());
        CarReview r8 = review(carId(cars, 11), 4L, "Phạm Thu Dung", 5,
                "SUV sang trọng", "GLC 300 vận hành êm, nội thất cao cấp.",
                dt(2026, 9, 15, 11, 0),
                List.of(reply("Car Dealer", "Cảm ơn chị Dung!", dt(2026, 9, 16, 9, 0))));

        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        reviews.add(r4);
        reviews.add(r5);
        reviews.add(r6);
        reviews.add(r7);
        reviews.add(r8);
        carReviewRepository.saveAll(reviews);
    }

    private void seedActivityLogs(List<Car> cars) {
        List<ActivityLog> logs = new ArrayList<>();
        logs.add(activity("CREATE", "CAR", carId(cars, 0), "Thêm xe Toyota Camry 2.0G", "admin", dt(2026, 1, 5, 8, 0)));
        logs.add(activity("CREATE", "ORDER", 1L, "Tạo đơn hàng ORD-2026-0001", "employee", dt(2026, 1, 15, 9, 30)));
        logs.add(activity("STATUS_CHANGE", "ORDER", 1L, "Chuyển trạng thái ORD-2026-0001 sang COMPLETED", "employee", dt(2026, 1, 20, 10, 0)));
        logs.add(activity("CREATE", "ORDER", 4L, "Tạo đơn hàng ORD-2026-0004", "employee", dt(2026, 4, 5, 16, 45)));
        logs.add(activity("UPDATE", "CAR", carId(cars, 9), "Cập nhật tồn kho Mercedes-Benz C300", "admin", dt(2026, 4, 6, 9, 0)));
        logs.add(activity("CREATE", "REVIEW", null, "Khách hàng đánh giá xe Civic 1.5 RS", "user", dt(2026, 2, 20, 16, 0)));
        logs.add(activity("DELETE", "TEST_DRIVE", 8L, "Hủy lịch lái thử của khách Nguyễn Văn An", "employee", dt(2026, 9, 15, 16, 30)));
        logs.add(activity("UPDATE", "CAR", carId(cars, 14), "Đưa BMW X5 vào bảo trì", "admin", dt(2026, 10, 1, 8, 30)));
        activityLogRepository.saveAll(logs);
    }

    // ============================================================
    // Helpers — SQL entities
    // ============================================================

    private void seedUser(String username, String password, String email, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setRole(role);
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    // Gắn hồ sơ khách với tài khoản đăng nhập (nếu chưa gắn) — phục vụ /api/customers/me
    private void linkCustomerToUser(Customer customer, String username) {
        if (customer == null || customer.getUser() != null) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            customer.setUser(user);
            customerRepository.save(customer);
        });
    }

    private Brand brand(String name, String country, String description) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry(country);
        brand.setDescription(description);
        return brand;
    }

    private CarModel model(String name, String bodyType, String description, Brand brand) {
        CarModel model = new CarModel();
        model.setName(name);
        model.setBodyType(bodyType);
        model.setDescription(description);
        model.setBrand(brand);
        return model;
    }

    private Car car(String name, Double basePrice, String description, CarModel model) {
        Car car = new Car();
        car.setName(name);
        car.setBasePrice(basePrice);
        car.setDescription(description);
        car.setModel(model);
        return car;
    }

    // Tạo một chiếc xe vật lý (car_item)
    private CarItem carItem(Car car, String vin, Double price, Integer year, String color,
                            CarCondition condition, CarStatus status, Integer mileageKm) {
        CarItem item = new CarItem();
        item.setCar(car);
        item.setVin(vin);
        item.setPrice(price);
        item.setYear(year);
        item.setColor(color);
        item.setCondition(condition);
        item.setStatus(status);
        item.setMileageKm(mileageKm);
        return item;
    }

    // Tạo `count` chiếc cho một dòng xe rồi trả về CHIẾC ĐẦU TIÊN.
    // Mỗi chiếc thứ 3 là xe cũ (giá thấp hơn) để minh hoạ giá khác nhau theo tình trạng.
    private CarItem stock(Car car, String vinPrefix, double price, int count, int year,
                          String color, CarStatus firstStatus) {
        CarItem first = null;
        for (int i = 1; i <= count; i++) {
            CarCondition condition = (i % 3 == 0) ? CarCondition.USED : CarCondition.NEW;
            double itemPrice = condition == CarCondition.USED
                    ? Math.round(price * 0.85)
                    : price;
            CarItem item = carItem(car, vinPrefix + String.format("%02d", i), itemPrice, year, color,
                    condition, i == 1 ? firstStatus : CarStatus.AVAILABLE,
                    condition == CarCondition.USED ? 20000 + i * 5000 : null);
            CarItem saved = carItemRepository.save(item);
            if (first == null) {
                first = saved;
            }
        }
        return first;
    }

    private Customer customer(String fullName, String email, String phone, String address, String nationalId) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setNationalId(nationalId);
        return customer;
    }

    private Employee employee(String fullName, String email, String phone, String position) {
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setPosition(position);
        return employee;
    }

    private SalesOrder order(String code, LocalDateTime date, OrderStatus status,
                             Customer customer, Employee employee, List<OrderItem> items) {
        SalesOrder order = new SalesOrder();
        order.setOrderCode(code);
        order.setOrderDate(date);
        order.setStatus(status);
        order.setCustomer(customer);
        order.setEmployee(employee);
        double total = 0.0;
        for (OrderItem item : items) {
            item.setOrder(order);
            total += item.getQuantity() * item.getUnitPrice();
        }
        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(total);
        return order;
    }

    // Mỗi dòng đơn là một chiếc xe vật lý nên quantity luôn = 1
    private OrderItem item(CarItem carItem, int quantity) {
        OrderItem item = new OrderItem();
        item.setCarItem(carItem);
        item.setQuantity(1);
        item.setUnitPrice(carItem.getPrice());
        return item;
    }

    private Payment payment(SalesOrder order, double amount, PaymentMethod method,
                            PaymentStatus status, LocalDateTime paidAt) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setPaidAt(paidAt);
        return payment;
    }

    private Contract contract(SalesOrder order, String number, LocalDateTime signedAt, String content) {
        Contract contract = new Contract();
        contract.setOrder(order);
        contract.setContractNumber(number);
        contract.setSignedAt(signedAt);
        contract.setContent(content);
        return contract;
    }

    private Invoice invoice(SalesOrder order, String number, LocalDateTime issuedAt, double total) {
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(number);
        invoice.setIssuedAt(issuedAt);
        invoice.setTotal(total);
        return invoice;
    }

    private TestDriveAppointment appointment(LocalDateTime scheduledAt, AppointmentStatus status, String note,
                                             Customer customer, CarItem carItem, Employee employee) {
        TestDriveAppointment appointment = new TestDriveAppointment();
        appointment.setScheduledAt(scheduledAt);
        appointment.setStatus(status);
        appointment.setNote(note);
        appointment.setCustomer(customer);
        appointment.setCarItem(carItem);
        appointment.setEmployee(employee);
        return appointment;
    }

    // ============================================================
    // Helpers — MongoDB documents
    // ============================================================

    private CarReview review(Long carId, Long customerId, String customerName, int rating, String title,
                             String content, LocalDateTime createdAt, List<CarReview.ReviewReply> replies) {
        CarReview review = new CarReview();
        review.setCarId(carId);
        review.setCustomerId(customerId);
        review.setCustomerName(customerName);
        review.setRating(rating);
        review.setTitle(title);
        review.setContent(content);
        review.setCreatedAt(createdAt);
        review.setReplies(new ArrayList<>(replies));
        return review;
    }

    private CarReview.ReviewReply reply(String author, String content, LocalDateTime createdAt) {
        return new CarReview.ReviewReply(author, content, createdAt);
    }

    private ActivityLog activity(String action, String entityType, Long entityId, String description,
                                 String performedBy, LocalDateTime timestamp) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setAction(action);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);
        activityLog.setDescription(description);
        activityLog.setPerformedBy(performedBy);
        activityLog.setTimestamp(timestamp);
        return activityLog;
    }

    // ============================================================
    // Helpers — chung
    // ============================================================

    private static Long carId(List<Car> cars, int index) {
        return cars.get(index % cars.size()).getId();
    }

    private static LocalDateTime dt(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }
}
