package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // 1 trong 2 sẽ NULL tùy người nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 1000)
    private String content;

    // ORDER_CONFIRMED, ORDER_SHIPPING, ORDER_DELIVERED,
    // RETURN_APPROVED, RETURN_REJECTED, VOUCHER_NEW, LOW_STOCK, NEW_ORDER
    @Column(name = "type", length = 50)
    private String type;

    // ID của Bill / Return_request / Product liên quan
    @Column(name = "reference_id", length = 20)
    private String referenceId;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
    }
}
