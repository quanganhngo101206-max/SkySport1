package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Order_status_history")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    private Integer oldStatus;

    @Column(nullable = false)
    private Integer newStatus;

    private String note;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        if (createDate == null) {
            createDate = LocalDateTime.now();
        }
    }
}
