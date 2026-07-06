package com.example.skysport1.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CounterSaleCheckoutRequest {

    // null = khách vãng lai
    private String customerId;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentId;

    @NotEmpty(message = "Giỏ hàng đang trống")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        private Integer productDetailId;

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private int quantity;
    }
}