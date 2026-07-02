package com.example.skysport1.enums;

public enum RoleName {

    ADMIN("ROLE001",   "ADMIN"),
    STAFF("ROLE002",   "STAFF"),
    CUSTOMER("ROLE003","CUSTOMER");

    private final String id;     // PK trong DB
    private final String dbName; // Giá trị cột name

    RoleName(String id, String dbName) {
        this.id = id;
        this.dbName = dbName;
    }

    public String getId()      { return id; }
    public String getDbName()  { return dbName; }

    public boolean matchesName(String name) {
        return this.dbName.equalsIgnoreCase(name);
    }

    public boolean matchesId(String id) {
        return this.id.equals(id);
    }

    public static RoleName ofId(String id) {
        if (id == null) return CUSTOMER;
        for (RoleName r : values())
            if (r.id.equals(id)) return r;
        return CUSTOMER;
    }

    public static RoleName ofName(String name) {
        if (name == null) return CUSTOMER;
        for (RoleName r : values())
            if (r.dbName.equalsIgnoreCase(name)) return r;
        return CUSTOMER;
    }
}
