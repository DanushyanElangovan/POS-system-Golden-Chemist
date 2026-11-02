package com.goldenchemist.pos.view;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.controller.AuthController;
import com.goldenchemist.pos.controller.Session;
import com.goldenchemist.pos.model.Employee;
import com.goldenchemist.pos.model.Module;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.EnumMap;
import java.util.Map;

/** Main window: green sidebar with the screens this role may open, content area on the right. */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Module, JPanel> created = new EnumMap<>(Module.class);
    private final Map<Module, NavButton> navButtons = new EnumMap<>(Module.class);

    private static class NavButton extends JButton {
        private static final long serialVersionUID = 1L;
        private boolean selected;

        NavButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setPreferredSize(new Dimension(220, 44));
        }

        void setSelectedNav(boolean s) {
            this.selected = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                g.setColor(Theme.GREEN_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.GOLD);
                g.fillRect(0, 0, 5, getHeight());
            }
            super.paintComponent(g);
        }
    }

    public MainFrame(Employee user) {
        super(AppConfig.shopName() + " - POS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildSidebar(user), BorderLayout.WEST);
        content.setBackground(Theme.BG);
        add(content, BorderLayout.CENTER);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1320, 800);
        setLocationRelativeTo(null);
        show(Module.DASHBOARD);
    }

    private JPanel buildSidebar(Employee user) {
        JPanel side = new JPanel();
        side.setBackground(Theme.GREEN);
        side.setPreferredSize(new Dimension(220, 100));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("<html><div style='padding:4px 0'>" + AppConfig.shopName()
                + "<br><span style='color:#C9A227;font-size:10px'>POINT OF SALE</span></div></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 10));
        brand.setAlignmentX(0);
        side.add(brand);

        for (Module m : Module.values()) {
            if (!Session.can(m)) {
                continue;
            }
            NavButton b = new NavButton(m.getLabel());
            b.addActionListener(e -> show(m));
            navButtons.put(m, b);
            side.add(b);
        }
        side.add(javax.swing.Box.createVerticalGlue());

        JLabel who = new JLabel("<html><b>" + user.getFullName() + "</b><br><span style='color:#C9A227'>"
                + user.getRole().getLabel() + "</span></html>");
        who.setForeground(Color.WHITE);
        who.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 10));
        side.add(who);
        JButton pw = linkButton("Change password");
        pw.addActionListener(e -> ChangePasswordDialog.show(this));
        JButton out = linkButton("Sign out");
        out.addActionListener(e -> signOut());
        side.add(pw);
        side.add(out);
        side.add(javax.swing.Box.createVerticalStrut(14));
        return side;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(new Color(0xCFE3D8));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(4, 22, 4, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return b;
    }

    private void signOut() {
        new AuthController().logout();
        dispose();
        new LoginView(false).setVisible(true);
    }

    private JPanel create(Module m) {
        switch (m) {
            case DASHBOARD: return new DashboardPanel();
            case BILLING: return new BillingPanel();
            case INVOICES: return new InvoicesPanel();
            case MEDICINES: return new MedicinePanel();
            case INVENTORY: return new InventoryPanel();
            case SUPPLIERS: return new SupplierPanel();
            case CUSTOMERS: return new CustomerPanel();
            case EMPLOYEES: return new EmployeePanel();
            default: return new ReportsPanel();
        }
    }

    public void show(Module m) {
        JPanel p = created.get(m);
        if (p == null) {
            p = create(m);
            created.put(m, p);
            content.add(p, m.name());
        }
        cards.show(content, m.name());
        for (Map.Entry<Module, NavButton> e : navButtons.entrySet()) {
            e.getValue().setSelectedNav(e.getKey() == m);
        }
        if (p instanceof Refreshable) {
            ((Refreshable) p).onShow();
        }
    }
}
