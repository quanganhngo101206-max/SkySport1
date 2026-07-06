package com.example.skysport1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterSaleProductResponse {
    private Integer productDetailId;
    private String sku;
    private String productName;
    private String sizeName;
    private String colorName;
    private BigDecimal price;
    private Integer quantityAvailable;
}