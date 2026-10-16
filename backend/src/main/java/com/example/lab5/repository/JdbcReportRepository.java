package com.example.lab5.repository;

import com.example.lab5.dto.CarInventoryItem;
import com.example.lab5.dto.CarSalesOrderLine;
import com.example.lab5.dto.DashboardSummary;
import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.InventoryByModel;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.OrderSummary;
import com.example.lab5.dto.RevenueByMonth;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

// Báo cáo/thống kê dùng SQL thuần (JdbcTemplate) - theo mẫu JdbcCategoryRepository
// Lưu ý: sau khi tách bảng, số liệu tồn kho/"xe" đếm theo car_items (từng chiếc vật lý).
@Repository
public class JdbcReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TopSellingCar> findTopSellingCars(int limit) {
        String sql = """
                SELECT c.id AS car_id,
                       c.name AS car_name,
                       SUM(oi.quantity) AS total_sold,
                       SUM(oi.quantity * oi.unit_price) AS revenue
                FROM order_items oi
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                GROUP BY c.id, c.name
                ORDER BY total_sold DESC, c.id
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TopSellingCar(
                rs.getLong("car_id"),
                rs.getString("car_name"),
                rs.getLong("total_sold"),
                rs.getDouble("revenue")
        ), limit);
    }

    public List<InventoryByBrand> findInventoryByBrand() {
        String sql = """
                SELECT b.id AS brand_id,
                       b.name AS brand_name,
                       COUNT(DISTINCT c.id) AS total_cars,
                       COUNT(ci.id) AS total_quantity,
                       COALESCE(SUM(CASE WHEN ci.status = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS available_items
                FROM brands b
                LEFT JOIN car_models m ON m.brand_id = b.id
                LEFT JOIN cars c ON c.model_id = m.id
                LEFT JOIN car_items ci ON ci.car_id = c.id
                GROUP BY b.id, b.name
                ORDER BY b.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new InventoryByBrand(
                rs.getLong("brand_id"),
                rs.getString("brand_name"),
                rs.getLong("total_cars"),
                rs.getLong("total_quantity"),
                rs.getLong("available_items")
        ));
    }

    public RevenueSummary findRevenueSummary() {
        String sql = """
                SELECT COUNT(*) AS total_orders,
                       COALESCE(SUM(total_amount), 0) AS total_revenue
                FROM sales_orders
                WHERE status <> 'CANCELLED'
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new RevenueSummary(
                rs.getLong("total_orders"),
                rs.getDouble("total_revenue")
        ));
    }

    public List<OrderStatusCount> countOrdersByStatus() {
        String sql = """
                SELECT status, COUNT(*) AS total
                FROM sales_orders
                GROUP BY status
                ORDER BY status
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrderStatusCount(
                rs.getString("status"),
                rs.getLong("total")
        ));
    }

    // ===================== DASHBOARD & DRILLDOWN =====================

    // D1: tổng hợp chỉ số cho dashboard (đếm theo từng chiếc xe vật lý)
    public DashboardSummary findDashboardSummary() {
        long totalCars = count("SELECT COUNT(*) FROM car_items");
        long available = countItemStatus("AVAILABLE");
        long reserved = countItemStatus("RESERVED");
        long sold = countItemStatus("SOLD");
        long maintenance = countItemStatus("MAINTENANCE");
        long newItems = count("SELECT COUNT(*) FROM car_items WHERE car_condition = 'NEW'");
        long usedItems = count("SELECT COUNT(*) FROM car_items WHERE car_condition = 'USED'");
        long customers = count("SELECT COUNT(*) FROM customers");
        long employees = count("SELECT COUNT(*) FROM employees");
        long orders = count("SELECT COUNT(*) FROM sales_orders");
        long scheduledTestDrives = count("SELECT COUNT(*) FROM test_drive_appointments WHERE status = 'SCHEDULED'");
        double revenue = value("SELECT COALESCE(SUM(total_amount), 0) FROM sales_orders WHERE status <> 'CANCELLED'");

        return new DashboardSummary(totalCars, available, reserved, sold, maintenance, newItems, usedItems,
                customers, employees, orders, scheduledTestDrives, revenue);
    }

    // D2: các dòng xe (model) thuộc 1 hãng + tồn kho (số dòng xe và số chiếc)
    public List<InventoryByModel> findModelsByBrand(Long brandId) {
        String sql = """
                SELECT m.id AS model_id,
                       m.name AS model_name,
                       m.body_type,
                       COUNT(DISTINCT c.id) AS total_cars,
                       COUNT(ci.id) AS total_quantity,
                       COALESCE(SUM(CASE WHEN ci.status = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS available_items
                FROM car_models m
                LEFT JOIN cars c ON c.model_id = m.id
                LEFT JOIN car_items ci ON ci.car_id = c.id
                WHERE m.brand_id = ?
                GROUP BY m.id, m.name, m.body_type
                ORDER BY m.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new InventoryByModel(
                rs.getLong("model_id"),
                rs.getString("model_name"),
                rs.getString("body_type"),
                rs.getLong("total_cars"),
                rs.getLong("total_quantity"),
                rs.getLong("available_items")
        ), brandId);
    }

    // D3: các chiếc xe cụ thể thuộc 1 dòng xe
    public List<CarInventoryItem> findCarsByModel(Long modelId) {
        String sql = """
                SELECT ci.id AS car_id,
                       c.name AS car_name,
                       ci.vin,
                       ci.color,
                       ci.model_year,
                       ci.status,
                       ci.car_condition,
                       ci.mileage_km,
                       ci.price
                FROM car_items ci
                JOIN cars c ON c.id = ci.car_id
                WHERE c.model_id = ?
                ORDER BY ci.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CarInventoryItem(
                rs.getLong("car_id"),
                rs.getString("car_name"),
                rs.getString("vin"),
                rs.getString("color"),
                (Integer) rs.getObject("model_year"),
                rs.getString("status"),
                rs.getString("car_condition"),
                (Integer) rs.getObject("mileage_km"),
                rs.getDouble("price")
        ), modelId);
    }

    // D4: các dòng đơn chứa 1 dòng xe (drilldown từ top-selling-cars)
    public List<CarSalesOrderLine> findOrdersByCar(Long carId) {
        String sql = """
                SELECT o.id AS order_id,
                       o.order_code,
                       o.order_date,
                       o.status,
                       cu.full_name AS customer_name,
                       oi.quantity,
                       oi.unit_price,
                       (oi.quantity * oi.unit_price) AS line_total
                FROM order_items oi
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN sales_orders o ON o.id = oi.order_id
                LEFT JOIN customers cu ON cu.id = o.customer_id
                WHERE ci.car_id = ?
                ORDER BY o.order_date DESC, o.id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CarSalesOrderLine(
                rs.getLong("order_id"),
                rs.getString("order_code"),
                toLocalDateTime(rs, "order_date"),
                rs.getString("status"),
                rs.getString("customer_name"),
                (Integer) rs.getObject("quantity"),
                rs.getDouble("unit_price"),
                rs.getDouble("line_total")
        ), carId);
    }

    // D5: danh sách đơn theo trạng thái
    public List<OrderSummary> findOrdersByStatus(String status) {
        String sql = """
                SELECT o.id AS order_id,
                       o.order_code,
                       o.order_date,
                       o.status,
                       o.total_amount,
                       cu.full_name AS customer_name,
                       e.full_name AS employee_name
                FROM sales_orders o
                LEFT JOIN customers cu ON cu.id = o.customer_id
                LEFT JOIN employees e ON e.id = o.employee_id
                WHERE o.status = ?
                ORDER BY o.order_date DESC, o.id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrderSummary(
                rs.getLong("order_id"),
                rs.getString("order_code"),
                toLocalDateTime(rs, "order_date"),
                rs.getString("status"),
                rs.getDouble("total_amount"),
                rs.getString("customer_name"),
                rs.getString("employee_name")
        ), status);
    }

    // D6: doanh thu theo tháng (nhóm năm/tháng, ghép chuỗi ở Java để tránh khác biệt MySQL/H2)
    public List<RevenueByMonth> findRevenueByMonth() {
        String sql = """
                SELECT EXTRACT(YEAR FROM order_date) AS yr,
                       EXTRACT(MONTH FROM order_date) AS mo,
                       COUNT(*) AS total_orders,
                       COALESCE(SUM(total_amount), 0) AS total_revenue
                FROM sales_orders
                WHERE status <> 'CANCELLED'
                GROUP BY EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date)
                ORDER BY EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date)
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RevenueByMonth(
                String.format("%04d-%02d", rs.getInt("yr"), rs.getInt("mo")),
                rs.getLong("total_orders"),
                rs.getDouble("total_revenue")
        ));
    }

    // --------------------- helpers ---------------------

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0L : result;
    }

    private long countItemStatus(String status) {
        return count("SELECT COUNT(*) FROM car_items WHERE status = ?", status);
    }

    private double value(String sql, Object... args) {
        Double result = jdbcTemplate.queryForObject(sql, Double.class, args);
        return result == null ? 0.0 : result;
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
