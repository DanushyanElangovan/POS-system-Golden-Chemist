package com.goldenchemist.pos.view;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.controller.InventoryController;
import com.goldenchemist.pos.dao.BatchDao;
import com.goldenchemist.pos.model.Batch;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.StockMovement;
import com.goldenchemist.pos.model.Supplier;
import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.Strings;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Stock by batch, receiving goods, manual adjustments and the stock history. */
public class InventoryPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final Supplier NO_SUPPLIER = new Supplier();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static {
        NO_SUPPLIER.setName("(none / unknown)");
    }

    private final transient InventoryController controller = new InventoryController();

    // stock tab
    private final JTextField search = Ui.field(18);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"Sellable stock", "Expiring soon",
            "Expired (still in stock)", "All batches"});
    private final ObjectTableModel<Batch> batchModel = new ObjectTableModel<>(
            new String[]{"Medicine", "Batch", "Expiry", "Days left", "Qty", "Cost price", "Supplier", "Received"},
            b -> new Object[]{b.getMedicineName(), b.getBatchNo(), b.getExpiryDate().toString(), b.daysToExpiry(),
                    b.getQuantity(), b.getCostPrice(), Strings.nz(b.getSupplierName()), b.getReceivedDate().toString()});
    private final PosTable batchTable = Ui.table(batchModel);
    private final JSpinner delta = new JSpinner(new SpinnerNumberModel(-1, -100000, 100000, 1));
    private final JComboBox<String> reason = new JComboBox<>(new String[]{"Count correction", "Write-off (damaged / expired)"});
    private final JTextField note = Ui.field(24);

    // receive tab
    private final JTextField medFilter = Ui.field(14);
    private final JComboBox<Medicine> medicine = new JComboBox<>();
    private final JComboBox<Supplier> supplier = new JComboBox<>();
    private final JTextField batchNo = Ui.field(14);
    private final DateField expiry = new DateField(LocalDate.now().plusYears(1));
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
    private final JTextField cost = Ui.field(10);
    private final DateField received = new DateField();
    private transient List<Medicine> allMedicines = new ArrayList<>();

    // history tab
    private final ObjectTableModel<StockMovement> moveModel = new ObjectTableModel<>(
            new String[]{"When", "Medicine", "Batch", "Change", "Reason", "Reference", "By"},
            s -> new Object[]{s.getCreatedAt() == null ? "" : s.getCreatedAt().format(DT), s.getMedicineName(),
                    s.getBatchNo(), s.getChangeQty(), s.getReason(), Strings.nz(s.getReference()),
                    Strings.nz(s.getEmployeeName())});
    private final PosTable moveTable = Ui.table(moveModel);

    public InventoryPanel() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);
        page.add(Ui.title("Inventory"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Stock by batch", buildStockTab());
        tabs.addTab("Receive stock", buildReceiveTab());
        tabs.addTab("Stock history", buildHistoryTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) {
                loadHistory();
            }
        });
        page.add(tabs, BorderLayout.CENTER);
    }

    // ---- stock tab ---------------------------------------------------------------------------

    private JPanel buildStockTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        p.setOpaque(false);
        JButton refresh = Ui.neutral("Refresh");
        refresh.addActionListener(e -> loadBatches());
        filter.addActionListener(e -> loadBatches());
        Ui.onTextChange(search, this::loadBatches);
        JPanel top = Ui.row(Ui.muted("Search"), search, Ui.muted("Show"), filter, refresh);
        p.add(top, BorderLayout.NORTH);

        Ui.widths(batchTable, 220, 90, 90, 70, 60, 80, 160, 90);
        batchTable.setRowStyler(new PosTable.RowStyler() {
            @Override
            public Color background(Object item) {
                Batch b = (Batch) item;
                if (b.getQuantity() > 0 && b.isExpired()) {
                    return Theme.DANGER_SOFT;
                }
                if (b.getQuantity() > 0 && b.daysToExpiry() <= AppConfig.expiryWarningDays()) {
                    return Theme.WARN_SOFT;
                }
                return null;
            }

            @Override
            public Color foreground(Object item) {
                return ((Batch) item).getQuantity() == 0 ? Theme.MUTED : null;
            }
        });
        p.add(Ui.scroll(batchTable), BorderLayout.CENTER);

        JButton apply = Ui.primary("Apply change");
        apply.addActionListener(e -> {
            Batch b = (Batch) batchTable.itemAt(batchTable.getSelectedRow());
            int d = (Integer) delta.getValue();
            String r = reason.getSelectedIndex() == 0 ? "ADJUSTMENT" : "WRITE_OFF";
            if (Ui.guard(this, () -> controller.adjust(b, d, r, note.getText()))) {
                note.setText("");
                loadBatches();
            }
        });
        JPanel adjust = Ui.card();
        adjust.setLayout(new BorderLayout(10, 6));
        JLabel hint = Ui.muted("Select a batch above, then enter the change (use a negative number to remove units).");
        adjust.add(hint, BorderLayout.NORTH);
        adjust.add(Ui.row(Ui.bold("Change"), delta, Ui.bold("Reason"), reason, Ui.bold("Note"), note, apply),
                BorderLayout.CENTER);
        p.add(adjust, BorderLayout.SOUTH);
        return p;
    }

    private void loadBatches() {
        String[] keys = {BatchDao.F_IN_STOCK, BatchDao.F_EXPIRING, BatchDao.F_EXPIRED, BatchDao.F_ALL};
        String f = keys[Math.max(0, filter.getSelectedIndex())];
        Ui.guard(this, () -> batchModel.setItems(controller.batches(search.getText(), f)));
    }

    // ---- receive tab -------------------------------------------------------------------------

    private JPanel buildReceiveTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(460, 100));
        card.add(Ui.bold("Receive goods from a supplier (creates a new batch)"), BorderLayout.NORTH);

        FormPanel f = new FormPanel();
        f.add("Find medicine (type to filter)", medFilter);
        f.add("Medicine *", medicine);
        f.add("Supplier", supplier);
        f.add("Batch number * (as printed on the pack)", batchNo);
        f.add("Expiry date *", expiry);
        f.add("Quantity received *", qty);
        f.add("Cost price per unit *", cost);
        f.add("Received date", received);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(f, BorderLayout.NORTH);
        card.add(wrap, BorderLayout.CENTER);

        JButton receive = Ui.accent("Receive stock");
        receive.addActionListener(e -> {
            boolean ok = Ui.guard(this, () -> controller.receive((Medicine) medicine.getSelectedItem(),
                    (Supplier) supplier.getSelectedItem() == NO_SUPPLIER ? null : (Supplier) supplier.getSelectedItem(),
                    batchNo.getText(), expiry.getDate(), (Integer) qty.getValue(),
                    Money.parse("Cost price", cost.getText()), received.getDate()));
            if (ok) {
                Ui.info(this, "Stock received.");
                batchNo.setText("");
                cost.setText("");
                qty.setValue(1);
                loadBatches();
            }
        });
        card.add(Ui.buttonRow(receive), BorderLayout.SOUTH);
        outer.add(card, BorderLayout.WEST);

        Ui.onTextChangeNow(medFilter, this::applyMedicineFilter);
        return outer;
    }

    private void loadReceiveLists() {
        Ui.guard(this, () -> {
            allMedicines = controller.activeMedicines();
            applyMedicineFilter();
            DefaultComboBoxModel<Supplier> sm = new DefaultComboBoxModel<>();
            sm.addElement(NO_SUPPLIER);
            for (Supplier s : controller.activeSuppliers()) {
                sm.addElement(s);
            }
            supplier.setModel(sm);
        });
    }

    private void applyMedicineFilter() {
        String t = medFilter.getText().trim().toLowerCase();
        DefaultComboBoxModel<Medicine> m = new DefaultComboBoxModel<>();
        for (Medicine med : allMedicines) {
            if (t.isEmpty() || med.toString().toLowerCase().contains(t)
                    || Strings.nz(med.getGenericName()).toLowerCase().contains(t) || med.getBarcode().contains(t)) {
                m.addElement(med);
            }
        }
        medicine.setModel(m);
    }

    // ---- history tab -------------------------------------------------------------------------

    private JPanel buildHistoryTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        Ui.widths(moveTable, 130, 220, 90, 60, 90, 150, 140);
        p.add(Ui.scroll(moveTable), BorderLayout.CENTER);
        p.add(Ui.muted("The latest 300 stock movements: receipts, sales, voids and manual adjustments."),
                BorderLayout.NORTH);
        return p;
    }

    private void loadHistory() {
        Ui.guard(this, () -> moveModel.setItems(controller.movements()));
    }

    @Override
    public void onShow() {
        loadBatches();
        loadReceiveLists();
    }
}
