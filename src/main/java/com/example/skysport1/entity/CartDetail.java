package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Cart_detail")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CartDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_detail_id", nullable = false)
    private ProductDetail productDetail;

    private Integer quantity;
}
