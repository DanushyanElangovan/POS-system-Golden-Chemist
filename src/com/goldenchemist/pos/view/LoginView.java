package com.goldenchemist.pos.view;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.controller.AuthController;
import com.goldenchemist.pos.model.Employee;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

public class LoginView extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient AuthController auth = new AuthController();
    private final JTextField username = Ui.field(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JLabel message = new JLabel(" ");

    public LoginView(boolean firstRun) {
        super(AppConfig.shopName() + " - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel brand = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.GOLD);
                g2.fillOval(getWidth() / 2 - 44, 90, 88, 88);
                g2.setColor(Theme.GREEN_DARK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 40));
                g2.drawString("GC", getWidth() / 2 - 26, 150);
            }
        };
        brand.setBackground(Theme.GREEN);
        brand.setPreferredSize(new Dimension(300, 440));
        brand.setLayout(new BorderLayout());
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(BorderFactory.createEmptyBorder(210, 20, 0, 20));
        JLabel name = new JLabel("<html><div style='text-align:center'>" + AppConfig.shopName() + "</div></html>",
                SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 22));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(0.5f);
        JLabel sub = new JLabel("Point of Sale", SwingConstants.CENTER);
        sub.setForeground(Theme.GOLD);
        sub.setFont(Theme.BOLD);
        sub.setAlignmentX(0.5f);
        text.add(name);
        text.add(sub);
        brand.add(text, BorderLayout.NORTH);
        add(brand, BorderLayout.WEST);

        JPanel formWrap = new JPanel(new GridBagLayout());
        formWrap.setBackground(Theme.WHITE);
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JLabel hello = Ui.title("Welcome back");
        hello.setAlignmentX(0);
        form.add(hello);
        form.add(javax.swing.Box.createVerticalStrut(18));
        form.add(label("Username"));
        form.add(username);
        form.add(javax.swing.Box.createVerticalStrut(10));
        form.add(label("Password"));
        password.setPreferredSize(new Dimension(220, 28));
        form.add(password);
        form.add(javax.swing.Box.createVerticalStrut(8));
        message.setForeground(Theme.DANGER);
        message.setAlignmentX(0);
        form.add(message);
        form.add(javax.swing.Box.createVerticalStrut(8));
        JButton login = Ui.primary("Sign in");
        login.setAlignmentX(0);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        login.addActionListener(e -> doLogin());
        form.add(login);
        if (firstRun) {
            form.add(javax.swing.Box.createVerticalStrut(14));
            JLabel hint = new JLabel("<html><div style='width:230px'>First start: sign in with <b>admin</b> / "
                    + "<b>admin123</b>, then change the password and add your staff under Employees.</div></html>");
            hint.setForeground(Theme.WARN);
            hint.setAlignmentX(0);
            form.add(hint);
        }
        formWrap.add(form);
        add(formWrap, BorderLayout.CENTER);

        password.addActionListener(e -> doLogin());
        username.addActionListener(e -> password.requestFocusInWindow());
        getRootPane().setDefaultButton(login);
        pack();
        setSize(740, 440);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JLabel label(String t) {
        JLabel l = Ui.bold(t);
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(0);
        return l;
    }

    private void doLogin() {
        message.setText(" ");
        try {
            Employee e = auth.login(username.getText(), new String(password.getPassword()));
            dispose();
            new MainFrame(e).setVisible(true);
        } catch (RuntimeException ex) {
            message.setText("<html>" + ex.getMessage() + "</html>");
            password.setText("");
            password.requestFocusInWindow();
        }
    }
}
