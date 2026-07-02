package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Bill_return")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillReturn {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "return_money", precision = 18, scale = 0)
    private BigDecimal returnMoney;

    @Column(name = "percent_fee", precision = 5, scale = 0)
    @Builder.Default
    private BigDecimal percentFee = BigDecimal.ZERO;

    @Column(name = "is_cancel")
    @Builder.Default
    private Boolean isCancel = false;

    // 1:Chờ hoàn 2:Đã hoàn tiền
    @Column(name = "return_status")
    @Builder.Default
    private Integer returnStatus = 1;

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
