package com.example.lab5.repository;

import com.example.lab5.dto.InventoryByBrand;
import com.example.lab5.dto.OrderStatusCount;
import com.example.lab5.dto.RevenueSummary;
import com.example.lab5.dto.TopSellingCar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

// Báo cáo/thống kê dùng SQL thuần (JdbcTemplate) - theo mẫu JdbcCategoryRepository
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
                JOIN cars c ON c.id = oi.car_id
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
                       COUNT(c.id) AS total_cars,
                       COALESCE(SUM(c.quantity), 0) AS total_quantity
                FROM brands b
                LEFT JOIN car_models m ON m.brand_id = b.id
                LEFT JOIN cars c ON c.model_id = m.id
                GROUP BY b.id, b.name
                ORDER BY b.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new InventoryByBrand(
                rs.getLong("brand_id"),
                rs.getString("brand_name"),
                rs.getLong("total_cars"),
                rs.getLong("total_quantity")
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
}
