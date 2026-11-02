package com.goldenchemist.pos.model;

/** Screens of the application; used for role based access control. */
public enum Module {
    DASHBOARD("Dashboard"),
    BILLING("Billing"),
    INVOICES("Invoices"),
    MEDICINES("Medicines"),
    INVENTORY("Inventory"),
    SUPPLIERS("Suppliers"),
    CUSTOMERS("Customers"),
    EMPLOYEES("Employees"),
    REPORTS("Reports");

    private final String label;

    Module(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
