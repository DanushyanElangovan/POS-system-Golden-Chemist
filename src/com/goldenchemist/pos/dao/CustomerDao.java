package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.model.Customer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomerDao {

    private static final String COLS = "customer_id, full_name, phone, email, address";

    static Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("customer_id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        return c;
    }

    public List<Customer> search(String q) {
        String like = "%" + q.trim() + "%";
        return Database.list("load customers",
                "SELECT " + COLS + " FROM customer WHERE full_name LIKE ? OR phone LIKE ? ORDER BY full_name",
                CustomerDao::map, like, like);
    }

    public int insert(Customer c) {
        return (int) Database.insert("add the customer",
                "INSERT INTO customer (full_name, phone, email, address) VALUES (?,?,?,?)",
                c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress());
    }

    public void update(Customer c) {
        Database.update("update the customer",
                "UPDATE customer SET full_name=?, phone=?, email=?, address=? WHERE customer_id=?",
                c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getId());
    }

    /** Past invoices keep their data; the link to the customer is cleared by the foreign key. */
    public void delete(int id) {
        Database.update("delete the customer", "DELETE FROM customer WHERE customer_id=?", id);
    }
}
