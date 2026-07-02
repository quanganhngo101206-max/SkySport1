package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Wishlist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
    }
}
