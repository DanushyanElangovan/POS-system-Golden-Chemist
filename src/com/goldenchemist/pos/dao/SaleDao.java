package com.goldenchemist.pos.dao;

import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.db.Sql;
import com.goldenchemist.pos.model.CartLine;
import com.goldenchemist.pos.model.Discount;
import com.goldenchemist.pos.model.Sale;
import com.goldenchemist.pos.model.SaleItem;
import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.PosException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {

    private static final String SELECT =
            "SELECT s.sale_id, s.invoice_no, s.sale_date, s.employee_id, e.full_name AS employee_name, s.customer_id, "
                    + "c.full_name AS customer_name, s.subtotal, s.discount, s.total, s.paid_amount, s.change_amount, "
                    + "s.payment_method, s.status, s.prescription_ref, s.void_reason "
                    + "FROM sale s JOIN employee e ON e.employee_id = s.employee_id "
                    + "LEFT JOIN customer c ON c.customer_id = s.customer_id ";

    static Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("sale_id"));
        s.setInvoiceNo(rs.getString("invoice_no"));
        s.setSaleDate(Sql.getDateTime(rs, "sale_date"));
        s.setEmployeeId(rs.getInt("employee_id"));
        s.setEmployeeName(rs.getString("employee_name"));
        s.setCustomerId(Sql.getInteger(rs, "customer_id"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setSubtotal(rs.getBigDecimal("subtotal"));
        s.setDiscount(rs.getBigDecimal("discount"));
        s.setTotal(rs.getBigDecimal("total"));
        s.setPaidAmount(rs.getBigDecimal("paid_amount"));
        s.setChangeAmount(rs.getBigDecimal("change_amount"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setStatus(rs.getString("status"));
        s.setPrescriptionRef(rs.getString("prescription_ref"));
        s.setVoidReason(rs.getString("void_reason"));
        return s;
    }

    /**
     * Completes a sale in ONE transaction: prices are read from the database, stock is taken from the
     * batches that expire first (FEFO, expired batches are never used) and every change is logged.
     */
    public Sale checkout(List<CartLine> lines, Integer customerId, int employeeId, Discount discount,
                         String paymentMethod, BigDecimal tendered, String prescriptionRef) {
        return Database.inTransaction("complete the sale", c -> {
            LocalDateTime now = LocalDateTime.now();
            long saleId = Sql.insert(c,
                    "INSERT INTO sale (invoice_no, sale_date, employee_id, customer_id, payment_method, prescription_ref) "
                            + "VALUES (?,?,?,?,?,?)",
                    "TMP-" + System.nanoTime(), now, employeeId, customerId, paymentMethod, prescriptionRef);
            String invoiceNo = "GC-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                    + String.format("%05d", saleId);

            BigDecimal subtotal = BigDecimal.ZERO;
            boolean needsRx = false;
            for (CartLine line : lines) {
                Object[] med = Sql.queryOne(c,
                        "SELECT brand_name, selling_price, requires_prescription, active FROM medicine "
                                + "WHERE medicine_id = ?",
                        rs -> new Object[]{rs.getString("brand_name"), rs.getBigDecimal("selling_price"),
                                rs.getBoolean("requires_prescription"), rs.getBoolean("active")},
                        line.getMedicine().getId());
                if (med == null || !((Boolean) med[3])) {
                    throw new PosException(line.getMedicine() + " is no longer available for sale.");
                }
                BigDecimal price = (BigDecimal) med[1];
                needsRx |= (Boolean) med[2];

                int remaining = line.getQuantity();
                List<Object[]> batches = Sql.queryList(c,
                        "SELECT batch_id, quantity FROM batch WHERE medicine_id = ? AND quantity > 0 "
                                + "AND expiry_date >= CURDATE() ORDER BY expiry_date, batch_id FOR UPDATE",
                        rs -> new Object[]{rs.getInt("batch_id"), rs.getInt("quantity")},
                        line.getMedicine().getId());
                for (Object[] b : batches) {
                    if (remaining == 0) {
                        break;
                    }
                    int batchId = (Integer) b[0];
                    int take = Math.min(remaining, (Integer) b[1]);
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(take));
                    Sql.exec(c, "UPDATE batch SET quantity = quantity - ? WHERE batch_id = ?", take, batchId);
                    Sql.exec(c, "INSERT INTO sale_item (sale_id, medicine_id, batch_id, quantity, unit_price, line_total) "
                            + "VALUES (?,?,?,?,?,?)", saleId, line.getMedicine().getId(), batchId, take, price, lineTotal);
                    Sql.exec(c, "INSERT INTO stock_movement (batch_id, change_qty, reason, reference, employee_id) "
                            + "VALUES (?,?,?,?,?)", batchId, -take, "SALE", invoiceNo, employeeId);
                    subtotal = subtotal.add(lineTotal);
                    remaining -= take;
                }
                if (remaining > 0) {
                    throw new PosException("Not enough stock for " + med[0] + ": " + remaining
                            + " unit(s) short. Refresh the cart and try again.");
                }
            }
            if (needsRx && prescriptionRef == null) {
                throw new PosException("A prescription reference is required for prescription-only medicines.");
            }

            subtotal = Money.scale(subtotal);
            BigDecimal disc = discount.amountFor(subtotal);
            BigDecimal total = subtotal.subtract(disc);
            BigDecimal paid = "CARD".equals(paymentMethod) ? total : tendered;
            if (paid == null || paid.compareTo(total) < 0) {
                throw new PosException("The amount received is less than the total of " + Money.format(total) + ".");
            }
            BigDecimal change = paid.subtract(total);
            Sql.exec(c, "UPDATE sale SET invoice_no=?, subtotal=?, discount=?, total=?, paid_amount=?, change_amount=? "
                    + "WHERE sale_id=?", invoiceNo, subtotal, disc, total, paid, change, saleId);

            Sale sale = new Sale();
            sale.setId((int) saleId);
            sale.setInvoiceNo(invoiceNo);
            sale.setSubtotal(subtotal);
            sale.setDiscount(disc);
            sale.setTotal(total);
            sale.setPaidAmount(paid);
            sale.setChangeAmount(change);
            sale.setPaymentMethod(paymentMethod);
            sale.setSaleDate(now);
            return sale;
        });
    }

    public Sale findById(int saleId) {
        Sale s = Database.one("load the invoice", SELECT + "WHERE s.sale_id = ?", SaleDao::map, saleId);
        if (s != null) {
            s.getItems().addAll(Database.list("load invoice items",
                    "SELECT si.medicine_id, CONCAT(m.brand_name, IFNULL(CONCAT(' ', m.strength), '')) AS medicine_name, "
                            + "si.batch_id, b.batch_no, b.expiry_date, si.quantity, si.unit_price, si.line_total "
                            + "FROM sale_item si JOIN medicine m ON m.medicine_id = si.medicine_id "
                            + "JOIN batch b ON b.batch_id = si.batch_id WHERE si.sale_id = ? ORDER BY si.sale_item_id",
                    rs -> {
                        SaleItem i = new SaleItem();
                        i.setMedicineId(rs.getInt("medicine_id"));
                        i.setMedicineName(rs.getString("medicine_name"));
                        i.setBatchId(rs.getInt("batch_id"));
                        i.setBatchNo(rs.getString("batch_no"));
                        i.setExpiryDate(Sql.getDate(rs, "expiry_date"));
                        i.setQuantity(rs.getInt("quantity"));
                        i.setUnitPrice(rs.getBigDecimal("unit_price"));
                        i.setLineTotal(rs.getBigDecimal("line_total"));
                        return i;
                    }, saleId));
        }
        return s;
    }

    /** status may be null (all), COMPLETED or VOIDED. */
    public List<Sale> search(String q, LocalDate from, LocalDate to, String status, Integer onlyEmployeeId) {
        String like = "%" + q.trim() + "%";
        List<Object> args = new ArrayList<>();
        StringBuilder sb = new StringBuilder(SELECT);
        sb.append("WHERE (s.invoice_no LIKE ? OR c.full_name LIKE ? OR c.phone LIKE ?) ");
        args.add(like);
        args.add(like);
        args.add(like);
        sb.append("AND DATE(s.sale_date) BETWEEN ? AND ? ");
        args.add(from);
        args.add(to);
        if (status != null) {
            sb.append("AND s.status = ? ");
            args.add(status);
        }
        if (onlyEmployeeId != null) {
            sb.append("AND s.employee_id = ? ");
            args.add(onlyEmployeeId);
        }
        sb.append("ORDER BY s.sale_id DESC LIMIT 500");
        return Database.list("load invoices", sb.toString(), SaleDao::map, args.toArray());
    }

    /** Cancels a completed sale and puts every sold unit back into its batch. */
    public void voidSale(int saleId, String reason, int employeeId) {
        Database.inTransaction("void the invoice", c -> {
            String status = Sql.queryOne(c, "SELECT status FROM sale WHERE sale_id = ? FOR UPDATE",
                    rs -> rs.getString("status"), saleId);
            if (status == null) {
                throw new PosException("The invoice no longer exists.");
            }
            if (!Sale.COMPLETED.equals(status)) {
                throw new PosException("This invoice has already been voided.");
            }
            String invoiceNo = Sql.queryOne(c, "SELECT invoice_no FROM sale WHERE sale_id = ?",
                    rs -> rs.getString("invoice_no"), saleId);
            List<int[]> items = Sql.queryList(c, "SELECT batch_id, quantity FROM sale_item WHERE sale_id = ?",
                    rs -> new int[]{rs.getInt("batch_id"), rs.getInt("quantity")}, saleId);
            for (int[] it : items) {
                Sql.exec(c, "UPDATE batch SET quantity = quantity + ? WHERE batch_id = ?", it[1], it[0]);
                Sql.exec(c, "INSERT INTO stock_movement (batch_id, change_qty, reason, reference, employee_id) "
                        + "VALUES (?,?,?,?,?)", it[0], it[1], "VOID", invoiceNo, employeeId);
            }
            Sql.exec(c, "UPDATE sale SET status = 'VOIDED', void_reason = ?, voided_by = ?, voided_at = ? "
                    + "WHERE sale_id = ?", reason, employeeId, LocalDateTime.now(), saleId);
            return null;
        });
    }
}
