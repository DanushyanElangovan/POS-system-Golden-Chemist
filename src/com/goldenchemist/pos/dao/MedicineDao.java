package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.db.Sql;
import com.goldenchemist.pos.model.Medicine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MedicineDao {

    /** Medicine columns plus the sellable (non-expired) stock total. */
    private static final String SELECT =
            "SELECT m.medicine_id, m.barcode, m.brand_name, m.generic_name, m.category_id, c.name AS category_name, "
                    + "m.dosage_form, m.strength, m.manufacturer, m.selling_price, m.reorder_level, "
                    + "m.requires_prescription, m.active, COALESCE(st.qty, 0) AS stock_qty "
                    + "FROM medicine m "
                    + "LEFT JOIN category c ON c.category_id = m.category_id "
                    + "LEFT JOIN (SELECT medicine_id, SUM(quantity) AS qty FROM batch "
                    + "            WHERE expiry_date >= CURDATE() GROUP BY medicine_id) st "
                    + "       ON st.medicine_id = m.medicine_id ";

    static Medicine map(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getInt("medicine_id"));
        m.setBarcode(rs.getString("barcode"));
        m.setBrandName(rs.getString("brand_name"));
        m.setGenericName(rs.getString("generic_name"));
        m.setCategoryId(Sql.getInteger(rs, "category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setDosageForm(rs.getString("dosage_form"));
        m.setStrength(rs.getString("strength"));
        m.setManufacturer(rs.getString("manufacturer"));
        m.setSellingPrice(rs.getBigDecimal("selling_price"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setRequiresPrescription(rs.getBoolean("requires_prescription"));
        m.setActive(rs.getBoolean("active"));
        m.setStockQty(rs.getInt("stock_qty"));
        return m;
    }

    public List<Medicine> search(String q, boolean activeOnly) {
        String t = q.trim();
        String like = "%" + t + "%";
        return Database.list("load medicines",
                SELECT + "WHERE (m.barcode = ? OR m.brand_name LIKE ? OR m.generic_name LIKE ?)"
                        + (activeOnly ? " AND m.active = 1" : "")
                        + " ORDER BY m.active DESC, m.brand_name LIMIT 500",
                MedicineDao::map, t, like, like);
    }

    public Medicine findByBarcode(String barcode) {
        return Database.one("look up the barcode",
                SELECT + "WHERE m.barcode = ? AND m.active = 1", MedicineDao::map, barcode.trim());
    }

    public Medicine findById(int id) {
        return Database.one("load the medicine", SELECT + "WHERE m.medicine_id = ?", MedicineDao::map, id);
    }

    public List<Medicine> lowStock(int limit) {
        return Database.list("load low stock items",
                SELECT + "WHERE m.active = 1 AND COALESCE(st.qty, 0) <= m.reorder_level "
                        + "ORDER BY COALESCE(st.qty, 0), m.brand_name LIMIT " + limit,
                MedicineDao::map);
    }

    public int insert(Medicine m) {
        return (int) Database.insert("add the medicine",
                "INSERT INTO medicine (barcode, brand_name, generic_name, category_id, dosage_form, strength, "
                        + "manufacturer, selling_price, reorder_level, requires_prescription, active) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                m.getBarcode(), m.getBrandName(), m.getGenericName(), m.getCategoryId(), m.getDosageForm(),
                m.getStrength(), m.getManufacturer(), m.getSellingPrice(), m.getReorderLevel(),
                m.isRequiresPrescription(), m.isActive());
    }

    public void update(Medicine m) {
        Database.update("update the medicine",
                "UPDATE medicine SET barcode=?, brand_name=?, generic_name=?, category_id=?, dosage_form=?, strength=?, "
                        + "manufacturer=?, selling_price=?, reorder_level=?, requires_prescription=?, active=? "
                        + "WHERE medicine_id=?",
                m.getBarcode(), m.getBrandName(), m.getGenericName(), m.getCategoryId(), m.getDosageForm(),
                m.getStrength(), m.getManufacturer(), m.getSellingPrice(), m.getReorderLevel(),
                m.isRequiresPrescription(), m.isActive(), m.getId());
    }

    public void setActive(int id, boolean active) {
        Database.update("change the medicine status", "UPDATE medicine SET active=? WHERE medicine_id=?", active, id);
    }
}
