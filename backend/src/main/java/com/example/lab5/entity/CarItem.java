package com.example.lab5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Một chiếc xe vật lý trong kho (đơn vị bán thực tế).
 *
 * <p>Giữ những thông tin chỉ đúng cho từng chiếc: VIN, màu, năm sản xuất, tình trạng mới/cũ,
 * giá bán, trạng thái (AVAILABLE / RESERVED / SOLD / MAINTENANCE), số km đã đi (xe cũ) và ghi chú.
 */
@Entity
@Table(name = "car_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Car car;

    @Column(unique = true, length = 50)
    private String vin;

    @Column(length = 50)
    private String color;

    @Column(name = "model_year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_condition", nullable = false, length = 20)
    private CarCondition condition = CarCondition.NEW;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status = CarStatus.AVAILABLE;

    // Số km đã đi — chỉ có ý nghĩa với xe cũ (condition = USED)
    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(length = 500)
    private String note;
}
