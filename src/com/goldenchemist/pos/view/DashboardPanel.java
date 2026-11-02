package com.goldenchemist.pos.view;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.controller.DashboardController;
import com.goldenchemist.pos.model.Batch;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final transient DashboardController controller = new DashboardController();
    private final JLabel sales = new JLabel("-");
    private final JLabel invoices = new JLabel("-");
    private final JLabel low = new JLabel("-");
    private final JLabel expiring = new JLabel("-");
    private final JLabel expired = new JLabel("-");
    private final JLabel meds = new JLabel("-");

    private final ObjectTableModel<Medicine> lowModel = new ObjectTableModel<>(
            new String[]{"Medicine", "In stock", "Reorder level"},
            m -> new Object[]{m.toString(), m.getStockQty(), m.getReorderLevel()});
    private final ObjectTableModel<Batch> expModel = new ObjectTableModel<>(
            new String[]{"Medicine", "Batch", "Expiry", "Days", "Qty"},
            b -> new Object[]{b.getMedicineName(), b.getBatchNo(), b.getExpiryDate().toString(), b.daysToExpiry(),
                    b.getQuantity()});

    public DashboardPanel() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Ui.title("Dashboard"), BorderLayout.WEST);
        JButton refresh = Ui.neutral("Refresh");
        refresh.addActionListener(e -> onShow());
        JLabel date = Ui.muted(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) + "   ");
        header.add(Ui.row(date, refresh), BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 6, 12, 0));
        cards.setOpaque(false);
        cards.add(stat("Today's sales", sales, Theme.GREEN));
        cards.add(stat("Invoices today", invoices, Theme.GREEN));
        cards.add(stat("Active medicines", meds, Theme.GREEN));
        cards.add(stat("Low stock items", low, Theme.WARN));
        cards.add(stat("Expiring in " + AppConfig.expiryWarningDays() + " days", expiring, Theme.WARN));
        cards.add(stat("Expired, still in stock", expired, Theme.DANGER));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        PosTable lowTable = Ui.table(lowModel);
        Ui.widths(lowTable, 220, 70, 90);
        PosTable expTable = Ui.table(expModel);
        Ui.widths(expTable, 200, 80, 90, 50, 50);
        expTable.setRowStyler(new PosTable.RowStyler() {
            @Override
            public Color background(Object item) {
                return ((Batch) item).isExpired() ? Theme.DANGER_SOFT : null;
            }

            @Override
            public Color foreground(Object item) {
                return null;
            }
        });

        JPanel tables = new JPanel(new GridLayout(1, 2, 14, 0));
        tables.setOpaque(false);
        tables.add(section("Medicines to reorder", lowTable));
        tables.add(section("Batches expiring soon / expired", expTable));
        page.add(tables, BorderLayout.CENTER);
    }

    private JPanel stat(String caption, JLabel value, final Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 4)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(accent);
                g.fillRect(0, 0, 5, getHeight());
            }
        };
        p.setBackground(Theme.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 10)));
        JLabel c = Ui.muted(caption);
        value.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
        value.setForeground(accent);
        p.add(c, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel section(String title, PosTable table) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        p.add(Ui.bold(title), BorderLayout.NORTH);
        p.add(Ui.scroll(table), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void onShow() {
        Ui.guard(this, () -> {
            DashboardController.Summary s = controller.summary();
            sales.setText(Money.format(s.todaySales));
            invoices.setText(String.valueOf(s.todayInvoices));
            meds.setText(String.valueOf(s.activeMedicines));
            low.setText(String.valueOf(s.lowStock));
            expiring.setText(String.valueOf(s.expiringSoon));
            expired.setText(String.valueOf(s.expired));
            lowModel.setItems(controller.lowStockItems());
            expModel.setItems(controller.expiringBatches());
        });
    }
}
