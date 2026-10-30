package com.example.lab5.service;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentMethod;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentExportServiceTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PdfDocumentService pdfDocumentService;

    @Autowired
    private ReportExportService reportExportService;

    private Long contractId;
    private Long invoiceId;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setName("Toyota");
        brand.setCountry("Nhật Bản");
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry 2.0G");
        model.setBrand(brand);
        entityManager.persist(model);

        Car car = new Car();
        car.setName("Toyota Camry 2.0G");
        car.setBasePrice(1_200_000_000d);
        car.setModel(model);
        entityManager.persist(car);

        CarItem item = new CarItem();
        item.setCar(car);
        item.setVin("TESTVIN00000000001");
        item.setColor("Đen");
        item.setYear(2025);
        item.setCondition(CarCondition.NEW);
        item.setPrice(1_150_000_000d);
        item.setStatus(CarStatus.SOLD);
        entityManager.persist(item);

        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn Khách");
        customer.setEmail("khach@example.com");
        customer.setPhone("0901234567");
        customer.setAddress("Quận 1, TP. Hồ Chí Minh");
        customer.setNationalId("079123456789");
        entityManager.persist(customer);

        SalesOrder order = new SalesOrder();
        order.setOrderCode("ORD-EXPORT-001");
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);
        order.setTotalAmount(1_150_000_000d);
        order.setCustomer(customer);

        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(1_150_000_000d);
        orderItem.setOrder(order);
        orderItem.setCarItem(item);
        order.getItems().add(orderItem);
        entityManager.persist(order);

        Contract contract = new Contract();
        contract.setContractNumber("HD-EXPORT-001");
        contract.setSignedAt(LocalDateTime.now());
        contract.setContent("Hai bên thống nhất mua bán xe Toyota Camry 2.0G, giá 1.150.000.000 VNĐ.");
        contract.setOrder(order);
        entityManager.persist(contract);
        contractId = contract.getId();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("HĐ-EXPORT-001");
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setTotal(1_150_000_000d);
        invoice.setOrder(order);
        entityManager.persist(invoice);
        invoiceId = invoice.getId();

        Payment payment = new Payment();
        payment.setAmount(500_000_000d);
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setOrder(order);
        entityManager.persist(payment);

        entityManager.flush();
    }

    @Test
    void rendersContractPdfWithVietnameseContent() {
        byte[] pdf = pdfDocumentService.renderContractPdf(contractId, "admin", true);

        assertThat(pdf).hasSizeGreaterThan(1_000);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void rendersInvoicePdfWithPaymentSummary() {
        byte[] pdf = pdfDocumentService.renderInvoicePdf(invoiceId, "admin", true);

        assertThat(pdf).hasSizeGreaterThan(1_000);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void exportsEveryReportTypeToXlsxAndCsv() {
        List<String> types = List.of(
                "dashboard-summary",
                "top-selling-cars",
                "inventory-by-brand",
                "revenue-summary",
                "orders-by-status",
                "revenue-by-month"
        );

        for (String type : types) {
            byte[] xlsx = reportExportService.export(type, "xlsx");
            assertThat(xlsx).as("xlsx %s", type).hasSizeGreaterThan(100);
            assertThat(new String(xlsx, 0, 2, StandardCharsets.US_ASCII)).as("zip header %s", type)
                    .isEqualTo("PK");

            byte[] csv = reportExportService.export(type, "csv");
            assertThat(csv).as("csv %s", type).isNotEmpty();
            assertThat(new String(csv, StandardCharsets.UTF_8)).contains("﻿");
        }
    }

    @Test
    void exportsCsvWithReadableHeaderAndValues() {
        String csv = new String(reportExportService.export("revenue-summary", "csv"), StandardCharsets.UTF_8);

        assertThat(csv).contains("Chỉ số", "Giá trị", "Tổng doanh thu");
        assertThat(reportExportService.suggestedFilename("revenue-summary", "csv"))
                .startsWith("bao-cao-revenue-summary-")
                .endsWith(".csv");
    }
}
