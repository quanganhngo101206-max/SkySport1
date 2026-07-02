package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Bill_detail")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BillDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id")
    private ProductDetail productDetail;

    @Column(name = "product_name_snapshot", length = 100)
    private String productNameSnapshot;

    @Column(name = "color_snapshot", length = 50)
    private String colorSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    // BigDecimal thay Double — tránh lỗi làm tròn tiền tệ
    @Column(name = "price_snapshot", precision = 18, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "return_quantity")
    @Builder.Default
    private Integer returnQuantity = 0;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;
}
