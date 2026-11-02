package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.SupplierController;
import com.goldenchemist.pos.model.Supplier;
import com.goldenchemist.pos.util.Strings;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.util.List;

public class SupplierPanel extends AbstractCrudPanel<Supplier> {
    private static final long serialVersionUID = 1L;

    private final transient SupplierController controller = new SupplierController();
    private final JTextField name = Ui.field(20);
    private final JTextField contact = Ui.field(20);
    private final JTextField phone = Ui.field(20);
    private final JTextField email = Ui.field(20);
    private final JTextField address = Ui.field(20);
    private final JCheckBox active = new JCheckBox("Active supplier", true);

    public SupplierPanel() {
        super("Suppliers", new String[]{"Name", "Contact person", "Phone", "E-mail", "Status"});
        init();
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{220, 160, 110, 200, 70};
    }

    @Override
    protected Object[] toRow(Supplier s) {
        return new Object[]{s.getName(), Strings.nz(s.getContactPerson()), Strings.nz(s.getPhone()),
                Strings.nz(s.getEmail()), s.isActive() ? "Active" : "Inactive"};
    }

    @Override
    protected List<Supplier> fetch(String q) {
        return controller.search(q);
    }

    @Override
    protected void buildForm(FormPanel f) {
        f.add("Supplier name *", name);
        f.add("Contact person", contact);
        f.add("Phone", phone);
        f.add("E-mail", email);
        f.add("Address", address);
        f.addPlain(active);
    }

    @Override
    protected void fillForm(Supplier s) {
        name.setText(s.getName());
        contact.setText(Strings.nz(s.getContactPerson()));
        phone.setText(Strings.nz(s.getPhone()));
        email.setText(Strings.nz(s.getEmail()));
        address.setText(Strings.nz(s.getAddress()));
        active.setSelected(s.isActive());
    }

    @Override
    protected void clearForm() {
        name.setText("");
        contact.setText("");
        phone.setText("");
        email.setText("");
        address.setText("");
        active.setSelected(true);
    }

    @Override
    protected Supplier readForm() {
        Supplier s = new Supplier();
        if (editing != null) {
            s.setId(editing.getId());
        }
        s.setName(name.getText());
        s.setContactPerson(contact.getText());
        s.setPhone(phone.getText());
        s.setEmail(email.getText());
        s.setAddress(address.getText());
        s.setActive(active.isSelected());
        return s;
    }

    @Override
    protected void persist(Supplier s) {
        controller.save(s);
    }
}
