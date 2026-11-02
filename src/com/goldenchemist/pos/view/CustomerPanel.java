package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.CustomerController;
import com.goldenchemist.pos.model.Customer;
import com.goldenchemist.pos.util.Strings;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.util.List;

public class CustomerPanel extends AbstractCrudPanel<Customer> {
    private static final long serialVersionUID = 1L;

    private final transient CustomerController controller = new CustomerController();
    private final JTextField name = Ui.field(20);
    private final JTextField phone = Ui.field(20);
    private final JTextField email = Ui.field(20);
    private final JTextField address = Ui.field(20);

    public CustomerPanel() {
        super("Customers", new String[]{"Name", "Phone", "E-mail", "Address"});
        init();
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{220, 120, 200, 260};
    }

    @Override
    protected JButton[] extraButtons() {
        JButton delete = Ui.danger("Delete");
        delete.addActionListener(e -> {
            Customer c = selected();
            if (c == null) {
                Ui.error(this, "Select a customer in the table first.");
                return;
            }
            if (Ui.confirm(this, "Delete " + c.getFullName() + "? Their past invoices are kept.")
                    && Ui.guard(this, () -> controller.delete(c))) {
                startNew();
                reload();
            }
        });
        return new JButton[]{delete};
    }

    @Override
    protected Object[] toRow(Customer c) {
        return new Object[]{c.getFullName(), Strings.nz(c.getPhone()), Strings.nz(c.getEmail()),
                Strings.nz(c.getAddress())};
    }

    @Override
    protected List<Customer> fetch(String q) {
        return controller.search(q);
    }

    @Override
    protected void buildForm(FormPanel f) {
        f.add("Full name *", name);
        f.add("Phone", phone);
        f.add("E-mail", email);
        f.add("Address", address);
    }

    @Override
    protected void fillForm(Customer c) {
        name.setText(c.getFullName());
        phone.setText(Strings.nz(c.getPhone()));
        email.setText(Strings.nz(c.getEmail()));
        address.setText(Strings.nz(c.getAddress()));
    }

    @Override
    protected void clearForm() {
        name.setText("");
        phone.setText("");
        email.setText("");
        address.setText("");
    }

    @Override
    protected Customer readForm() {
        Customer c = new Customer();
        if (editing != null) {
            c.setId(editing.getId());
        }
        c.setFullName(name.getText());
        c.setPhone(phone.getText());
        c.setEmail(email.getText());
        c.setAddress(address.getText());
        return c;
    }

    @Override
    protected void persist(Customer c) {
        controller.save(c);
    }
}
