package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Customer_discount",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_CustomerDiscount",
                columnNames = {"customer_id", "discount_code_id"}
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id", nullable = false)
    private DiscountCode discountCode;

    // FK thêm sau khi Bill tạo xong (ALTER TABLE ở DB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Column(name = "used_date")
    private LocalDateTime usedDate;

    @PrePersist
    protected void onCreate() {
        if (usedDate == null) usedDate = LocalDateTime.now();
    }
}
