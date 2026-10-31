package com.example.lab5.repository;

import com.example.lab5.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcDashboardRepository {
    private final JdbcTemplate jdbc;

    public JdbcDashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardData findDashboard(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(ChronoUnit.DAYS.between(from, to));

        long total = countInventory(null, brandId, modelId);
        long available = countInventory("AVAILABLE", brandId, modelId);
        long reserved = countInventory("RESERVED", brandId, modelId);
        long sold = countInventory("SOLD", brandId, modelId);
        long maintenance = countInventory("MAINTENANCE", brandId, modelId);
        long newItems = countCondition("NEW", brandId, modelId);
        long usedItems = countCondition("USED", brandId, modelId);
        long soldPeriod = countSold(from, to, brandId, modelId);
        long previousSold = countSold(previousFrom, previousTo, brandId, modelId);
        double revenue = revenue(from, to, brandId, modelId);
        double previousRevenue = revenue(previousFrom, previousTo, brandId, modelId);

        return new DashboardData(total, available, reserved, sold, maintenance, newItems, usedItems,
                soldPeriod, previousSold, revenue, previousRevenue,
                findOrderStatuses(from, to, brandId, modelId),
                findRevenueByMonth(from, to, brandId, modelId),
                findRecentOrders(from, to, brandId, modelId),
                findTopSellingCars(5, from, to, brandId, modelId));
    }

    public List<CarInventoryItem> findInventoryCars(String status, LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT ci.id AS car_id, c.name AS car_name, ci.vin, ci.color,
                       ci.model_year, ci.status, ci.car_condition, ci.mileage_km, ci.price
                FROM car_items ci
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE (? IS NULL
                    OR (? = 'SOLD' AND EXISTS (
                      SELECT 1 FROM order_items oi JOIN sales_orders o ON o.id = oi.order_id
                      WHERE oi.car_item_id = ci.id AND o.status = 'COMPLETED'
                        AND o.order_date >= ? AND o.order_date < ?
                    ))
                    OR (? <> 'SOLD' AND ci.status = ?))
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                ORDER BY ci.id DESC
                """;
        return jdbc.query(sql, (rs, i) -> new CarInventoryItem(
                rs.getLong("car_id"), rs.getString("car_name"), rs.getString("vin"),
                rs.getString("color"), (Integer) rs.getObject("model_year"),
                rs.getString("status"), rs.getString("car_condition"),
                (Integer) rs.getObject("mileage_km"), rs.getDouble("price")),
                status, status, start(from), start(to.plusDays(1)),
                status, status, brandId, brandId, modelId, modelId);
    }

    private long countInventory(String status, Long brandId, Long modelId) {
        String sql = """
                SELECT COUNT(*) FROM car_items ci
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE (? IS NULL OR ci.status = ?)
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                """;
        Long value = jdbc.queryForObject(sql, Long.class,
                status, status, brandId, brandId, modelId, modelId);
        return value == null ? 0 : value;
    }

    private long countCondition(String condition, Long brandId, Long modelId) {
        String sql = """
                SELECT COUNT(*) FROM car_items ci
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE ci.car_condition = ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                """;
        Long value = jdbc.queryForObject(sql, Long.class, condition, brandId, brandId, modelId, modelId);
        return value == null ? 0 : value;
    }

    private long countSold(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT COUNT(DISTINCT ci.id)
                FROM sales_orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE o.status = 'COMPLETED'
                  AND o.order_date >= ? AND o.order_date < ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                """;
        Long value = jdbc.queryForObject(sql, Long.class, start(from), start(to.plusDays(1)),
                brandId, brandId, modelId, modelId);
        return value == null ? 0 : value;
    }

    private double revenue(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
                FROM sales_orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE o.status <> 'CANCELLED'
                  AND o.order_date >= ? AND o.order_date < ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                """;
        Double value = jdbc.queryForObject(sql, Double.class, start(from), start(to.plusDays(1)),
                brandId, brandId, modelId, modelId);
        return value == null ? 0 : value;
    }

    private List<OrderStatusCount> findOrderStatuses(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT o.status, COUNT(*) AS total FROM sales_orders o
                WHERE o.order_date >= ? AND o.order_date < ?
                  AND EXISTS (
                    SELECT 1 FROM order_items oi
                    JOIN car_items ci ON ci.id = oi.car_item_id
                    JOIN cars c ON c.id = ci.car_id
                    JOIN car_models m ON m.id = c.model_id
                    WHERE oi.order_id = o.id
                      AND (? IS NULL OR m.brand_id = ?)
                      AND (? IS NULL OR m.id = ?)
                  )
                GROUP BY o.status ORDER BY o.status
                """;
        return jdbc.query(sql, (rs, i) -> new OrderStatusCount(rs.getString("status"), rs.getLong("total")),
                start(from), start(to.plusDays(1)), brandId, brandId, modelId, modelId);
    }

    private List<RevenueByMonth> findRevenueByMonth(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT EXTRACT(YEAR FROM o.order_date) AS yr,
                       EXTRACT(MONTH FROM o.order_date) AS mo,
                       COUNT(DISTINCT o.id) AS total_orders,
                       COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS total_revenue
                FROM sales_orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE o.status <> 'CANCELLED'
                  AND o.order_date >= ? AND o.order_date < ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                GROUP BY EXTRACT(YEAR FROM o.order_date), EXTRACT(MONTH FROM o.order_date)
                ORDER BY yr, mo
                """;
        List<RevenueByMonth> actual = jdbc.query(sql, (rs, i) -> new RevenueByMonth(
                String.format("%04d-%02d", rs.getInt("yr"), rs.getInt("mo")),
                rs.getLong("total_orders"), rs.getDouble("total_revenue")),
                start(from), start(to.plusDays(1)), brandId, brandId, modelId, modelId);

        // Giữ đủ các mốc tháng trong khoảng lọc, kể cả tháng chưa phát sinh doanh thu.
        Map<String, RevenueByMonth> byPeriod = new HashMap<>();
        for (RevenueByMonth item : actual) {
            byPeriod.put(item.period(), item);
        }

        List<RevenueByMonth> result = new ArrayList<>();
        YearMonth current = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        while (!current.isAfter(last)) {
            String period = current.toString();
            result.add(byPeriod.getOrDefault(period, new RevenueByMonth(period, 0L, 0.0)));
            current = current.plusMonths(1);
        }
        return result;
    }

    private List<OrderSummary> findRecentOrders(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT o.id AS order_id, o.order_code, o.order_date, o.status, o.total_amount,
                       cu.full_name AS customer_name, e.full_name AS employee_name
                FROM sales_orders o
                LEFT JOIN customers cu ON cu.id = o.customer_id
                LEFT JOIN employees e ON e.id = o.employee_id
                WHERE o.order_date >= ? AND o.order_date < ?
                  AND EXISTS (
                    SELECT 1 FROM order_items oi
                    JOIN car_items ci ON ci.id = oi.car_item_id
                    JOIN cars c ON c.id = ci.car_id
                    JOIN car_models m ON m.id = c.model_id
                    WHERE oi.order_id = o.id
                      AND (? IS NULL OR m.brand_id = ?)
                      AND (? IS NULL OR m.id = ?)
                  )
                ORDER BY o.order_date DESC, o.id DESC LIMIT 8
                """;
        return jdbc.query(sql, (rs, i) -> new OrderSummary(
                rs.getLong("order_id"), rs.getString("order_code"), rs.getTimestamp("order_date").toLocalDateTime(),
                rs.getString("status"), rs.getDouble("total_amount"), rs.getString("customer_name"),
                rs.getString("employee_name")),
                start(from), start(to.plusDays(1)), brandId, brandId, modelId, modelId);
    }

    private List<TopSellingCar> findTopSellingCars(int limit, LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT c.id AS car_id, c.name AS car_name,
                       SUM(oi.quantity) AS total_sold,
                       SUM(oi.quantity * oi.unit_price) AS revenue
                FROM order_items oi
                JOIN sales_orders o ON o.id = oi.order_id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE o.status <> 'CANCELLED'
                  AND o.order_date >= ? AND o.order_date < ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                GROUP BY c.id, c.name
                ORDER BY total_sold DESC, c.id LIMIT ?
                """;
        return jdbc.query(sql, (rs, i) -> new TopSellingCar(
                rs.getLong("car_id"), rs.getString("car_name"),
                rs.getLong("total_sold"), rs.getDouble("revenue")),
                start(from), start(to.plusDays(1)), brandId, brandId, modelId, modelId, limit);
    }

    private static Timestamp start(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }
}
