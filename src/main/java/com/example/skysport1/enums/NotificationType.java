package com.example.skysport1.enums;

public enum NotificationType {

    ORDER_CONFIRMED("ORDER_CONFIRMED",   "Đơn hàng đã xác nhận"),
    ORDER_SHIPPING("ORDER_SHIPPING",     "Đơn hàng đang giao"),
    ORDER_DELIVERED("ORDER_DELIVERED",   "Đơn hàng đã giao"),
    ORDER_CANCELLED("ORDER_CANCELLED",   "Đơn hàng đã hủy"),
    RETURN_APPROVED("RETURN_APPROVED",   "Hoàn trả được duyệt"),
    RETURN_REJECTED("RETURN_REJECTED",   "Hoàn trả bị từ chối"),
    VOUCHER_NEW("VOUCHER_NEW",           "Voucher mới"),
    LOW_STOCK("LOW_STOCK",               "Tồn kho thấp"),
    NEW_ORDER("NEW_ORDER",               "Đơn hàng mới");

    private final String value;
    private final String label;

    NotificationType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue()   { return value; }
    public String getLabel()   { return label; }
}
