package com.example.skysport1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String slug;
    private String description;
    private String genderLabel;      // "Nam", "Nữ", "Unisex"
    private String statusLabel;      // "Đang bán", "Hết hàng", "Ngừng bán"
    private BrandResponse brand;
    private CategoryResponse category;
    private String materialName;
    private List<ProductDetailResponse> productDetails;
    private LocalDateTime createDate;
}