package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.MedicineController;
import com.goldenchemist.pos.model.Category;
import com.goldenchemist.pos.model.Medicine;
import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.util.Strings;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;

public class MedicinePanel extends AbstractCrudPanel<Medicine> {
    private static final long serialVersionUID = 1L;
    private static final Category NO_CATEGORY = new Category(0, "(no category)");
    private static final String[] FORMS = {"Tablet", "Capsule", "Syrup", "Suspension", "Injection", "Cream",
            "Ointment", "Gel", "Drops", "Inhaler", "Sachet", "Solution", "Suppository", "Other"};

    private final transient MedicineController controller = new MedicineController();
    private final JTextField barcode = Ui.field(20);
    private final JTextField brand = Ui.field(20);
    private final JTextField generic = Ui.field(20);
    private final JComboBox<Category> category = new JComboBox<>();
    private final JComboBox<String> dosageForm = new JComboBox<>(FORMS);
    private final JTextField strength = Ui.field(20);
    private final JTextField manufacturer = Ui.field(20);
    private final JTextField price = Ui.field(20);
    private final JTextField reorder = Ui.field(20);
    private final JCheckBox rx = new JCheckBox("Prescription only (Rx)");
    private final JCheckBox active = new JCheckBox("Active (can be sold)", true);

    public MedicinePanel() {
        super("Medicines", new String[]{"Barcode", "Medicine", "Generic", "Category", "Form", "Price", "In stock",
                "Rx", "Status"});
        dosageForm.setEditable(true);
        init();
        table.setRowStyler(new PosTable.RowStyler() {
            @Override
            public Color background(Object item) {
                Medicine m = (Medicine) item;
                return !m.isActive() ? new Color(0xEDEFEE) : (m.isLowStock() ? Theme.WARN_SOFT : null);
            }

            @Override
            public Color foreground(Object item) {
                return !((Medicine) item).isActive() ? Theme.MUTED : null;
            }
        });
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{110, 190, 140, 140, 80, 80, 70, 40, 70};
    }

    @Override
    protected Object[] toRow(Medicine m) {
        return new Object[]{m.getBarcode(), m.toString(), Strings.nz(m.getGenericName()),
                Strings.nz(m.getCategoryName()), Strings.nz(m.getDosageForm()), m.getSellingPrice(),
                m.getStockQty(), m.isRequiresPrescription() ? "Rx" : "", m.isActive() ? "Active" : "Inactive"};
    }

    @Override
    protected List<Medicine> fetch(String q) {
        return controller.search(q);
    }

    @Override
    public void onShow() {
        Ui.guard(this, this::loadCategories);
        super.onShow();
    }

    private void loadCategories() {
        Object keep = category.getSelectedItem();
        DefaultComboBoxModel<Category> m = new DefaultComboBoxModel<>();
        m.addElement(NO_CATEGORY);
        for (Category c : controller.categories()) {
            m.addElement(c);
        }
        category.setModel(m);
        if (keep != null) {
            for (int i = 0; i < m.getSize(); i++) {
                if (m.getElementAt(i).getId() == ((Category) keep).getId()) {
                    category.setSelectedIndex(i);
                }
            }
        }
    }

    @Override
    protected void buildForm(FormPanel f) {
        f.add("Barcode *", barcode);
        f.add("Brand / product name *", brand);
        f.add("Generic name", generic);
        JButton addCat = Ui.neutral("+");
        addCat.addActionListener(e -> {
            String n = JOptionPane.showInputDialog(this, "New category name:");
            if (n != null && Ui.guard(this, () -> controller.addCategory(n))) {
                Ui.guard(this, this::loadCategories);
            }
        });
        JPanel catRow = new JPanel(new BorderLayout(6, 0));
        catRow.setOpaque(false);
        catRow.add(category, BorderLayout.CENTER);
        catRow.add(addCat, BorderLayout.EAST);
        f.add("Category", catRow);
        f.add("Dosage form", dosageForm);
        f.add("Strength (e.g. 500mg)", strength);
        f.add("Manufacturer", manufacturer);
        f.add("Selling price (per unit) *", price);
        f.add("Reorder level (units)", reorder);
        f.addPlain(rx);
        f.addPlain(active);
    }

    @Override
    protected void fillForm(Medicine m) {
        barcode.setText(m.getBarcode());
        brand.setText(m.getBrandName());
        generic.setText(Strings.nz(m.getGenericName()));
        category.setSelectedItem(NO_CATEGORY);
        for (int i = 0; i < category.getItemCount(); i++) {
            if (m.getCategoryId() != null && category.getItemAt(i).getId() == m.getCategoryId()) {
                category.setSelectedIndex(i);
            }
        }
        dosageForm.setSelectedItem(Strings.nz(m.getDosageForm()));
        strength.setText(Strings.nz(m.getStrength()));
        manufacturer.setText(Strings.nz(m.getManufacturer()));
        price.setText(Money.plain(m.getSellingPrice()).replace(",", ""));
        reorder.setText(String.valueOf(m.getReorderLevel()));
        rx.setSelected(m.isRequiresPrescription());
        active.setSelected(m.isActive());
    }

    @Override
    protected void clearForm() {
        barcode.setText("");
        brand.setText("");
        generic.setText("");
        if (category.getItemCount() > 0) {
            category.setSelectedIndex(0);
        }
        dosageForm.setSelectedItem("Tablet");
        strength.setText("");
        manufacturer.setText("");
        price.setText("");
        reorder.setText("10");
        rx.setSelected(false);
        active.setSelected(true);
    }

    @Override
    protected Medicine readForm() {
        Medicine m = new Medicine();
        if (editing != null) {
            m.setId(editing.getId());
        }
        m.setBarcode(barcode.getText());
        m.setBrandName(brand.getText());
        m.setGenericName(generic.getText());
        Category c = (Category) category.getSelectedItem();
        m.setCategoryId(c == null || c.getId() == 0 ? null : c.getId());
        m.setDosageForm(String.valueOf(dosageForm.getSelectedItem()));
        m.setStrength(strength.getText());
        m.setManufacturer(manufacturer.getText());
        BigDecimal p = Money.parse("Selling price", price.getText());
        m.setSellingPrice(p);
        try {
            m.setReorderLevel(Integer.parseInt(reorder.getText().trim()));
        } catch (NumberFormatException e) {
            throw new PosException("Reorder level must be a whole number.");
        }
        m.setRequiresPrescription(rx.isSelected());
        m.setActive(active.isSelected());
        return m;
    }

    @Override
    protected void persist(Medicine m) {
        controller.save(m);
    }
}
