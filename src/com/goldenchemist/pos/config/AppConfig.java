package com.goldenchemist.pos.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

/** Reads config/app.properties (database, shop details and business rules). */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        File f = new File(System.getProperty("pos.config", "config/app.properties"));
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                PROPS.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + f.getAbsolutePath() + ": " + e.getMessage());
            }
        } else {
            System.err.println("Config file not found: " + f.getAbsolutePath() + " (using defaults)");
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String v = PROPS.getProperty(key);
        return v == null ? defaultValue : v.trim();
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static BigDecimal getDecimal(String key, String defaultValue) {
        try {
            return new BigDecimal(get(key, defaultValue));
        } catch (NumberFormatException e) {
            return new BigDecimal(defaultValue);
        }
    }

    public static String shopName() {
        return get("shop.name", "Golden Chemist Pharmacy");
    }

    public static String currency() {
        return get("currency.symbol", "Rs.");
    }

    public static int expiryWarningDays() {
        return getInt("stock.expiry.warning.days", 60);
    }
}
