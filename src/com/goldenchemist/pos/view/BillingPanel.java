package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.BillingController;
import com.goldenchemist.pos.controller.CustomerController;
import com.goldenchemist.pos.controller.ReportController;
import com.goldenchemist.pos.model.CartLine;
import com.goldenchemist.pos.model.Customer;
import com.goldenchemist.pos.model.Discount;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.model.Sale;
import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;

/** The till: find medicines, build the cart, take payment, print the invoice. */
public class BillingPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final Customer WALK_IN = new Customer();

    static {
        WALK_IN.setFullName("Walk-in customer");
    }

    private final transient BillingController controller = new BillingController();
    private final transient CustomerController customerController = new CustomerController();
    private final transient ReportController reports = new ReportController();

    // finder
    private final JTextField searchField = Ui.field(10);
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final ObjectTableModel<Medicine> resultModel = new ObjectTableModel<>(
            new String[]{"Medicine", "Generic", "Form", "Rx", "Price", "Stock"},
            m -> new Object[]{m.toString(), Strings.nz(m.getGenericName()), Strings.nz(m.getDosageForm()),
                    m.isRequiresPrescription() ? "Rx" : "", m.getSellingPrice(), m.getStockQty()});
    private final PosTable resultTable = Ui.table(resultModel);

    // cart
    private final CartModel cartModel = new CartModel();
    private final PosTable cartTable = Ui.table(cartModel);

    // checkout
    private final JComboBox<Customer> customerBox = new JComboBox<>();
    private final JTextField rxField = Ui.field(14);
    private final JTextField discountField = Ui.field(6);
    private final JComboBox<String> discountType = new JComboBox<>(new String[]{"Rs.", "%"});
    private final JComboBox<String> paymentBox = new JComboBox<>(new String[]{"Cash", "Card"});
    private final JTextField receivedField = Ui.field(10);
    private final JLabel subtotalLabel = Ui.right("0.00");
    private final JLabel discountLabel = Ui.right("0.00");
    private final JLabel totalLabel = new JLabel("0.00", SwingConstants.RIGHT);
    private final JLabel changeLabel = Ui.right("0.00");
    private final JLabel rxNotice = new JLabel(" ");

    /** Table model over the controller's cart; quantity is editable. */
    private class CartModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] cols = {"#", "Medicine", "Qty", "Unit price", "Line total"};

        @Override
        public int getRowCount() {
            return controller.getCart().size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 || c == 2 ? Integer.class : c >= 3 ? BigDecimal.class : String.class;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 2;
        }

        @Override
        public Object getValueAt(int r, int c) {
            CartLine l = controller.getCart().get(r);
            switch (c) {
                case 0: return r + 1;
                case 1: return l.getMedicine().toString() + (l.getMedicine().isRequiresPrescription() ? "  [Rx]" : "");
                case 2: return l.getQuantity();
                case 3: return l.getUnitPrice();
                default: return l.getLineTotal();
            }
        }

        @Override
        public void setValueAt(Object v, int r, int c) {
            if (c != 2) {
                return;
            }
            try {
                int q = Integer.parseInt(String.valueOf(v).trim());
                Ui.guard(BillingPanel.this, () -> controller.setQuantity(r, q));
            } catch (NumberFormatException e) {
                Ui.error(BillingPanel.this, "Quantity must be a whole number.");
            }
            cartChanged();
        }
    }

    public BillingPanel() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);
        page.add(Ui.title("Billing"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildFinder(), buildCartSide());
        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);
        split.setDividerSize(10);
        split.setBorder(null);
        split.setOpaque(false);
        page.add(split, BorderLayout.CENTER);

        buildFinderMinimum(split);
        wireKeys();
        updateTotals();
    }

    private void buildFinderMinimum(JSplitPane split) {
        split.getLeftComponent().setMinimumSize(new Dimension(500, 100));
        split.getRightComponent().setMinimumSize(new Dimension(520, 100));
    }

    // ---- left: finder ------------------------------------------------------------------------

    private JPanel buildFinder() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        JButton add = Ui.primary("Add to cart");
        add.addActionListener(e -> addSelected());
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(Ui.bold("Scan barcode or search medicine  (F2)"), BorderLayout.NORTH);
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(Ui.row(Ui.muted("Qty"), qtySpinner, add), BorderLayout.EAST);
        top.add(searchRow, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);

        Ui.widths(resultTable, 180, 110, 65, 45, 70, 60);
        resultTable.setRowStyler(new PosTable.RowStyler() {
            @Override
            public Color background(Object item) {
                return ((Medicine) item).getStockQty() <= 0 ? Theme.DANGER_SOFT : null;
            }

            @Override
            public Color foreground(Object item) {
                return ((Medicine) item).getStockQty() <= 0 ? Theme.DANGER : null;
            }
        });
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelected();
                }
            }
        });
        p.add(Ui.scroll(resultTable), BorderLayout.CENTER);
        p.add(Ui.muted("Double-click a medicine to add it. Red rows are out of stock."), BorderLayout.SOUTH);

        Ui.onTextChange(searchField, this::search);
        searchField.addActionListener(e -> onEnterInSearch());
        return p;
    }

    private void search() {
        Ui.guard(this, () -> resultModel.setItems(controller.searchMedicines(searchField.getText())));
    }

    /** Enter: an exact barcode goes straight into the cart (barcode scanner); otherwise add the only match. */
    private void onEnterInSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        Ui.guard(this, () -> {
            Medicine byCode = controller.findByBarcode(text);
            if (byCode != null) {
                controller.add(byCode, (Integer) qtySpinner.getValue());
                searchField.setText("");
                qtySpinner.setValue(1);
                cartChanged();
                return;
            }
            List<Medicine> found = controller.searchMedicines(text);
            resultModel.setItems(found);
            if (found.size() == 1) {
                controller.add(found.get(0), (Integer) qtySpinner.getValue());
                searchField.setText("");
                qtySpinner.setValue(1);
                cartChanged();
            } else if (found.isEmpty()) {
                throw new PosException("No medicine found for \"" + text + "\".");
            } else {
                resultTable.setRowSelectionInterval(0, 0);
            }
        });
    }

    private void addSelected() {
        Medicine m = (Medicine) resultTable.itemAt(resultTable.getSelectedRow());
        if (m == null) {
            Ui.error(this, "Select a medicine in the list first.");
            return;
        }
        if (Ui.guard(this, () -> controller.add(m, (Integer) qtySpinner.getValue()))) {
            qtySpinner.setValue(1);
            cartChanged();
            searchField.requestFocusInWindow();
        }
    }

    // ---- right: cart + checkout --------------------------------------------------------------

    private JPanel buildCartSide() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        Ui.widths(cartTable, 30, 230, 50, 80, 90);
        cartTable.setAutoCreateRowSorter(false);
        JPanel cartBox = new JPanel(new BorderLayout(0, 6));
        cartBox.setOpaque(false);
        cartBox.add(Ui.bold("Cart  (click a quantity to edit it)"), BorderLayout.NORTH);
        cartBox.add(Ui.scroll(cartTable), BorderLayout.CENTER);

        JButton plus = Ui.neutral("+1");
        JButton minus = Ui.neutral("-1");
        JButton remove = Ui.neutral("Remove");
        JButton clear = Ui.neutral("Clear cart");
        plus.addActionListener(e -> bump(1));
        minus.addActionListener(e -> bump(-1));
        remove.addActionListener(e -> {
            int r = cartTable.getSelectedRow();
            if (r >= 0) {
                controller.remove(r);
                cartChanged();
            }
        });
        clear.addActionListener(e -> {
            if (controller.getCart().isEmpty() || Ui.confirm(this, "Remove everything from the cart?")) {
                resetSale();
            }
        });
        cartBox.add(Ui.buttonRow(plus, minus, remove, clear), BorderLayout.SOUTH);
        p.add(cartBox, BorderLayout.CENTER);
        p.add(buildCheckout(), BorderLayout.SOUTH);
        return p;
    }

    private void bump(int d) {
        int r = cartTable.getSelectedRow();
        if (r < 0) {
            return;
        }
        int current = controller.getCart().get(r).getQuantity();
        Ui.guard(this, () -> controller.setQuantity(r, current + d));
        cartChanged();
        if (r < cartModel.getRowCount()) {
            cartTable.setRowSelectionInterval(r, r);
        }
    }

    private JPanel buildCheckout() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));

        JButton newCustomer = Ui.neutral("New");
        newCustomer.addActionListener(e -> quickAddCustomer());
        JPanel customerRow = new JPanel(new BorderLayout(8, 0));
        customerRow.setOpaque(false);
        customerRow.add(customerBox, BorderLayout.CENTER);
        customerRow.add(newCustomer, BorderLayout.EAST);

        JPanel head = new JPanel(new GridLayout(2, 2, 10, 2));
        head.setOpaque(false);
        head.add(Ui.muted("Customer"));
        head.add(Ui.muted("Prescription ref (doctor / Rx no.)"));
        head.add(customerRow);
        head.add(rxField);
        JPanel headWrap = new JPanel(new BorderLayout(0, 2));
        headWrap.setOpaque(false);
        headWrap.add(head, BorderLayout.CENTER);
        rxNotice.setForeground(Theme.DANGER);
        rxNotice.setFont(Theme.BOLD);
        headWrap.add(rxNotice, BorderLayout.SOUTH);
        card.add(headWrap, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 5));
        grid.setOpaque(false);
        grid.add(Ui.bold("Subtotal"));
        grid.add(subtotalLabel);
        JPanel disc = new JPanel(new BorderLayout(6, 0));
        disc.setOpaque(false);
        disc.add(Ui.bold("Discount"), BorderLayout.WEST);
        JPanel discIn = Ui.row(discountField, discountType);
        discIn.setOpaque(false);
        disc.add(discIn, BorderLayout.EAST);
        grid.add(disc);
        grid.add(discountLabel);
        grid.add(Ui.bold("TOTAL"));
        totalLabel.setFont(Theme.BIG);
        totalLabel.setForeground(Theme.GREEN);
        grid.add(totalLabel);
        grid.add(Ui.row(Ui.bold("Payment"), paymentBox));
        grid.add(new JLabel());
        grid.add(Ui.bold("Amount received"));
        grid.add(receivedField);
        grid.add(Ui.bold("Change"));
        changeLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        grid.add(changeLabel);
        card.add(grid, BorderLayout.CENTER);

        JButton complete = Ui.accent("Complete sale  (F9)");
        complete.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        complete.setPreferredSize(new Dimension(100, 46));
        complete.addActionListener(e -> completeSale());
        card.add(complete, BorderLayout.SOUTH);

        Ui.onTextChangeNow(discountField, this::updateTotals);
        Ui.onTextChangeNow(receivedField, this::updateTotals);
        discountType.addActionListener(e -> updateTotals());
        paymentBox.addActionListener(e -> {
            receivedField.setEnabled(paymentBox.getSelectedIndex() == 0);
            updateTotals();
        });
        return card;
    }

    private void quickAddCustomer() {
        JTextField name = Ui.field(18);
        JTextField phone = Ui.field(18);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Full name *"));
        form.add(name);
        form.add(new JLabel("Phone"));
        form.add(phone);
        if (JOptionPane.showConfirmDialog(this, form, "New customer", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        Customer c = new Customer();
        c.setFullName(name.getText());
        c.setPhone(phone.getText());
        if (Ui.guard(this, () -> customerController.save(c))) {
            loadCustomers();
            for (int i = 0; i < customerBox.getItemCount(); i++) {
                if (customerBox.getItemAt(i).getId() == c.getId()) {
                    customerBox.setSelectedIndex(i);
                }
            }
        }
    }

    private void loadCustomers() {
        Ui.guard(this, () -> {
            DefaultComboBoxModel<Customer> m = new DefaultComboBoxModel<>();
            m.addElement(WALK_IN);
            for (Customer c : controller.customers()) {
                m.addElement(c);
            }
            customerBox.setModel(m);
        });
    }

    // ---- totals and checkout -----------------------------------------------------------------

    private void cartChanged() {
        cartModel.fireTableDataChanged();
        updateTotals();
        search();
    }

    private void updateTotals() {
        BigDecimal subtotal = controller.subtotal();
        BigDecimal discount = BigDecimal.ZERO;
        discountLabel.setForeground(Theme.TEXT);
        try {
            Discount d = controller.parseDiscount(discountField.getText(),
                    discountType.getSelectedIndex() == 1 ? Discount.Type.PERCENT : Discount.Type.AMOUNT);
            discount = d.amountFor(subtotal);
        } catch (PosException e) {
            discountLabel.setForeground(Theme.DANGER);
        }
        BigDecimal total = subtotal.subtract(discount);
        subtotalLabel.setText(Money.plain(subtotal));
        discountLabel.setText("- " + Money.plain(discount));
        totalLabel.setText(Money.format(total));

        boolean rx = controller.needsPrescription();
        rxField.setBackground(rx && Strings.isBlank(rxField.getText()) ? Theme.WARN_SOFT : Color.WHITE);
        rxNotice.setText(rx ? "Prescription-only medicine in cart - reference required" : " ");

        if (paymentBox.getSelectedIndex() == 0) {
            try {
                BigDecimal received = Money.parse("Amount received", receivedField.getText());
                BigDecimal change = received.subtract(total);
                changeLabel.setText(Money.plain(change.max(BigDecimal.ZERO)));
                changeLabel.setForeground(change.signum() < 0 ? Theme.DANGER : Theme.GREEN);
                if (change.signum() < 0) {
                    changeLabel.setText("short " + Money.plain(change.negate()));
                }
            } catch (PosException e) {
                changeLabel.setText("-");
                changeLabel.setForeground(Theme.TEXT);
            }
        } else {
            changeLabel.setText("0.00");
            changeLabel.setForeground(Theme.TEXT);
        }
    }

    private void completeSale() {
        final Sale[] result = new Sale[1];
        boolean ok = Ui.guard(this, () -> {
            Discount d = controller.parseDiscount(discountField.getText(),
                    discountType.getSelectedIndex() == 1 ? Discount.Type.PERCENT : Discount.Type.AMOUNT);
            Customer c = (Customer) customerBox.getSelectedItem();
            boolean cash = paymentBox.getSelectedIndex() == 0;
            result[0] = controller.checkout(c == null || c.getId() == 0 ? null : c, d, cash ? "CASH" : "CARD",
                    receivedField.getText(), rxField.getText());
        });
        if (!ok) {
            cartChanged();
            return;
        }
        Sale s = result[0];
        resetFields();
        cartChanged();
        Ui.info(this, "Sale completed.\n\nInvoice: " + s.getInvoiceNo() + "\nTotal: " + Money.format(s.getTotal())
                + ("CASH".equals(s.getPaymentMethod()) ? "\nChange to give: " + Money.format(s.getChangeAmount()) : ""));
        Ui.guard(this, () -> ReportViewerDialog.show(this, "Invoice " + s.getInvoiceNo(), reports.invoice(s.getId())));
        searchField.requestFocusInWindow();
    }

    private void resetSale() {
        controller.clear();
        resetFields();
        cartChanged();
    }

    private void resetFields() {
        rxField.setText("");
        discountField.setText("");
        receivedField.setText("");
        paymentBox.setSelectedIndex(0);
        if (customerBox.getItemCount() > 0) {
            customerBox.setSelectedIndex(0);
        }
    }

    private void wireKeys() {
        bindKey("F9", () -> completeSale());
        bindKey("F2", () -> searchField.requestFocusInWindow());
    }

    private void bindKey(String key, Runnable r) {
        JComponent c = this;
        c.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        c.getActionMap().put(key, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    @Override
    public void onShow() {
        loadCustomers();
        search();
        cartModel.fireTableDataChanged();
        updateTotals();
        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }
}
