package com.goldenchemist.pos.util;

/** String and validation helpers. */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Trimmed text, or null when blank (so optional columns store NULL, not ''). */
    public static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public static String nz(String s) {
        return s == null ? "" : s;
    }

    public static String required(String label, String value, int maxLen) {
        if (isBlank(value)) {
            throw new PosException(label + " is required.");
        }
        String v = value.trim();
        if (v.length() > maxLen) {
            throw new PosException(label + " must be at most " + maxLen + " characters.");
        }
        return v;
    }

    public static String optional(String label, String value, int maxLen) {
        if (isBlank(value)) {
            return null;
        }
        String v = value.trim();
        if (v.length() > maxLen) {
            throw new PosException(label + " must be at most " + maxLen + " characters.");
        }
        return v;
    }

    public static String phone(String label, String value) {
        String v = optional(label, value, 20);
        if (v != null && !v.matches("^[+0-9][0-9 \\-]{5,18}$")) {
            throw new PosException(label + " must contain only digits, spaces or dashes (optionally starting with +).");
        }
        return v;
    }

    public static String email(String label, String value) {
        String v = optional(label, value, 100);
        if (v != null && !v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new PosException(label + " is not a valid e-mail address.");
        }
        return v;
    }
}
