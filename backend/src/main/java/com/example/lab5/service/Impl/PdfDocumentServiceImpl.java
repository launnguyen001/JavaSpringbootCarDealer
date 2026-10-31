package com.example.lab5.service.Impl;

import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.ForbiddenException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.service.PdfDocumentService;
import com.example.lab5.util.Formatters;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PdfDocumentServiceImpl implements PdfDocumentService {

    private static final Color GOLD = new Color(197, 142, 0);
    private static final Color DARK = new Color(10, 10, 10);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color LIGHT = new Color(250, 250, 250);
    private static final Color BORDER = new Color(229, 231, 235);

    private static final BaseFont REGULAR = loadFont("/fonts/NotoSans-Regular.ttf", "NotoSans-Regular.ttf");
    private static final BaseFont BOLD = loadFont("/fonts/NotoSans-Bold.ttf", "NotoSans-Bold.ttf");

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public PdfDocumentServiceImpl(
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository
    ) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderContractPdf(Long contractId, String username, boolean staff) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng có id = " + contractId));
        SalesOrder order = contract.getOrder();
        assertCanAccess(order, username, staff);
        documentAccessGuard(order, contract.getContractNumber());
        return render(
                "HỢP ĐỒNG MUA BÁN XE",
                "Số hợp đồng: " + contract.getContractNumber(),
                contract.getSignedAt(),
                order,
                contract.getContent(),
                "Hợp đồng có hiệu lực kể từ ngày ký. Các bên cam kết thực hiện đúng các điều khoản đã thoả thuận."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderInvoicePdf(Long invoiceId, String username, boolean staff) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn có id = " + invoiceId));
        SalesOrder order = invoice.getOrder();
        assertCanAccess(order, username, staff);
        double paid = paidAmount(order);
        return render(
                "HÓA ĐƠN BÁN HÀNG",
                "Số hóa đơn: " + invoice.getInvoiceNumber(),
                invoice.getIssuedAt(),
                order,
                "Tổng tiền hóa đơn: " + Formatters.money(invoice.getTotal())
                        + ". Đã thanh toán: " + Formatters.money(paid)
                        + ". Còn lại: " + Formatters.money(Math.max(invoice.getTotal() - paid, 0)) + ".",
                "Hóa đơn được phát hành bởi hệ thống Car Dealer. Vui lòng giữ hóa đơn để đối chiếu khi cần thiết."
        );
    }

    private byte[] render(
            String title,
            String documentNumber,
            LocalDateTime issuedAt,
            SalesOrder order,
            String note,
            String footerNote
    ) {
        Document document = new Document(PageSize.A4, 42, 42, 44, 48);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, title, documentNumber, issuedAt, order);
            addParties(document, order);
            addItems(document, order.getItems());
            addTotals(document, order, note);
            addSignature(document, footerNote);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo PDF: " + exception.getMessage(), exception);
        }
    }

    private void addHeader(Document document, String title, String documentNumber,
                           LocalDateTime issuedAt, SalesOrder order) {
        PdfPTable header = new PdfPTable(new float[]{62, 38});
        header.setWidthPercentage(100);

        PdfPCell company = cell(
                "CARDEALER\nĐại lý xe uy tín\nHotline: 1900 0000 • cardealer.vn",
                font(BOLD, 11, DARK), Element.ALIGN_LEFT
        );
        company.setBorder(Rectangle.NO_BORDER);
        header.addCell(company);

        PdfPCell meta = cell(
                "Ngày lập: " + Formatters.dateTime(issuedAt)
                        + "\nMã đơn: " + (order == null ? "—" : safe(order.getOrderCode())),
                font(REGULAR, 9, MUTED), Element.ALIGN_RIGHT
        );
        meta.setBorder(Rectangle.NO_BORDER);
        header.addCell(meta);
        document.add(header);

        Paragraph titleParagraph = new Paragraph(title, font(BOLD, 18, DARK));
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingBefore(18);
        document.add(titleParagraph);

        Paragraph number = new Paragraph(documentNumber, font(REGULAR, 10, MUTED));
        number.setAlignment(Element.ALIGN_CENTER);
        number.setSpacingAfter(16);
        document.add(number);
    }

    private void addParties(Document document, SalesOrder order) {
        Customer customer = order.getCustomer();
        Employee employee = order.getEmployee();

        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(12);

        table.addCell(infoBox("KHÁCH HÀNG",
                safe(customer == null ? null : customer.getFullName())
                        + "\nĐiện thoại: " + safe(customer == null ? null : customer.getPhone())
                        + "\nEmail: " + safe(customer == null ? null : customer.getEmail())
                        + "\nĐịa chỉ: " + safe(customer == null ? null : customer.getAddress())
                        + "\nCCCD/CMND: " + safe(customer == null ? null : customer.getNationalId())));
        table.addCell(infoBox("THÔNG TIN ĐƠN HÀNG",
                "Mã đơn: " + safe(order.getOrderCode())
                        + "\nNgày đặt: " + Formatters.dateTime(order.getOrderDate())
                        + "\nNhân viên phụ trách: " + safe(employee == null ? null : employee.getFullName())
                        + "\nTrạng thái: " + order.getStatus()));
        document.add(table);
    }

    private void addItems(Document document, List<OrderItem> items) {
        PdfPTable table = new PdfPTable(new float[]{5, 27, 16, 9, 8, 11, 12, 12});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingBefore(2);

        addHeaderCell(table, "STT");
        addHeaderCell(table, "Tên xe");
        addHeaderCell(table, "VIN");
        addHeaderCell(table, "Màu");
        addHeaderCell(table, "Đời");
        addHeaderCell(table, "Tình trạng");
        addHeaderCell(table, "Đơn giá");
        addHeaderCell(table, "Thành tiền");

        int index = 1;
        for (OrderItem item : items) {
            CarItem car = item.getCarItem();
            String carName = car == null || car.getCar() == null ? "—" : safe(car.getCar().getName());
            double lineTotal = (item.getUnitPrice() == null ? 0 : item.getUnitPrice())
                    * (item.getQuantity() == null ? 0 : item.getQuantity());
            addBodyCell(table, String.valueOf(index++), Element.ALIGN_CENTER);
            addBodyCell(table, carName, Element.ALIGN_LEFT);
            addBodyCell(table, car == null ? "—" : safe(car.getVin()), Element.ALIGN_LEFT);
            addBodyCell(table, car == null ? "—" : safe(car.getColor()), Element.ALIGN_LEFT);
            addBodyCell(table, car == null || car.getYear() == null ? "—" : String.valueOf(car.getYear()), Element.ALIGN_CENTER);
            addBodyCell(table, car == null || car.getCondition() == null
                    ? "—" : (car.getCondition().name().equals("NEW") ? "Mới" : "Cũ"), Element.ALIGN_CENTER);
            addBodyCell(table, Formatters.money(item.getUnitPrice()), Element.ALIGN_RIGHT);
            addBodyCell(table, Formatters.money(lineTotal), Element.ALIGN_RIGHT);
        }
        document.add(table);
    }

    private void addTotals(Document document, SalesOrder order, String note) {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);
        totals.setSpacingBefore(8);

        PdfPCell noteCell = cell(note == null || note.isBlank() ? "" : note, font(REGULAR, 9, MUTED), Element.ALIGN_LEFT);
        noteCell.setBorder(Rectangle.NO_BORDER);
        noteCell.setPaddingTop(6);
        totals.addCell(noteCell);

        PdfPTable amounts = new PdfPTable(new float[]{55, 45});
        amounts.setWidthPercentage(100);
        amounts.addCell(totalLabel("Tổng tiền hàng"));
        amounts.addCell(totalValue(Formatters.money(order.getTotalAmount())));
        double paid = paidAmount(order);
        amounts.addCell(totalLabel("Đã thanh toán"));
        amounts.addCell(totalValue(Formatters.money(paid)));
        amounts.addCell(totalLabel("Còn lại"));
        amounts.addCell(totalValue(Formatters.money(Math.max(order.getTotalAmount() - paid, 0))));

        PdfPCell amountCell = new PdfPCell(amounts);
        amountCell.setBorder(Rectangle.NO_BORDER);
        totals.addCell(amountCell);
        document.add(totals);
    }

    private void addSignature(Document document, String footerNote) {
        Paragraph note = new Paragraph(footerNote, font(REGULAR, 8, MUTED));
        note.setSpacingBefore(18);
        document.add(note);

        PdfPTable signatures = new PdfPTable(new float[]{50, 50});
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(28);

        signatures.addCell(signatureBox("KHÁCH HÀNG", "(Ký, ghi rõ họ tên)"));
        signatures.addCell(signatureBox("ĐẠI DIỆN ĐẠI LÝ", "(Ký, đóng dấu)"));
        document.add(signatures);
    }

    private PdfPCell infoBox(String title, String content) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        PdfPCell titleCell = cell(title, font(BOLD, 8, GOLD), Element.ALIGN_LEFT);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(6);
        inner.addCell(titleCell);
        PdfPCell contentCell = cell(content, font(REGULAR, 9, DARK), Element.ALIGN_LEFT);
        contentCell.setBorder(Rectangle.NO_BORDER);
        inner.addCell(contentCell);

        PdfPCell box = new PdfPCell(inner);
        box.setBackgroundColor(LIGHT);
        box.setBorderColor(BORDER);
        box.setPadding(10);
        return box;
    }

    private PdfPCell signatureBox(String title, String hint) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        PdfPCell titleCell = cell(title, font(BOLD, 9, DARK), Element.ALIGN_CENTER);
        titleCell.setBorder(Rectangle.NO_BORDER);
        inner.addCell(titleCell);
        PdfPCell hintCell = cell(hint, font(REGULAR, 8, MUTED), Element.ALIGN_CENTER);
        hintCell.setBorder(Rectangle.NO_BORDER);
        hintCell.setPaddingTop(4);
        hintCell.setPaddingBottom(44);
        inner.addCell(hintCell);
        PdfPCell box = new PdfPCell(inner);
        box.setBorder(Rectangle.NO_BORDER);
        return box;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = cell(text, font(BOLD, 8, Color.WHITE), Element.ALIGN_CENTER);
        cell.setBackgroundColor(DARK);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = cell(text, font(REGULAR, 8, DARK), alignment);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        table.addCell(cell);
    }

    private PdfPCell totalLabel(String text) {
        PdfPCell cell = cell(text, font(REGULAR, 9, MUTED), Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        return cell;
    }

    private PdfPCell totalValue(String text) {
        PdfPCell cell = cell(text, font(BOLD, 10, DARK), Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        return cell;
    }

    private PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER);
        cell.setPadding(5);

        Paragraph paragraph = new Paragraph();
        paragraph.setAlignment(alignment);
        String[] lines = (text == null ? "" : text).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                paragraph.add(Chunk.NEWLINE);
            }
            paragraph.add(new Chunk(lines[i], font));
        }
        cell.addElement(paragraph);
        return cell;
    }

    private double paidAmount(SalesOrder order) {
        if (order == null || order.getId() == null) {
            return 0;
        }
        return paymentRepository.findByOrder_IdOrderByPaidAtDescIdDesc(order.getId()).stream()
                .filter(payment -> payment != null && payment.getStatus() != null
                        && (payment.getStatus().name().equals("PAID")
                        || payment.getStatus().name().equals("PARTIAL")))
                .mapToDouble(payment -> payment.getAmount() == null ? 0.0 : payment.getAmount())
                .sum();
    }

    private void documentAccessGuard(SalesOrder order, String documentNumber) {
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của chứng từ " + documentNumber);
        }
    }

    private void assertCanAccess(SalesOrder order, String username, boolean staff) {
        if (staff) {
            return;
        }
        boolean owner = order != null
                && order.getCustomer() != null
                && order.getCustomer().getUser() != null
                && username != null
                && username.equals(order.getCustomer().getUser().getUsername());
        if (!owner) {
            throw new ForbiddenException("Bạn không có quyền tải chứng từ này");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private Font font(BaseFont baseFont, float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    private static BaseFont loadFont(String resourcePath, String name) {
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            byte[] bytes = input.readAllBytes();
            return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (Exception exception) {
            throw new IllegalStateException("Không tải được font PDF " + resourcePath + ": "
                    + exception.getMessage(), exception);
        }
    }
}
