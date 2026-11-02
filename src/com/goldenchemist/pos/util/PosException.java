package com.goldenchemist.pos.util;

/** Unchecked exception whose message is safe to show directly to the user. */
public class PosException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PosException(String message) {
        super(message);
    }

    public PosException(String message, Throwable cause) {
        super(message, cause);
    }
}
