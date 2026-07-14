package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Discount_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;               // WELCOME10, GIAM50K...

    @Column(length = 100)
    private String name;

    // 1: Cố định, 2: Phần trăm
    @Column(name = "discount_type", nullable = false)
    private Integer discountType;

    // BigDecimal thay Double — tránh lỗi làm tròn tiền tệ
    @Column(name = "discount_value", precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "max_discount_value", precision = 18, scale = 2)
    private BigDecimal maxDiscountValue;

    @Column(name = "quantity")
    private Integer quantity;

    // 0: tất cả khách hàng, 1: chỉ khách mới (chưa có đơn hoàn thành), 2: chỉ khách VIP
    @Column(name = "applicable_customer_group")
    @Builder.Default
    private Integer applicableCustomerGroup = 0;

    // Số lần tối đa 1 khách hàng được dùng mã này. null = không giới hạn
    // (khách dùng bao nhiêu lần cũng được, miễn còn lượt chung của mã).
    // Mặc định 1 để giữ hành vi cũ (mỗi khách chỉ dùng được 1 lần).
    @Column(name = "max_usage_per_customer")
    @Builder.Default
    private Integer maxUsagePerCustomer = 1;

    // Danh sách sản phẩm được áp dụng mã này. Rỗng = áp dụng cho TẤT CẢ sản
    // phẩm (giữ hành vi mặc định cũ). Nếu có ít nhất 1 sản phẩm, mã chỉ giảm
    // giá trên các sản phẩm nằm trong danh sách, sản phẩm khác trong cùng đơn
    // giữ nguyên giá.
    @ManyToMany
    @JoinTable(
            name = "discount_code_product",
            joinColumns = @JoinColumn(name = "discount_code_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private java.util.List<Product> applicableProducts = new java.util.ArrayList<>();

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    @Builder.Default
    private Integer status = 1;

    @Column(name = "delete_flag")
    @Builder.Default
    private Boolean deleteFlag = false;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        updateDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }
}