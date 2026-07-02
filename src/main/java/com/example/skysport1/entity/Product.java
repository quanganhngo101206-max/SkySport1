package com.example.skysport1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Product")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 255, message = "Tên sản phẩm phải từ 2 đến 255 ký tự")
    private String name;

    @Column(unique = true)
    @NotBlank(message = "Slug không được để trống")
    @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", 
             message = "Slug chỉ chứa chữ thường, số, và dấu gạch ngang")
    private String slug;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    // 1: Nam, 2: Nữ, 3: Unisex
    @Min(value = 1, message = "Giới tính phải là 1 (Nam), 2 (Nữ), hoặc 3 (Unisex)")
    @Max(value = 3, message = "Giới tính phải là 1 (Nam), 2 (Nữ), hoặc 3 (Unisex)")
    @Column(name = "gender")
    private Integer gender;

    //1: còn, 2: hết, 0: dừng
    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái không hợp lệ")
    @Max(value = 2, message = "Trạng thái không hợp lệ")
    @Column(name = "status")
    private Integer status;

    @NotNull(message = "Cờ xóa không được để trống")
    private Boolean deleteFlag;

    @NotNull(message = "Thương hiệu không được để trống")
    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @NotNull(message = "Danh mục không được để trống")
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "Chất liệu không được để trống")
    @ManyToOne
    @JoinColumn(name = "material_id")
    private Material material;

    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductDetail> productDetails;
}
