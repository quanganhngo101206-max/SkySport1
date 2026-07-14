package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "gender")
    private Boolean gender;

    @Column(name = "dob")
    private LocalDate dob;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "status")
    @Builder.Default
    private Integer status = 1;

    // Cờ VIP gán thủ công bởi admin — dùng để lọc điều kiện áp dụng mã giảm
    // giá theo nhóm khách hàng (mục "chỉ áp dụng cho khách VIP").
    @Column(name = "is_vip")
    @Builder.Default
    private Boolean isVip = false;

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