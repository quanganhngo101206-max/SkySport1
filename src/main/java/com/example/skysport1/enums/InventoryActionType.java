package com.example.skysport1.enums;

public enum InventoryActionType {

    IMPORT("IMPORT",     "Nhập kho"),
    SALE("SALE",         "Bán hàng"),
    RETURN("RETURN",     "Hoàn trả"),
    ADJUSTMENT("ADJUSTMENT", "Điều chỉnh thủ công");

    private final String value;
    private final String label;

    InventoryActionType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue()   { return value; }
    public String getLabel()   { return label; }

    public static InventoryActionType of(String value) {
        if (value == null) return null;
        for (InventoryActionType t : values())
            if (t.value.equalsIgnoreCase(value)) return t;
        return null;
    }
}
