package com.example.skysport1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Supplier {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 100, message = "Tên nhà cung cấp tối đa 100 ký tự")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Pattern(regexp = "^$|^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ (vd: 0912345678)")
    @Column(name = "phone", length = 20)
    private String phone;

    @Pattern(regexp = "^$|^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", message = "Email không hợp lệ")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

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