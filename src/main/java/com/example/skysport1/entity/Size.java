package com.example.skysport1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "Size")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Size {

    @Id
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Vui lòng nhập tên kích cỡ")
    // Dùng fully-qualified name thay vì import jakarta.validation.constraints.Size,
    // vì import đó sẽ đụng tên với chính class Size (entity) này -> lỗi biên dịch.
    @jakarta.validation.constraints.Size(max = 20, message = "Tên kích cỡ tối đa 20 ký tự")
    private String name;

    private Integer status;
    private Boolean deleteFlag;
}