package com.example.lab5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi dòng đơn là MỘT chiếc xe vật lý nên quantity luôn = 1.
    // Giữ cột lại để các câu SQL báo cáo hiện có (SUM(oi.quantity)) không phải đổi.
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SalesOrder order;

    // Trỏ tới chiếc xe cụ thể (car_items) thay vì dòng xe (cars)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CarItem carItem;
}
