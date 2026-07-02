package com.example.skysport1.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrackOrderRequest {
    @NotBlank(message = "Vui lòng nhập mã đơn hàng")
    private String orderId;

    @NotBlank(message = "Vui lòng nhập email")
    private String email;
}