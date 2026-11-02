package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.db.Sql;
import com.goldenchemist.pos.model.Batch;
import com.goldenchemist.pos.model.StockMovement;
import com.goldenchemist.pos.util.PosException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BatchDao {

    public static final String F_IN_STOCK = "IN_STOCK";
    public static final String F_EXPIRING = "EXPIRING";
    public static final String F_EXPIRED = "EXPIRED";
    public static final String F_ALL = "ALL";

    private static final String SELECT =
            "SELECT b.batch_id, b.medicine_id, CONCAT(m.brand_name, IFNULL(CONCAT(' ', m.strength), '')) AS medicine_name, "
                    + "b.supplier_id, s.name AS supplier_name, b.batch_no, b.expiry_date, b.quantity, b.cost_price, "
                    + "b.received_date "
                    + "FROM batch b JOIN medicine m ON m.medicine_id = b.medicine_id "
                    + "LEFT JOIN supplier s ON s.supplier_id = b.supplier_id ";

    static Batch map(ResultSet rs) throws SQLException {
        Batch b = new Batch();
        b.setId(rs.getInt("batch_id"));
        b.setMedicineId(rs.getInt("medicine_id"));
        b.setMedicineName(rs.getString("medicine_name"));
        b.setSupplierId(Sql.getInteger(rs, "supplier_id"));
        b.setSupplierName(rs.getString("supplier_name"));
        b.setBatchNo(rs.getString("batch_no"));
        b.setExpiryDate(Sql.getDate(rs, "expiry_date"));
        b.setQuantity(rs.getInt("quantity"));
        b.setCostPrice(rs.getBigDecimal("cost_price"));
        b.setReceivedDate(Sql.getDate(rs, "received_date"));
        return b;
    }

    public List<Batch> search(String q, String filter, int expiryDays) {
        String like = "%" + q.trim() + "%";
        String where = "WHERE (m.brand_name LIKE ? OR m.generic_name LIKE ? OR b.batch_no LIKE ? OR m.barcode = ?) ";
        if (F_IN_STOCK.equals(filter)) {
            where += "AND b.quantity > 0 AND b.expiry_date >= CURDATE() ";
        } else if (F_EXPIRING.equals(filter)) {
            where += "AND b.quantity > 0 AND b.expiry_date >= CURDATE() "
                    + "AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL " + Math.max(1, expiryDays) + " DAY) ";
        } else if (F_EXPIRED.equals(filter)) {
            where += "AND b.quantity > 0 AND b.expiry_date < CURDATE() ";
        }
        return Database.list("load stock",
                SELECT + where + "ORDER BY b.expiry_date, m.brand_name LIMIT 1000",
                BatchDao::map, like, like, like, q.trim());
    }

    public List<Batch> expiringSoon(int days, int limit) {
        return Database.list("load expiring stock",
                SELECT + "WHERE b.quantity > 0 AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL " + days + " DAY) "
                        + "ORDER BY b.expiry_date LIMIT " + limit,
                BatchDao::map);
    }

    /** Adds a new batch and records the RECEIVE movement. */
    public int receive(Batch b, int employeeId) {
        return Database.inTransaction("receive the stock", c -> {
            long id = Sql.insert(c,
                    "INSERT INTO batch (medicine_id, supplier_id, batch_no, expiry_date, quantity, cost_price, received_date) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    b.getMedicineId(), b.getSupplierId(), b.getBatchNo(), b.getExpiryDate(), b.getQuantity(),
                    b.getCostPrice(), b.getReceivedDate());
            Sql.exec(c, "INSERT INTO stock_movement (batch_id, change_qty, reason, reference, employee_id) "
                    + "VALUES (?,?,?,?,?)", id, b.getQuantity(), "RECEIVE", "Batch " + b.getBatchNo(), employeeId);
            return (int) id;
        });
    }

    /** Applies a manual correction (positive or negative) to one batch. */
    public void adjust(int batchId, int delta, String reason, String note, int employeeId) {
        Database.inTransaction("adjust the stock", c -> {
            Integer qty = Sql.queryOne(c, "SELECT quantity FROM batch WHERE batch_id = ? FOR UPDATE",
                    rs -> rs.getInt("quantity"), batchId);
            if (qty == null) {
                throw new PosException("The batch no longer exists.");
            }
            if (qty + delta < 0) {
                throw new PosException("The batch only has " + qty + " units; it cannot go below zero.");
            }
            Sql.exec(c, "UPDATE batch SET quantity = quantity + ? WHERE batch_id = ?", delta, batchId);
            Sql.exec(c, "INSERT INTO stock_movement (batch_id, change_qty, reason, reference, employee_id) "
                    + "VALUES (?,?,?,?,?)", batchId, delta, reason, note, employeeId);
            return null;
        });
    }

    public List<StockMovement> recentMovements(int limit) {
        return Database.list("load stock history",
                "SELECT sm.movement_id, CONCAT(m.brand_name, IFNULL(CONCAT(' ', m.strength), '')) AS medicine_name, "
                        + "b.batch_no, sm.change_qty, sm.reason, sm.reference, e.full_name, sm.created_at "
                        + "FROM stock_movement sm JOIN batch b ON b.batch_id = sm.batch_id "
                        + "JOIN medicine m ON m.medicine_id = b.medicine_id "
                        + "LEFT JOIN employee e ON e.employee_id = sm.employee_id "
                        + "ORDER BY sm.movement_id DESC LIMIT " + limit,
                rs -> {
                    StockMovement s = new StockMovement();
                    s.setId(rs.getInt("movement_id"));
                    s.setMedicineName(rs.getString("medicine_name"));
                    s.setBatchNo(rs.getString("batch_no"));
                    s.setChangeQty(rs.getInt("change_qty"));
                    s.setReason(rs.getString("reason"));
                    s.setReference(rs.getString("reference"));
                    s.setEmployeeName(rs.getString("full_name"));
                    s.setCreatedAt(Sql.getDateTime(rs, "created_at"));
                    return s;
                });
    }

    /** Units that can be sold today (non-expired batches only). */
    public int availableQty(int medicineId) {
        Integer n = Database.one("check stock",
                "SELECT COALESCE(SUM(quantity), 0) AS n FROM batch WHERE medicine_id = ? AND expiry_date >= CURDATE()",
                rs -> rs.getInt("n"), medicineId);
        return n == null ? 0 : n;
    }
}
