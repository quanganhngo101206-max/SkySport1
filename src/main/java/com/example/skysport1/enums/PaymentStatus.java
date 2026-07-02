package com.example.skysport1.enums;

public enum PaymentStatus {

    UNPAID(0,   "Chưa thanh toán"),
    PAID(1,     "Đã thanh toán"),
    REFUNDED(2, "Hoàn tiền");

    private final int value;
    private final String label;

    PaymentStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue()    { return value; }
    public String getLabel() { return label; }

    public static PaymentStatus of(Integer value) {
        if (value == null) return UNPAID;
        for (PaymentStatus s : values())
            if (s.value == value) return s;
        return UNPAID;
    }

    public boolean matches(Integer v) {
        return this.value == (v == null ? -1 : v);
    }
}
