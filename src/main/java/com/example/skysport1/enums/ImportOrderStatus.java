package com.example.skysport1.enums;

public enum ImportOrderStatus {

    PENDING(1,  "Chờ duyệt"),
    APPROVED(2, "Đã duyệt"),
    REJECTED(3, "Từ chối");

    private final int value;
    private final String label;

    ImportOrderStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue()    { return value; }
    public String getLabel() { return label; }

    public static ImportOrderStatus of(Integer value) {
        if (value == null) return null;
        for (ImportOrderStatus s : values())
            if (s.value == value) return s;
        return null;
    }

    public boolean matches(Integer v) {
        return this.value == (v == null ? -1 : v);
    }
}
