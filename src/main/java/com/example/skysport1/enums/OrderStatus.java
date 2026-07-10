package com.example.skysport1.enums;

public enum OrderStatus {

    PENDING(1,   "Chờ xác nhận"),
    CONFIRMED(2, "Đã xác nhận"),
    SHIPPING(3,  "Đang giao"),
    DELIVERED(4, "Đã giao"),
    CANCELLED(5, "Đã hủy"),
    RETURNING(6, "Hoàn trả"),
    COMPLETED(7, "Hoàn thành"),
    /**
     * Customer yêu cầu hủy (đơn đang chờ shop duyệt)
     */
    CANCEL_REQUESTED(8, "Yêu cầu hủy");

    private final int value;
    private final String label;

    OrderStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue()   { return value; }
    public String getLabel(){ return label; }

    public static OrderStatus of(Integer value) {
        if (value == null) return null;
        for (OrderStatus s : values())
            if (s.value == value) return s;
        return null;
    }

    public boolean matches(Integer value) {
        return this.value == (value == null ? -1 : value);
    }
}
