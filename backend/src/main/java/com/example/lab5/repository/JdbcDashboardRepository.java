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
                      SELECT 1 FROM order_items oi
                      JOIN contracts ct ON ct.order_id = oi.order_id
                      WHERE oi.car_item_id = ci.id
                        AND ct.signed_at >= ? AND ct.signed_at < ?
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
                JOIN contracts ct ON ct.order_id = o.id
                JOIN order_items oi ON oi.order_id = o.id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE ct.signed_at >= ? AND ct.signed_at < ?
                  AND (? IS NULL OR m.brand_id = ?)
                  AND (? IS NULL OR m.id = ?)
                """;
        Long value = jdbc.queryForObject(sql, Long.class, start(from), start(to.plusDays(1)),
                brandId, brandId, modelId, modelId);
        return value == null ? 0 : value;
    }

    private double revenue(LocalDate from, LocalDate to, Long brandId, Long modelId) {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE WHEN ? IS NULL AND ? IS NULL
                         THEN o.total_amount
                         ELSE COALESCE(selected_lines.selected_amount, 0)
                    END
                ), 0)
                FROM sales_orders o
                LEFT JOIN (
                    SELECT oi.order_id,
                           SUM(oi.quantity * oi.unit_price) AS selected_amount
                    FROM order_items oi
                    JOIN car_items ci ON ci.id = oi.car_item_id
                    JOIN cars c ON c.id = ci.car_id
                    JOIN car_models m ON m.id = c.model_id
                    WHERE (? IS NULL OR m.brand_id = ?)
                      AND (? IS NULL OR m.id = ?)
                    GROUP BY oi.order_id
                ) selected_lines ON selected_lines.order_id = o.id
                WHERE o.status IN ('CONFIRMED', 'COMPLETED')
                  AND COALESCE(o.confirmed_at, o.order_date) >= ?
                  AND COALESCE(o.confirmed_at, o.order_date) < ?
                """;
        Double value = jdbc.queryForObject(sql, Double.class,
                brandId, modelId, brandId, brandId, modelId, modelId,
                start(from), start(to.plusDays(1)));
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
        // Cả ba chỉ số được quy về tháng phát sinh của đơn: order_date với PENDING,
        // confirmed_at (fallback order_date) với CONFIRMED/COMPLETED.
        // Thực nhận là tổng Payments của nhóm đơn đó, bất kể paid_at.
        // Khi lọc hãng/model, phân bổ tiền thu theo tỷ trọng giá trị dòng xe trong đơn.
        String sql = """
                WITH order_line_totals AS (
                    SELECT oi.order_id,
                           SUM(oi.quantity * oi.unit_price) AS all_items_amount,
                           SUM(CASE
                               WHEN (? IS NULL OR m.brand_id = ?)
                                AND (? IS NULL OR m.id = ?)
                               THEN oi.quantity * oi.unit_price ELSE 0
                           END) AS selected_items_amount
                    FROM order_items oi
                    JOIN car_items ci ON ci.id = oi.car_item_id
                    JOIN cars c ON c.id = ci.car_id
                    JOIN car_models m ON m.id = c.model_id
                    GROUP BY oi.order_id
                ),
                eligible_orders AS (
                    SELECT o.id,
                           CASE WHEN o.status = 'PENDING'
                                THEN o.order_date
                                ELSE COALESCE(o.confirmed_at, o.order_date)
                           END AS revenue_date,
                           o.status,
                           o.total_amount,
                           COALESCE(olt.all_items_amount, 0) AS all_items_amount,
                           COALESCE(olt.selected_items_amount, 0) AS selected_items_amount,
                           (SELECT COALESCE(SUM(p.amount), 0)
                            FROM payments p
                            WHERE p.order_id = o.id
                              AND p.status IN ('PAID', 'PARTIAL')) AS order_collected
                    FROM sales_orders o
                    LEFT JOIN order_line_totals olt ON olt.order_id = o.id
                    WHERE o.status <> 'CANCELLED'
                      AND (CASE WHEN o.status = 'PENDING'
                                THEN o.order_date
                                ELSE COALESCE(o.confirmed_at, o.order_date)
                           END) >= ?
                      AND (CASE WHEN o.status = 'PENDING'
                                THEN o.order_date
                                ELSE COALESCE(o.confirmed_at, o.order_date)
                           END) < ?
                ),
                allocated_orders AS (
                    SELECT id, revenue_date, status,
                           CASE WHEN ? IS NULL AND ? IS NULL
                                THEN total_amount
                                ELSE selected_items_amount
                           END AS revenue_amount,
                           CASE
                               WHEN status NOT IN ('CONFIRMED', 'COMPLETED') THEN 0
                               WHEN ? IS NULL AND ? IS NULL THEN order_collected
                               WHEN all_items_amount > 0
                                   THEN order_collected * selected_items_amount / all_items_amount
                               ELSE 0
                           END AS collected
                    FROM eligible_orders
                )
                SELECT EXTRACT(YEAR FROM revenue_date) AS yr,
                       EXTRACT(MONTH FROM revenue_date) AS mo,
                       COUNT(*) AS total_orders,
                       COALESCE(SUM(CASE WHEN status = 'PENDING' THEN revenue_amount ELSE 0 END), 0) AS pending_revenue,
                       COALESCE(SUM(CASE WHEN status IN ('CONFIRMED', 'COMPLETED') THEN revenue_amount ELSE 0 END), 0) AS recognized_revenue,
                       COALESCE(SUM(collected), 0) AS actual_revenue,
                       COALESCE(SUM(CASE
                           WHEN status IN ('CONFIRMED', 'COMPLETED')
                           THEN GREATEST(revenue_amount - collected, 0)
                           ELSE 0
                       END), 0) AS outstanding_amount
                FROM allocated_orders
                GROUP BY EXTRACT(YEAR FROM revenue_date), EXTRACT(MONTH FROM revenue_date)
                ORDER BY yr, mo
                """;
        List<RevenueByMonth> actual = jdbc.query(sql, (rs, i) -> new RevenueByMonth(
                String.format("%04d-%02d", rs.getInt("yr"), rs.getInt("mo")),
                rs.getLong("total_orders"),
                rs.getDouble("pending_revenue"),
                rs.getDouble("recognized_revenue"),
                rs.getDouble("actual_revenue"),
                rs.getDouble("outstanding_amount")),
                brandId, brandId, modelId, modelId,
                start(from), start(to.plusDays(1)),
                brandId, modelId, brandId, modelId);

        Map<String, RevenueByMonth> byPeriod = new HashMap<>();
        for (RevenueByMonth item : actual) byPeriod.put(item.period(), item);

        List<RevenueByMonth> result = new ArrayList<>();
        YearMonth current = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        while (!current.isAfter(last)) {
            String period = current.toString();
            result.add(byPeriod.getOrDefault(period, new RevenueByMonth(period, 0L, 0.0, 0.0, 0.0, 0.0)));
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
                JOIN contracts ct ON ct.order_id = o.id
                JOIN car_items ci ON ci.id = oi.car_item_id
                JOIN cars c ON c.id = ci.car_id
                JOIN car_models m ON m.id = c.model_id
                WHERE o.status <> 'CANCELLED'
                  AND ct.signed_at >= ? AND ct.signed_at < ?
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
