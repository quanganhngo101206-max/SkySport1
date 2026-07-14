package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Lưu ý: KHÔNG còn unique constraint (customer_id, discount_code_id) nữa —
// từ khi có maxUsagePerCustomer, 1 khách có thể dùng cùng 1 mã nhiều lần
// (mỗi lần dùng là 1 dòng CustomerDiscount riêng), giới hạn số lần được
// kiểm tra bằng COUNT ở DiscountCodeService, không phải bằng ràng buộc DB.
@Entity
@Table(name = "Customer_discount")
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
