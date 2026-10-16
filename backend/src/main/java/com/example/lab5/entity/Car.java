package com.example.lab5.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Dòng xe / phiên bản (sản phẩm) — vd "Toyota Camry 2.0G".
 *
 * <p>Sau khi tách bảng: {@code cars} chỉ giữ thông tin sản phẩm (tên, mô tả, giá niêm yết,
 * dòng xe). Trạng thái và số lượng KHÔNG còn nằm ở đây — mỗi chiếc xe vật lý là một
 * {@link CarItem} riêng, giữ VIN / màu / năm / tình trạng mới-cũ / giá / trạng thái.
 */
@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // Giá niêm yết (tham khảo) của dòng xe; giá bán thực tế nằm ở từng car_item.
    @Column(name = "base_price", nullable = false)
    private Double basePrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CarModel model;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CarItem> items = new ArrayList<>();
}
