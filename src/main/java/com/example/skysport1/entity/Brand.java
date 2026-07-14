package com.example.skysport1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Brand")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Brand {

    @Id
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Vui lòng nhập tên thương hiệu")
    @Size(max = 100, message = "Tên thương hiệu tối đa 100 ký tự")
    private String name;

    private String description;
    private Integer status;
    private Boolean deleteFlag;

    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;
}