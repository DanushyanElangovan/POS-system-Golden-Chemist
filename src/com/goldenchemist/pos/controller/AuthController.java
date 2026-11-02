package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.EmployeeDao;
import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Role;
import com.goldenchemist.pos.util.PasswordUtil;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class AuthController {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 60_000L;

    private static final Map<String, int[]> FAILS = new HashMap<>();
    private static final Map<String, Long> LOCKED_UNTIL = new HashMap<>();

    private final EmployeeDao dao = new EmployeeDao();

    /** Creates admin / admin123 on the very first start so the shop can sign in. Returns true if created. */
    public boolean ensureDefaultAdmin() {
        if (dao.count() > 0) {
            return false;
        }
        Employee e = new Employee();
        e.setFullName("System Administrator");
        e.setUsername("admin");
        e.setPasswordHash(PasswordUtil.hash("admin123"));
        e.setRole(Role.ADMIN);
        e.setHiredDate(LocalDate.now());
        dao.insert(e);
        return true;
    }

    public Employee login(String username, String password) {
        String user = Strings.isBlank(username) ? "" : username.trim().toLowerCase();
        if (user.isEmpty() || password == null || password.isEmpty()) {
            throw new PosException("Enter your username and password.");
        }
        Long until = LOCKED_UNTIL.get(user);
        if (until != null && until > System.currentTimeMillis()) {
            long secs = (until - System.currentTimeMillis()) / 1000 + 1;
            throw new PosException("Too many failed attempts. Try again in " + secs + " seconds.");
        }
        Employee e = dao.findByUsername(user);
        boolean ok = e != null && e.isActive() && PasswordUtil.verify(password, e.getPasswordHash());
        if (!ok) {
            int[] n = FAILS.computeIfAbsent(user, k -> new int[1]);
            n[0]++;
            if (n[0] >= MAX_ATTEMPTS) {
                LOCKED_UNTIL.put(user, System.currentTimeMillis() + LOCK_MILLIS);
                n[0] = 0;
            }
            throw new PosException(e != null && !e.isActive()
                    ? "This account is disabled. Contact the administrator."
                    : "Incorrect username or password.");
        }
        FAILS.remove(user);
        LOCKED_UNTIL.remove(user);
        Session.start(e);
        return e;
    }

    public void logout() {
        Session.end();
    }

    public void changeOwnPassword(String oldPassword, String newPassword, String confirm) {
        Employee me = Session.user();
        Employee fresh = dao.findById(me.getId());
        if (fresh == null || !PasswordUtil.verify(oldPassword, fresh.getPasswordHash())) {
            throw new PosException("The current password is incorrect.");
        }
        if (!newPassword.equals(confirm)) {
            throw new PosException("The new passwords do not match.");
        }
        PasswordUtil.validateStrength(newPassword);
        dao.updatePassword(me.getId(), PasswordUtil.hash(newPassword));
    }
}
