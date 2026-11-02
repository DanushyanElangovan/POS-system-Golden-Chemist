package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.db.Sql;
import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EmployeeDao {

    private static final String COLS = "employee_id, full_name, username, password_hash, role, phone, email, nic, hired_date, active";

    static Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("employee_id"));
        e.setFullName(rs.getString("full_name"));
        e.setUsername(rs.getString("username"));
        e.setPasswordHash(rs.getString("password_hash"));
        e.setRole(Role.valueOf(rs.getString("role")));
        e.setPhone(rs.getString("phone"));
        e.setEmail(rs.getString("email"));
        e.setNic(rs.getString("nic"));
        e.setHiredDate(Sql.getDate(rs, "hired_date"));
        e.setActive(rs.getBoolean("active"));
        return e;
    }

    public Employee findByUsername(String username) {
        return Database.one("look up the user",
                "SELECT " + COLS + " FROM employee WHERE username = ?", EmployeeDao::map, username);
    }

    public Employee findById(int id) {
        return Database.one("load the employee",
                "SELECT " + COLS + " FROM employee WHERE employee_id = ?", EmployeeDao::map, id);
    }

    public List<Employee> search(String q) {
        String like = "%" + q.trim() + "%";
        return Database.list("load employees",
                "SELECT " + COLS + " FROM employee WHERE full_name LIKE ? OR username LIKE ? OR phone LIKE ? "
                        + "ORDER BY active DESC, full_name",
                EmployeeDao::map, like, like, like);
    }

    public long count() {
        Long n = Database.one("count employees", "SELECT COUNT(*) AS n FROM employee", rs -> rs.getLong("n"));
        return n == null ? 0 : n;
    }

    public long countActiveAdmins() {
        Long n = Database.one("count administrators",
                "SELECT COUNT(*) AS n FROM employee WHERE role = 'ADMIN' AND active = 1", rs -> rs.getLong("n"));
        return n == null ? 0 : n;
    }

    public int insert(Employee e) {
        return (int) Database.insert("add the employee",
                "INSERT INTO employee (full_name, username, password_hash, role, phone, email, nic, hired_date, active) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                e.getFullName(), e.getUsername(), e.getPasswordHash(), e.getRole(), e.getPhone(), e.getEmail(),
                e.getNic(), e.getHiredDate(), e.isActive());
    }

    public void update(Employee e) {
        Database.update("update the employee",
                "UPDATE employee SET full_name=?, username=?, role=?, phone=?, email=?, nic=?, hired_date=?, active=? "
                        + "WHERE employee_id=?",
                e.getFullName(), e.getUsername(), e.getRole(), e.getPhone(), e.getEmail(), e.getNic(),
                e.getHiredDate(), e.isActive(), e.getId());
    }

    public void updatePassword(int employeeId, String passwordHash) {
        Database.update("change the password",
                "UPDATE employee SET password_hash=? WHERE employee_id=?", passwordHash, employeeId);
    }

    public void setActive(int employeeId, boolean active) {
        Database.update("change the employee status",
                "UPDATE employee SET active=? WHERE employee_id=?", active, employeeId);
    }
}
