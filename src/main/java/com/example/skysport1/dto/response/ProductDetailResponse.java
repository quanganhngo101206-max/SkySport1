package com.example.skysport1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductDetailResponse {
    private Integer id;
    private String sizeName;
    private String colorName;
    private String colorHexCode;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
    private Integer status;
}
