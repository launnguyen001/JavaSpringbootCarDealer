package com.example.lab5.repository;

import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SalesOrder o WHERE o.id = :id")
    Optional<SalesOrder> findByIdForUpdate(@Param("id") Long id);

    List<SalesOrder> findByCustomer_IdOrderByOrderDateDesc(Long customerId);

    List<SalesOrder> findByStatusOrderByOrderDateDesc(OrderStatus status);

    List<SalesOrder> findAllByOrderByOrderDateDesc();

    boolean existsByCustomer_Id(Long customerId);

    boolean existsByEmployee_Id(Long employeeId);

    // ---- Đơn hàng đủ điều kiện gắn chứng từ (dùng cho dropdown ở form admin) ----

    /** Đơn chưa bị hủy và chưa có hóa đơn. */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status <> com.example.lab5.entity.OrderStatus.CANCELLED
              AND NOT EXISTS (SELECT i.id FROM Invoice i WHERE i.order = o)
            ORDER BY o.orderDate DESC
            """)
    List<SalesOrder> findOptionsForInvoice();

    /** Đơn chưa bị hủy và chưa có hợp đồng. */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status <> com.example.lab5.entity.OrderStatus.CANCELLED
              AND NOT EXISTS (SELECT c.id FROM Contract c WHERE c.order = o)
            ORDER BY o.orderDate DESC
            """)
    List<SalesOrder> findOptionsForContract();

    /** Mọi đơn chưa bị hủy (dùng cho form thanh toán). */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.customer
            WHERE o.status <> com.example.lab5.entity.OrderStatus.CANCELLED
            ORDER BY o.orderDate DESC
            """)
    List<SalesOrder> findActiveOrderOptions();
}
