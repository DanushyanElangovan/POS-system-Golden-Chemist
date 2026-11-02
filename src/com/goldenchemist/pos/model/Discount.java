package com.goldenchemist.pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A discount entered at the till, either a fixed amount or a percentage of the subtotal. */
public class Discount {

    public enum Type { AMOUNT, PERCENT }

    public static final Discount NONE = new Discount(Type.AMOUNT, BigDecimal.ZERO);

    private final Type type;
    private final BigDecimal value;

    public Discount(Type type, BigDecimal value) {
        this.type = type;
        this.value = value == null ? BigDecimal.ZERO : value;
    }

    public Type getType() { return type; }
    public BigDecimal getValue() { return value; }

    /** Discount in currency for the given subtotal (never more than the subtotal). */
    public BigDecimal amountFor(BigDecimal subtotal) {
        BigDecimal amt = type == Type.PERCENT
                ? subtotal.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
        return amt.min(subtotal).max(BigDecimal.ZERO);
    }

    /** The discount expressed as a percentage of the subtotal. */
    public BigDecimal percentOf(BigDecimal subtotal) {
        if (subtotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amountFor(subtotal).multiply(new BigDecimal("100")).divide(subtotal, 2, RoundingMode.HALF_UP);
    }
}
