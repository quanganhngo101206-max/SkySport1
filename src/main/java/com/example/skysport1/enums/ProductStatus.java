package com.example.skysport1.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    ACTIVE(1, "Đang bán"),
    OUT_OF_STOCK(2, "Hết hàng"),
    INACTIVE(0, "Ngừng bán");

    private final Integer value;
    private final String label;
}
