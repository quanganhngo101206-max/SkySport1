package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment_transaction")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    // BigDecimal thay Double — tránh lỗi làm tròn tiền tệ
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // VNPAY, MOMO, ZALOPAY, CASH, TRANSFER
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    // 0:Pending 1:Success 2:Failed 3:Refunded
    @Column(name = "payment_status")
    @Builder.Default
    private Integer paymentStatus = 0;

    @Column(name = "gateway_response", columnDefinition = "NVARCHAR(MAX)")
    private String gatewayResponse;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "create_by", length = 20)
    private String createBy;

    @PrePersist
    protected void onCreate() {
        if (paymentDate == null) paymentDate = LocalDateTime.now();
    }
}
