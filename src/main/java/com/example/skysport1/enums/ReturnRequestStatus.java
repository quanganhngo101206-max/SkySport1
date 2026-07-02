package com.example.skysport1.enums;

public enum ReturnRequestStatus {

    PENDING(1,  "Chờ duyệt"),
    APPROVED(2, "Đã duyệt"),
    REJECTED(3, "Từ chối"),
    REFUNDED(4, "Đã hoàn tiền");   // Mới hơn DATN

    private final int value;
    private final String label;

    ReturnRequestStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue()    { return value; }
    public String getLabel() { return label; }

    public static ReturnRequestStatus of(Integer value) {
        if (value == null) return null;
        for (ReturnRequestStatus s : values())
            if (s.value == value) return s;
        return null;
    }

    public boolean matches(Integer v) {
        return this.value == (v == null ? -1 : v);
    }
}
