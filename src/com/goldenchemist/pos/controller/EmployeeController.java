package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.dao.EmployeeDao;
import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.model.Role;
import com.goldenchemist.pos.util.PasswordUtil;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import java.util.List;

public class EmployeeController {

    private final EmployeeDao dao = new EmployeeDao();

    public List<Employee> search(String q) {
        Session.require(Module.EMPLOYEES);
        return dao.search(q);
    }

    /**
     * Creates (id == 0, password required) or updates an employee.
     * On update a non-blank newPassword resets the password.
     */
    public void save(Employee e, String newPassword) {
        Session.require(Module.EMPLOYEES);
        e.setFullName(Strings.required("Full name", e.getFullName(), 100));
        String username = Strings.required("Username", e.getUsername(), 40).toLowerCase();
        if (!username.matches("^[a-z0-9._-]{3,40}$")) {
            throw new PosException("Username may use letters, digits, dot, dash and underscore (3-40 characters).");
        }
        e.setUsername(username);
        e.setPhone(Strings.phone("Phone", e.getPhone()));
        e.setEmail(Strings.email("E-mail", e.getEmail()));
        e.setNic(Strings.optional("NIC", e.getNic(), 20));
        if (e.getRole() == null) {
            throw new PosException("Choose a role.");
        }
        if (e.getId() == 0) {
            PasswordUtil.validateStrength(newPassword);
            e.setPasswordHash(PasswordUtil.hash(newPassword));
            dao.insert(e);
            return;
        }
        guardLastAdmin(e.getId(), e.getRole(), e.isActive());
        dao.update(e);
        if (!Strings.isBlank(newPassword)) {
            PasswordUtil.validateStrength(newPassword);
            dao.updatePassword(e.getId(), PasswordUtil.hash(newPassword));
        }
    }

    public void setActive(Employee e, boolean active) {
        Session.require(Module.EMPLOYEES);
        guardLastAdmin(e.getId(), e.getRole(), active);
        dao.setActive(e.getId(), active);
    }

    /** You cannot lock yourself out or remove the last working administrator. */
    private void guardLastAdmin(int employeeId, Role newRole, boolean newActive) {
        Employee me = Session.user();
        Employee stored = dao.findById(employeeId);
        if (stored == null) {
            throw new PosException("The employee no longer exists.");
        }
        if (employeeId == me.getId() && (!newActive || newRole != Role.ADMIN)) {
            throw new PosException("You cannot disable your own account or remove your own administrator role.");
        }
        boolean wasActiveAdmin = stored.getRole() == Role.ADMIN && stored.isActive();
        boolean stillActiveAdmin = newRole == Role.ADMIN && newActive;
        if (wasActiveAdmin && !stillActiveAdmin && dao.countActiveAdmins() <= 1) {
            throw new PosException("At least one active administrator is required.");
        }
    }
}
