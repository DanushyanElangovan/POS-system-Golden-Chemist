package com.goldenchemist.pos.util;

import com.goldenchemist.pos.config.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Currency helpers - all money is BigDecimal with 2 decimal places. */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal v) {
        return AppConfig.currency() + " " + new DecimalFormat("#,##0.00").format(scale(v));
    }

    public static String plain(BigDecimal v) {
        return new DecimalFormat("#,##0.00").format(scale(v));
    }

    /** Parses user input such as "1,250.50" or "Rs. 300". */
    public static BigDecimal parse(String label, String text) {
        if (Strings.isBlank(text)) {
            throw new PosException(label + " is required.");
        }
        String cleaned = text.replace(AppConfig.currency(), "").replace(",", "").trim();
        try {
            return scale(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            throw new PosException(label + " must be a number (for example 250.00).");
        }
    }
}
