package com.example.lab5.service.Impl;

import com.example.lab5.dto.DashboardSummary;
import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.RevenueByMonth;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.service.ReportExportService;
import com.example.lab5.service.ReportService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    private static final String[] MONEY_HEADERS = {"Doanh thu", "Tổng doanh thu", "Giá", "Đơn giá", "Thành tiền"};

    private final ReportService reportService;

    public ReportExportServiceImpl(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public byte[] export(String type, String format) {
        ReportTable table = buildTable(type);
        String normalizedFormat = format == null ? "xlsx" : format.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedFormat) {
            case "xlsx" -> toXlsx(table);
            case "csv" -> toCsv(table);
            default -> throw new BadRequestException("Định dạng xuất không hợp lệ: " + format
                    + " (chỉ hỗ trợ xlsx hoặc csv)");
        };
    }

    @Override
    public String suggestedFilename(String type, String format) {
        String extension = format == null || format.isBlank() ? "xlsx" : format.trim().toLowerCase(Locale.ROOT);
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
        return "bao-cao-" + type + "-" + timestamp + "." + extension;
    }

    private ReportTable buildTable(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Loại báo cáo không được để trống");
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "dashboard-summary" -> dashboardSummary();
            case "top-selling-cars" -> topSellingCars();
            case "inventory-by-brand" -> inventoryByBrand();
            case "revenue-summary" -> revenueSummary();
            case "orders-by-status" -> ordersByStatus();
            case "revenue-by-month" -> revenueByMonth();
            default -> throw new BadRequestException("Loại báo cáo không hợp lệ: " + type);
        };
    }

    private ReportTable dashboardSummary() {
        DashboardSummary summary = reportService.getDashboardSummary();
        ReportTable table = new ReportTable("Tổng quan", List.of("Chỉ số", "Giá trị"));
        table.row("Tổng số xe", summary.totalCars());
        table.row("Xe sẵn sàng", summary.availableCars());
        table.row("Xe đã đặt", summary.reservedCars());
        table.row("Xe đã bán", summary.soldCars());
        table.row("Xe đang bảo trì", summary.maintenanceCars());
        table.row("Xe mới", summary.newItems());
        table.row("Xe cũ", summary.usedItems());
        table.row("Khách hàng", summary.totalCustomers());
        table.row("Nhân viên", summary.totalEmployees());
        table.row("Đơn hàng", summary.totalOrders());
        table.row("Lịch lái thử đã lên", summary.scheduledTestDrives());
        table.row("Tổng doanh thu", summary.totalRevenue());
        return table;
    }

    private ReportTable topSellingCars() {
        ReportTable table = new ReportTable("Xe bán chạy", List.of("Mã xe", "Tên xe", "Số lượng bán", "Doanh thu"));
        for (TopSellingCar car : reportService.getTopSellingCars(100)) {
            table.row(car.carId(), car.carName(), car.totalSold(), car.revenue());
        }
        return table;
    }

    private ReportTable inventoryByBrand() {
        ReportTable table = new ReportTable("Tồn kho theo hãng",
                List.of("Mã hãng", "Hãng", "Số dòng xe", "Tổng số xe", "Xe sẵn sàng"));
        for (InventoryByBrand brand : reportService.getInventoryByBrand()) {
            table.row(brand.brandId(), brand.brandName(), brand.totalCars(),
                    brand.totalQuantity(), brand.availableItems());
        }
        return table;
    }

    private ReportTable revenueSummary() {
        RevenueSummary summary = reportService.getRevenueSummary();
        ReportTable table = new ReportTable("Doanh thu", List.of("Chỉ số", "Giá trị"));
        table.row("Tổng số đơn hàng", summary.totalOrders());
        table.row("Tổng doanh thu", summary.totalRevenue());
        return table;
    }

    private ReportTable ordersByStatus() {
        ReportTable table = new ReportTable("Đơn theo trạng thái", List.of("Trạng thái", "Số lượng"));
        for (OrderStatusCount count : reportService.getOrderStatusCount()) {
            table.row(count.status(), count.total());
        }
        return table;
    }

    private ReportTable revenueByMonth() {
        ReportTable table = new ReportTable("Doanh thu theo tháng",
                List.of("Tháng", "Số đơn hàng", "Doanh thu"));
        for (RevenueByMonth item : reportService.getRevenueByMonth()) {
            table.row(item.period(), item.totalOrders(), item.totalRevenue());
        }
        return table;
    }

    private byte[] toCsv(ReportTable table) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        appendCsvRow(builder, table.headers());
        for (List<Object> row : table.rows()) {
            appendCsvRow(builder, row);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder builder, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values.get(i)));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private byte[] toXlsx(ReportTable table) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(safeSheetName(table.name()));
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = moneyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (List<Object> data : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < data.size(); i++) {
                    Object value = data.get(i);
                    Cell cell = row.createCell(i);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                        if (isMoneyColumn(table.headers().get(i)) || (data.size() == 2 && rowIndex > 1
                                && table.headers().get(0).equals("Chỉ số")
                                && String.valueOf(data.get(0)).toLowerCase(Locale.ROOT).contains("doanh thu"))) {
                            cell.setCellStyle(moneyStyle);
                        }
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }

            for (int i = 0; i < table.headers().size(); i++) {
                sheet.setColumnWidth(i, Math.min(50, Math.max(14, table.headers().get(i).length() + 6)) * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo file Excel: " + exception.getMessage(), exception);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private boolean isMoneyColumn(String header) {
        for (String moneyHeader : MONEY_HEADERS) {
            if (header.toLowerCase(Locale.ROOT).contains(moneyHeader.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private static final class ReportTable {
        private final String name;
        private final List<String> headers;
        private final List<List<Object>> rows = new ArrayList<>();

        private ReportTable(String name, List<String> headers) {
            this.name = name;
            this.headers = headers;
        }

        private void row(Object... values) {
            rows.add(List.of(values));
        }

        private String name() {
            return name;
        }

        private List<String> headers() {
            return headers;
        }

        private List<List<Object>> rows() {
            return rows;
        }
    }
}
