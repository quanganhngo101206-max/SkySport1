package com.example.skysport1.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    private Integer id;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String receiverPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    private String address;

    private Boolean isDefault = false;
}