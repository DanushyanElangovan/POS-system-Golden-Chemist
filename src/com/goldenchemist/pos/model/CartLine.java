package com.goldenchemist.pos.model;

import java.math.BigDecimal;

/** One medicine and quantity in the billing cart. */
public class CartLine {
    private final Medicine medicine;
    private int quantity;
    private int available;

    public CartLine(Medicine medicine, int quantity, int available) {
        this.medicine = medicine;
        this.quantity = quantity;
        this.available = available;
    }

    public Medicine getMedicine() { return medicine; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }

    public BigDecimal getUnitPrice() { return medicine.getSellingPrice(); }

    public BigDecimal getLineTotal() {
        return medicine.getSellingPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
