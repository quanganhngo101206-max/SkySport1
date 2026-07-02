package com.example.skysport1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillDetailResponse {
    private Integer id;
    private String productName;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
}
