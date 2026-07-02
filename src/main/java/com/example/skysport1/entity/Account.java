package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "pass_word", nullable = false, length = 255)
    private String password;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "status")
    @Builder.Default
    private Integer status = 1;

    @Column(name = "is_non_locked")
    @Builder.Default
    private Boolean isNonLocked = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

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