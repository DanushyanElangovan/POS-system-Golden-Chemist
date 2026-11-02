package com.goldenchemist.pos;

import com.goldenchemist.pos.controller.*;
import com.goldenchemist.pos.dao.BatchDao;
import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.model.*;
import com.goldenchemist.pos.util.PosException;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Headless end-to-end check of the controller / DAO / report layers against a real database.
 * Run with ./test.sh (needs schema.sql + seed.sql loaded). It adds a few test rows to the database.
 */
public class SmokeTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    static void expectError(String name, Runnable r, String contains) {
        try { r.run(); check(name + " (expected an error)", false); }
        catch (PosException e) { check(name + " -> \"" + e.getMessage() + "\"", e.getMessage().contains(contains)); }
    }

    static int stock(int medicineId) { return new BatchDao().availableQty(medicineId); }

    public static void main(String[] args) throws Exception {
        Database.verifyConnection();
        AuthController auth = new AuthController();
        auth.ensureDefaultAdmin();

        System.out.println("Authentication");
        expectError("wrong password rejected", () -> auth.login("admin", "nope"), "Incorrect");
        Employee admin = auth.login("admin", "admin123");
        check("admin can sign in", admin.getRole() == Role.ADMIN);

        System.out.println("Employees");
        EmployeeController emps = new EmployeeController();
        String uname = "cashier" + (System.currentTimeMillis() % 100000);
        Employee c = new Employee();
        c.setFullName("Test Cashier"); c.setUsername(uname); c.setRole(Role.CASHIER); c.setPhone("0771112223");
        c.setHiredDate(LocalDate.now());
        expectError("short password rejected", () -> emps.save(c, "123"), "at least 6");
        emps.save(c, "secret1");
        check("cashier created", !emps.search(uname).isEmpty());
        Employee self = emps.search("admin").get(0);
        expectError("cannot disable yourself", () -> emps.setActive(self, false), "own account");

        System.out.println("Medicines + inventory");
        MedicineController meds = new MedicineController();
        InventoryController inv = new InventoryController();
        String bc = "T" + System.currentTimeMillis();
        Medicine m = new Medicine();
        m.setBarcode(bc); m.setBrandName("Test Tablet"); m.setStrength("100mg"); m.setDosageForm("Tablet");
        m.setSellingPrice(new BigDecimal("10.00")); m.setReorderLevel(5);
        meds.save(m);
        Medicine saved = meds.search(bc).get(0);
        check("medicine saved", saved.getBrandName().equals("Test Tablet"));
        expectError("duplicate barcode rejected", () -> { Medicine d = new Medicine(); d.setBarcode(bc);
            d.setBrandName("Dup"); d.setSellingPrice(BigDecimal.ONE); meds.save(d); }, "already exists");
        expectError("expired stock cannot be received", () -> inv.receive(saved, null, "OLD", LocalDate.now().minusDays(1), 5, BigDecimal.ONE, null), "future");
        inv.receive(saved, null, "LATE", LocalDate.now().plusMonths(12), 10, new BigDecimal("6.00"), null);
        inv.receive(saved, null, "EARLY", LocalDate.now().plusMonths(2), 5, new BigDecimal("6.00"), null);
        check("15 units available", stock(saved.getId()) == 15);

        System.out.println("Billing (FEFO, prescription, discount, stock)");
        BillingController bill = new BillingController();
        bill.add(saved, 8);
        check("cart subtotal 80.00", bill.subtotal().compareTo(new BigDecimal("80.00")) == 0);
        expectError("cannot add more than stock", () -> bill.add(saved, 20), "Only 15");
        Sale sale = bill.checkout(null, bill.parseDiscount("10", Discount.Type.PERCENT), "CASH", "100", null);
        check("total after 10% = 72.00", sale.getTotal().compareTo(new BigDecimal("72.00")) == 0);
        check("change = 28.00", sale.getChangeAmount().compareTo(new BigDecimal("28.00")) == 0);
        check("invoice number format", sale.getInvoiceNo().matches("GC-\\d{8}-\\d{5}"));
        Sale loaded = new com.goldenchemist.pos.dao.SaleDao().findById(sale.getId());
        check("FEFO split: 5 from EARLY + 3 from LATE", loaded.getItems().size() == 2
                && loaded.getItems().get(0).getBatchNo().equals("EARLY") && loaded.getItems().get(0).getQuantity() == 5
                && loaded.getItems().get(1).getQuantity() == 3);
        check("stock now 7", stock(saved.getId()) == 7);

        Medicine rx = meds.search("Amoxicillin").get(0);
        bill.add(rx, 2);
        expectError("Rx medicine needs prescription ref", () -> bill.checkout(null, Discount.NONE, "CARD", null, null), "prescription");
        Sale rxSale = bill.checkout(null, Discount.NONE, "CARD", null, "Dr. Silva #123");
        check("Rx sale with reference completes", rxSale.getTotal().signum() > 0);
        bill.add(saved, 1);
        expectError("cash short of total rejected", () -> bill.checkout(null, Discount.NONE, "CASH", "5", null), "less than");
        check("failed checkout did not change stock", stock(saved.getId()) == 7);
        bill.clear();

        System.out.println("Void + restock");
        InvoiceController invc = new InvoiceController();
        List<Sale> found = invc.search(sale.getInvoiceNo(), LocalDate.now(), LocalDate.now(), null);
        check("invoice found by number", found.size() == 1);
        expectError("void needs a reason", () -> invc.voidSale(found.get(0), " "), "reason");
        invc.voidSale(found.get(0), "Customer changed mind");
        check("stock restored to 15", stock(saved.getId()) == 15);
        expectError("cannot void twice", () -> invc.voidSale(found.get(0), "again"), "already");

        System.out.println("Role enforcement");
        auth.login(uname, "secret1");
        expectError("cashier blocked from employees", () -> emps.search(""), "does not allow");
        expectError("cashier blocked from medicines screen", () -> meds.search(""), "does not allow");
        expectError("cashier blocked from reports", () -> new ReportController().run(ReportController.Type.LOW_STOCK, null, null, 30), "does not allow");
        bill.add(saved, 1);
        expectError("cashier discount capped at 10%", () -> bill.parseDiscount("50", Discount.Type.PERCENT), "at most 10");
        bill.clear();
        auth.login("admin", "admin123");

        System.out.println("Dashboard");
        DashboardController dash = new DashboardController();
        DashboardController.Summary s = dash.summary();
        check("dashboard summary loads (voided sale excluded, Rx sale counted)",
                s.todaySales.compareTo(new BigDecimal("36")) >= 0 && s.activeMedicines > 5);

        System.out.println("Reports -> PDF / Excel");
        ReportController rc = new ReportController();
        File dir = new File(System.getProperty("java.io.tmpdir"), "pos-reports"); dir.mkdirs();
        JasperPrint inv1 = rc.invoice(rxSale.getId());
        rc.exportPdf(inv1, new File(dir, "invoice.pdf"));
        check("invoice PDF", new File(dir, "invoice.pdf").length() > 1000);
        for (ReportController.Type t : ReportController.Type.values()) {
            JasperPrint p = rc.run(t, LocalDate.now().minusDays(30), LocalDate.now(), 90);
            File f = new File(dir, t.template + ".pdf");
            rc.exportPdf(p, f);
            check(t.label + " PDF (" + p.getPages().size() + " page)", f.length() > 1000);
        }
        rc.exportExcel(rc.run(ReportController.Type.SALES, LocalDate.now().minusDays(30), LocalDate.now(), 90), new File(dir, "sales.xls"));
        check("sales report Excel", new File(dir, "sales.xls").length() > 500);

        System.out.println("\n" + passed + " passed, " + failed + " failed   (PDFs in " + dir + ")");
        System.exit(failed == 0 ? 0 : 1);
    }
}
