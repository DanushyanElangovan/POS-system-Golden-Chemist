package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.util.PosException;

/** Holds the signed-in employee and enforces role permissions. */
public final class Session {

    private static Employee current;

    private Session() {
    }

    public static void start(Employee e) {
        current = e;
    }

    public static void end() {
        current = null;
    }

    public static Employee user() {
        if (current == null) {
            throw new PosException("Please sign in first.");
        }
        return current;
    }

    public static boolean isSignedIn() {
        return current != null;
    }

    public static boolean can(Module m) {
        return current != null && current.getRole().can(m);
    }

    /** Called by every controller: the UI hides screens, but the rule is enforced here too. */
    public static void require(Module m) {
        if (!can(m)) {
            throw new PosException("Your role does not allow access to " + m.getLabel() + ".");
        }
    }
}
