package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.model.Supplier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SupplierDao {

    private static final String COLS = "supplier_id, name, contact_person, phone, email, address, active";

    static Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("supplier_id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setActive(rs.getBoolean("active"));
        return s;
    }

    public List<Supplier> search(String q, boolean activeOnly) {
        String like = "%" + q.trim() + "%";
        return Database.list("load suppliers",
                "SELECT " + COLS + " FROM supplier WHERE (name LIKE ? OR contact_person LIKE ? OR phone LIKE ?)"
                        + (activeOnly ? " AND active = 1" : "") + " ORDER BY active DESC, name",
                SupplierDao::map, like, like, like);
    }

    public int insert(Supplier s) {
        return (int) Database.insert("add the supplier",
                "INSERT INTO supplier (name, contact_person, phone, email, address, active) VALUES (?,?,?,?,?,?)",
                s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress(), s.isActive());
    }

    public void update(Supplier s) {
        Database.update("update the supplier",
                "UPDATE supplier SET name=?, contact_person=?, phone=?, email=?, address=?, active=? WHERE supplier_id=?",
                s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress(), s.isActive(), s.getId());
    }

    public void setActive(int id, boolean active) {
        Database.update("change the supplier status", "UPDATE supplier SET active=? WHERE supplier_id=?", active, id);
    }
}
