package com.example.skysport1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "Color")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Color {

    @Id
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Vui lòng nhập tên màu")
    @Size(max = 50, message = "Tên màu tối đa 50 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng nhập mã màu")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Mã màu không hợp lệ (VD: #FF0000)")
    private String hexCode;
    private Integer status;
    private Boolean deleteFlag;
}