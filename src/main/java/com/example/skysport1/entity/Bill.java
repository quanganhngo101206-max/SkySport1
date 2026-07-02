package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Bill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Guest checkout: customer_id NULL thì guest_email NOT NULL (CHECK ở DB)
    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // 1: Online, 2: Tại quầy
    @Column(name = "invoice_type")
    @Builder.Default
    private Integer invoiceType = 1;

    @Column(name = "subtotal", precision = 18, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", precision = 18, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 0)
    private BigDecimal totalAmount;

    // 1:Chờ xác nhận 2:Đã xác nhận 3:Đang giao 4:Đã giao 5:Đã hủy 6:Hoàn trả 7:Hoàn thành
    @Column(name = "status")
    @Builder.Default
    private Integer status = 1;

    // 0:Chưa thanh toán 1:Đã thanh toán 2:Hoàn tiền
    @Column(name = "payment_status")
    @Builder.Default
    private Integer paymentStatus = 0;

    @Column(name = "note", length = 500)
    private String note;

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

    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY)
    private List<BillDetail> billDetails;
}