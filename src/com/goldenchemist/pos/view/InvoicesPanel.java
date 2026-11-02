package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.InvoiceController;
import com.goldenchemist.pos.controller.ReportController;
import com.goldenchemist.pos.controller.Session;
import com.goldenchemist.pos.model.Sale;
import com.goldenchemist.pos.util.Strings;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InvoicesPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient InvoiceController controller = new InvoiceController();
    private final transient ReportController reports = new ReportController();
    private final JTextField search = Ui.field(16);
    private final DateField from = new DateField(LocalDate.now().minusDays(7));
    private final DateField to = new DateField();
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All", "Completed", "Voided"});
    private final ObjectTableModel<Sale> model = new ObjectTableModel<>(
            new String[]{"Invoice", "Date / time", "Cashier", "Customer", "Payment", "Total", "Status"},
            s -> new Object[]{s.getInvoiceNo(), s.getSaleDate().format(DT), s.getEmployeeName(),
                    Strings.nz(s.getCustomerName()).isEmpty() ? "Walk-in" : s.getCustomerName(),
                    s.getPaymentMethod(), s.getTotal(), s.isVoided() ? "VOIDED" : "Completed"});
    private final PosTable table = Ui.table(model);

    public InvoicesPanel() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Ui.title("Invoices"), BorderLayout.WEST);
        JButton go = Ui.neutral("Search");
        go.addActionListener(e -> load());
        header.add(Ui.row(Ui.muted("Find"), search, Ui.muted("From"), from, Ui.muted("To"), to, status, go),
                BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        Ui.widths(table, 150, 130, 160, 160, 70, 90, 80);
        table.setRowStyler(new PosTable.RowStyler() {
            @Override
            public Color background(Object item) {
                return ((Sale) item).isVoided() ? Theme.DANGER_SOFT : null;
            }

            @Override
            public Color foreground(Object item) {
                return ((Sale) item).isVoided() ? Theme.DANGER : null;
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    view();
                }
            }
        });
        page.add(Ui.scroll(table), BorderLayout.CENTER);

        JButton view = Ui.primary("View / print invoice");
        view.addActionListener(e -> view());
        JPanel actions = Ui.buttonRow(view);
        if (Session.user().getRole().canVoidSales()) {
            JButton voidBtn = Ui.danger("Void invoice");
            voidBtn.addActionListener(e -> voidSelected());
            actions = Ui.buttonRow(view, voidBtn);
        } else {
            actions.add(javax.swing.Box.createHorizontalStrut(12));
            actions.add(Ui.muted("You can see your own invoices. Voiding needs a pharmacist or administrator."));
        }
        page.add(actions, BorderLayout.SOUTH);
        Ui.onTextChange(search, this::load);
        status.addActionListener(e -> load());
    }

    private Sale selected() {
        return (Sale) table.itemAt(table.getSelectedRow());
    }

    private void view() {
        Sale s = selected();
        if (s == null) {
            Ui.error(this, "Select an invoice first.");
            return;
        }
        Ui.guard(this, () -> ReportViewerDialog.show(this, "Invoice " + s.getInvoiceNo(), reports.invoice(s.getId())));
    }

    private void voidSelected() {
        Sale s = selected();
        if (s == null) {
            Ui.error(this, "Select an invoice first.");
            return;
        }
        if (s.isVoided()) {
            Ui.error(this, "This invoice is already voided.");
            return;
        }
        String reason = JOptionPane.showInputDialog(this,
                "Void invoice " + s.getInvoiceNo() + "?\nAll medicines return to stock.\n\nReason:",
                "Void invoice", JOptionPane.WARNING_MESSAGE);
        if (reason == null) {
            return;
        }
        if (Ui.guard(this, () -> controller.voidSale(s, reason))) {
            Ui.info(this, "Invoice voided and stock restored.");
            load();
        }
    }

    private void load() {
        String[] keys = {null, Sale.COMPLETED, Sale.VOIDED};
        Ui.guard(this, () -> model.setItems(controller.search(search.getText(), from.getDate(), to.getDate(),
                keys[Math.max(0, status.getSelectedIndex())])));
    }

    @Override
    public void onShow() {
        to.setDate(LocalDate.now());
        load();
    }
}
