package com.goldenchemist.pos.controller;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.model.Module;
import com.goldenchemist.pos.report.ReportService;
import com.goldenchemist.pos.util.PosException;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Builds report parameters and runs the Jasper templates. */
public class ReportController {

    /** The reports listed on the Reports screen. */
    public enum Type {
        SALES("Sales report", "sales_report", true, false,
                "All completed invoices in the period with totals."),
        TOP_MEDICINES("Top selling medicines", "top_medicines", true, false,
                "The 25 best selling medicines by units sold."),
        EMPLOYEE_SALES("Sales by employee", "employee_sales", true, false,
                "Invoices, discounts and net sales per employee."),
        LOW_STOCK("Low stock / reorder list", "low_stock", false, false,
                "Medicines at or below their reorder level."),
        EXPIRING("Expiring and expired stock", "expiring_stock", false, true,
                "Batches that expire within the chosen number of days, including expired ones."),
        VALUATION("Stock valuation", "stock_valuation", false, false,
                "Cost and retail value of sellable stock.");

        public final String label;
        public final String template;
        public final boolean usesDateRange;
        public final boolean usesDays;
        public final String description;

        Type(String label, String template, boolean usesDateRange, boolean usesDays, String description) {
            this.label = label;
            this.template = template;
            this.usesDateRange = usesDateRange;
            this.usesDays = usesDays;
            this.description = description;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ReportService service = new ReportService();

    private Map<String, Object> shopParams() {
        Map<String, Object> p = new HashMap<>();
        p.put("SHOP_NAME", AppConfig.shopName());
        p.put("SHOP_ADDRESS", AppConfig.get("shop.address", ""));
        p.put("SHOP_PHONE", AppConfig.get("shop.phone", ""));
        p.put("SHOP_REG", AppConfig.get("shop.regNo", ""));
        p.put("CURRENCY", AppConfig.currency());
        return p;
    }

    /** The printable invoice for one sale (billing and invoice screens). */
    public JasperPrint invoice(int saleId) {
        if (!Session.can(Module.BILLING) && !Session.can(Module.INVOICES)) {
            throw new PosException("Your role does not allow printing invoices.");
        }
        Map<String, Object> p = shopParams();
        p.put("SALE_ID", saleId);
        return service.fill("invoice", p);
    }

    public JasperPrint run(Type type, LocalDate from, LocalDate to, int days) {
        Session.require(Module.REPORTS);
        Map<String, Object> p = shopParams();
        if (type.usesDateRange) {
            if (from == null || to == null || to.isBefore(from)) {
                throw new PosException("Choose a valid date range (From must not be after To).");
            }
            p.put("FROM_DATE", java.sql.Date.valueOf(from));
            p.put("TO_DATE", java.sql.Date.valueOf(to));
        }
        if (type.usesDays) {
            if (days < 1 || days > 3650) {
                throw new PosException("Days must be between 1 and 3650.");
            }
            p.put("DAYS", days);
        }
        return service.fill(type.template, p);
    }

    public void exportPdf(JasperPrint print, File target) {
        service.exportPdf(print, target);
    }

    public void exportExcel(JasperPrint print, File target) {
        service.exportXls(print, target);
    }
}
