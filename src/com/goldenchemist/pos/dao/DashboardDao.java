package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;

import java.math.BigDecimal;

/** Headline numbers for the dashboard. */
public class DashboardDao {

    public BigDecimal todaySalesTotal() {
        BigDecimal v = Database.one("load today's sales",
                "SELECT COALESCE(SUM(total), 0) AS v FROM sale WHERE status = 'COMPLETED' AND DATE(sale_date) = CURDATE()",
                rs -> rs.getBigDecimal("v"));
        return v == null ? BigDecimal.ZERO : v;
    }

    public long todayInvoiceCount() {
        return count("SELECT COUNT(*) AS n FROM sale WHERE status = 'COMPLETED' AND DATE(sale_date) = CURDATE()");
    }

    public long activeMedicineCount() {
        return count("SELECT COUNT(*) AS n FROM medicine WHERE active = 1");
    }

    public long lowStockCount() {
        return count("SELECT COUNT(*) AS n FROM (SELECT m.medicine_id FROM medicine m "
                + "LEFT JOIN batch b ON b.medicine_id = m.medicine_id AND b.expiry_date >= CURDATE() "
                + "WHERE m.active = 1 GROUP BY m.medicine_id, m.reorder_level "
                + "HAVING COALESCE(SUM(b.quantity), 0) <= m.reorder_level) x");
    }

    public long expiringCount(int days) {
        return count("SELECT COUNT(*) AS n FROM batch WHERE quantity > 0 AND expiry_date >= CURDATE() "
                + "AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL " + days + " DAY)");
    }

    public long expiredWithStockCount() {
        return count("SELECT COUNT(*) AS n FROM batch WHERE quantity > 0 AND expiry_date < CURDATE()");
    }

    private long count(String sql) {
        Long n = Database.one("load dashboard figures", sql, rs -> rs.getLong("n"));
        return n == null ? 0 : n;
    }
}
