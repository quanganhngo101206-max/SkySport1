package com.example.skysport1.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductGender {
    MALE(1, "Nam"),
    FEMALE(2, "Nữ"),
    UNISEX(3, "Unisex");

    private final Integer value;
    private final String label;
}
