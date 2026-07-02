package com.example.skysport1.dto.response;

import java.math.BigDecimal;

public interface DashboardTopProductResponse {
    String getProductName();
    Long getSoldQuantity();
    BigDecimal getRevenue();
}
