package com.example.skysport1.enums;

public enum DiscountType {

    FIXED(1,      "Giảm cố định"),
    PERCENTAGE(2, "Giảm phần trăm");

    private final int value;
    private final String label;

    DiscountType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue()    { return value; }
    public String getLabel() { return label; }

    public static DiscountType of(Integer value) {
        if (value == null) return null;
        for (DiscountType t : values())
            if (t.value == value) return t;
        return null;
    }

    public boolean matches(Integer v) {
        return this.value == (v == null ? -1 : v);
    }
}