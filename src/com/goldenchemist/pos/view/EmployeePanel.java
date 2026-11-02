package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.EmployeeController;
import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Role;
import com.goldenchemist.pos.util.Strings;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.time.LocalDate;
import java.util.List;

public class EmployeePanel extends AbstractCrudPanel<Employee> {
    private static final long serialVersionUID = 1L;

    private final transient EmployeeController controller = new EmployeeController();
    private final JTextField fullName = Ui.field(20);
    private final JTextField username = Ui.field(20);
    private final JComboBox<Role> role = new JComboBox<>(Role.values());
    private final JTextField phone = Ui.field(20);
    private final JTextField email = Ui.field(20);
    private final JTextField nic = Ui.field(20);
    private final DateField hired = new DateField();
    private final JPasswordField password = new JPasswordField(20);
    private final JCheckBox active = new JCheckBox("Account active (can sign in)", true);

    public EmployeePanel() {
        super("Employees", new String[]{"Name", "Username", "Role", "Phone", "Hired", "Status"});
        init();
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{200, 120, 110, 110, 90, 70};
    }

    @Override
    protected Object[] toRow(Employee e) {
        return new Object[]{e.getFullName(), e.getUsername(), e.getRole().getLabel(), Strings.nz(e.getPhone()),
                e.getHiredDate() == null ? "" : e.getHiredDate().toString(), e.isActive() ? "Active" : "Disabled"};
    }

    @Override
    protected List<Employee> fetch(String q) {
        return controller.search(q);
    }

    @Override
    protected void buildForm(FormPanel f) {
        f.add("Full name *", fullName);
        f.add("Username *", username);
        f.add("Role *", role);
        f.add("Phone", phone);
        f.add("E-mail", email);
        f.add("NIC", nic);
        f.add("Hired date", hired);
        f.add("Password (required for new, blank = keep)", password);
        f.addPlain(active);
    }

    @Override
    protected void fillForm(Employee e) {
        fullName.setText(e.getFullName());
        username.setText(e.getUsername());
        role.setSelectedItem(e.getRole());
        phone.setText(Strings.nz(e.getPhone()));
        email.setText(Strings.nz(e.getEmail()));
        nic.setText(Strings.nz(e.getNic()));
        hired.setDate(e.getHiredDate());
        password.setText("");
        active.setSelected(e.isActive());
    }

    @Override
    protected void clearForm() {
        fullName.setText("");
        username.setText("");
        role.setSelectedItem(Role.CASHIER);
        phone.setText("");
        email.setText("");
        nic.setText("");
        hired.setDate(LocalDate.now());
        password.setText("");
        active.setSelected(true);
    }

    @Override
    protected Employee readForm() {
        Employee e = new Employee();
        if (editing != null) {
            e.setId(editing.getId());
        }
        e.setFullName(fullName.getText());
        e.setUsername(username.getText());
        e.setRole((Role) role.getSelectedItem());
        e.setPhone(phone.getText());
        e.setEmail(email.getText());
        e.setNic(nic.getText());
        e.setHiredDate(hired.getDate());
        e.setActive(active.isSelected());
        return e;
    }

    @Override
    protected void persist(Employee e) {
        String pw = new String(password.getPassword());
        controller.save(e, pw);
        password.setText("");
    }
}
