package com.goldenchemist.pos.model;

import java.util.EnumSet;
import java.util.Set;

/** Staff roles and what each may open. */
public enum Role {
    ADMIN("Administrator", EnumSet.allOf(Module.class)),
    PHARMACIST("Pharmacist", EnumSet.of(Module.DASHBOARD, Module.BILLING, Module.INVOICES, Module.MEDICINES,
            Module.INVENTORY, Module.SUPPLIERS, Module.CUSTOMERS, Module.REPORTS)),
    CASHIER("Cashier", EnumSet.of(Module.DASHBOARD, Module.BILLING, Module.INVOICES, Module.CUSTOMERS));

    private final String label;
    private final Set<Module> modules;

    Role(String label, Set<Module> modules) {
        this.label = label;
        this.modules = modules;
    }

    public String getLabel() {
        return label;
    }

    public boolean can(Module m) {
        return modules.contains(m);
    }

    /** Voiding an invoice returns stock, so it is limited to senior staff. */
    public boolean canVoidSales() {
        return this != CASHIER;
    }

    @Override
    public String toString() {
        return label;
    }
}
