package com.example.lab5.repository;

import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByOrderCode(String orderCode);

    /** MariaDB hỗ trợ FOR UPDATE nhưng không hỗ trợ biến thể FOR UPDATE OF alias do Hibernate sinh ra. */
    @Query(value = "SELECT * FROM sales_orders WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<SalesOrder> findByIdForUpdate(@Param("id") Long id);

    List<SalesOrder> findByCustomer_IdOrderByOrderDateDescIdDesc(Long customerId);

    List<SalesOrder> findByStatusOrderByOrderDateDescIdDesc(OrderStatus status);

    /** Đơn hàng mới tạo gần nhất đứng đầu; id DESC xử lý trường hợp trùng thời điểm. */
    List<SalesOrder> findAllByOrderByOrderDateDescIdDesc();

    boolean existsByCustomer_Id(Long customerId);

    boolean existsByEmployee_Id(Long employeeId);

    // ---- Đơn hàng đủ điều kiện gắn chứng từ (dùng cho dropdown ở form admin) ----

    /** Đơn chưa bị hủy và chưa có hóa đơn. */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status = com.example.lab5.entity.OrderStatus.CONFIRMED
              AND NOT EXISTS (SELECT i.id FROM Invoice i WHERE i.order = o)
            ORDER BY o.orderDate DESC, o.id DESC
            """)
    List<SalesOrder> findOptionsForInvoice();

    /** Đơn chưa bị hủy và chưa có hợp đồng. */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status = com.example.lab5.entity.OrderStatus.CONFIRMED
              AND NOT EXISTS (SELECT c.id FROM Contract c WHERE c.order = o)
            ORDER BY o.orderDate DESC, o.id DESC
            """)
    List<SalesOrder> findOptionsForContract();

    /** Chỉ đơn CONFIRMED (dùng cho form thanh toán). */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status = com.example.lab5.entity.OrderStatus.CONFIRMED
            ORDER BY o.orderDate DESC, o.id DESC
            """)
    List<SalesOrder> findActiveOrderOptions();
}
