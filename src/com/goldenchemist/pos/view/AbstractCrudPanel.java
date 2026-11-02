package com.goldenchemist.pos.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

/**
 * Master/detail screen: searchable table on the left, edit form on the right.
 * Subclasses supply the columns, the form and the controller calls; call init() last in the constructor.
 */
public abstract class AbstractCrudPanel<T> extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final String heading;
    protected final transient ObjectTableModel<T> model;
    protected final PosTable table;
    protected final JTextField searchField = Ui.field(18);
    protected final FormPanel form = new FormPanel();
    protected final JLabel formTitle = Ui.bold("New record");
    /** The record being edited, or null while creating a new one. */
    protected transient T editing;

    protected AbstractCrudPanel(String heading, String[] columns) {
        this.heading = heading;
        this.model = new ObjectTableModel<>(columns, this::toRow);
        this.table = Ui.table(model);
    }

    // ---- to be supplied by each screen -------------------------------------------------------
    protected abstract Object[] toRow(T item);

    protected abstract List<T> fetch(String query);

    protected abstract void buildForm(FormPanel form);

    protected abstract void fillForm(T item);

    protected abstract void clearForm();

    /** Builds an object from the form (copying the id from {@link #editing}); throw PosException when invalid. */
    protected abstract T readForm();

    protected abstract void persist(T item);

    /** Extra buttons placed next to Save (for example Delete). */
    protected JButton[] extraButtons() {
        return new JButton[0];
    }

    protected int[] columnWidths() {
        return new int[0];
    }

    // ---- layout ------------------------------------------------------------------------------

    protected void init() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(Ui.title(heading), BorderLayout.WEST);
        JButton refresh = Ui.neutral("Refresh");
        refresh.addActionListener(e -> reload());
        header.add(Ui.row(Ui.muted("Search"), searchField, refresh), BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        Ui.widths(table, columnWidths());
        page.add(Ui.scroll(table), BorderLayout.CENTER);

        JPanel side = Ui.card();
        side.setLayout(new BorderLayout(0, 12));
        side.setPreferredSize(new Dimension(340, 100));
        side.add(formTitle, BorderLayout.NORTH);
        buildForm(form);
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(form, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(formWrap);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        side.add(sp, BorderLayout.CENTER);

        JButton save = Ui.primary("Save");
        JButton neu = Ui.neutral("New");
        save.addActionListener(e -> save());
        neu.addActionListener(e -> startNew());
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(save);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(neu);
        for (JButton b : extraButtons()) {
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(b);
        }
        side.add(buttons, BorderLayout.SOUTH);
        page.add(side, BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                T sel = selected();
                if (sel != null) {
                    editing = sel;
                    formTitle.setText("Edit record");
                    fillForm(sel);
                }
            }
        });
        Ui.onTextChange(searchField, this::reload);
        clearForm();
    }

    // ---- behaviour ---------------------------------------------------------------------------

    @Override
    public void onShow() {
        reload();
    }

    protected T selected() {
        int row = table.getSelectedRow();
        @SuppressWarnings("unchecked")
        T item = (T) table.itemAt(row);
        return item;
    }

    protected void reload() {
        Ui.guard(this, () -> model.setItems(fetch(searchField.getText())));
    }

    protected void startNew() {
        table.clearSelection();
        editing = null;
        formTitle.setText("New record");
        clearForm();
    }

    private void save() {
        boolean ok = Ui.guard(this, () -> {
            persist(readForm());
        });
        if (ok) {
            startNew();
            reload();
        }
    }
}
