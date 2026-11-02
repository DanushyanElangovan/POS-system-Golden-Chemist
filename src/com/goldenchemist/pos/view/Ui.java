package com.goldenchemist.pos.view;

import com.goldenchemist.pos.util.Money;
import com.goldenchemist.pos.util.PosException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

/** Factory methods so every screen looks the same. */
public final class Ui {

    private Ui() {
    }

    // ---- buttons -----------------------------------------------------------------------------

    /** Flat rounded button that ignores the look-and-feel's own painting. */
    public static class FlatButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color base;
        private boolean hover;

        FlatButton(String text, Color base, Color fg) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(fg);
            setFont(Theme.BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(0xC9D1CC) : (hover ? base.darker() : base);
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton primary(String text) {
        return new FlatButton(text, Theme.GREEN, Color.WHITE);
    }

    public static JButton accent(String text) {
        return new FlatButton(text, Theme.GOLD, Theme.TEXT);
    }

    public static JButton danger(String text) {
        return new FlatButton(text, Theme.DANGER, Color.WHITE);
    }

    public static JButton neutral(String text) {
        return new FlatButton(text, new Color(0xE1E8E4), Theme.TEXT);
    }

    // ---- labels, fields, panels --------------------------------------------------------------

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.TITLE);
        l.setForeground(Theme.GREEN);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.MUTED);
        return l;
    }

    public static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.BOLD);
        return l;
    }

    public static JLabel right(String text) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(Theme.BOLD);
        return l;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 28));
        return f;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14));
    }

    /** White panel with a thin border. */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Theme.WHITE);
        p.setBorder(cardBorder());
        return p;
    }

    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        return p;
    }

    public static JPanel row(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component c : parts) {
            p.add(c);
        }
        return p;
    }

    public static JPanel buttonRow(Component... parts) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                p.add(Box.createHorizontalStrut(8));
            }
            p.add(parts[i]);
        }
        return p;
    }

    // ---- tables ------------------------------------------------------------------------------

    public static PosTable table(TableModel model) {
        PosTable t = new PosTable(model);
        t.setRowHeight(28);
        t.setFont(Theme.BASE);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xE8EEEA));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 30));
        h.setDefaultRenderer(new HeaderRenderer());

        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof BigDecimal ? Money.plain((BigDecimal) value) : String.valueOf(value));
            }
        };
        money.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(BigDecimal.class, money);
        DefaultTableCellRenderer num = new DefaultTableCellRenderer();
        num.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(Integer.class, num);
        t.setDefaultRenderer(Long.class, num);
        DefaultTableCellRenderer text = new DefaultTableCellRenderer();
        text.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(Object.class, text);
        t.setDefaultRenderer(String.class, text);
        return t;
    }

    /** Solid green header cell (Nimbus would otherwise paint its own pale gradient over our colours). */
    private static class HeaderRenderer extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        HeaderRenderer() {
            setOpaque(true);
            setBackground(Theme.GREEN);
            setForeground(Color.WHITE);
            setFont(Theme.BOLD);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus,
                                                       int row, int col) {
            setText(value == null ? "" : value.toString());
            setHorizontalAlignment(numericClass(table.getColumnClass(col)) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    private static boolean numericClass(Class<?> c) {
        return c == BigDecimal.class || c == Integer.class || c == Long.class;
    }

    public static void widths(JTable t, int... w) {
        TableColumnModel m = t.getColumnModel();
        for (int i = 0; i < w.length && i < m.getColumnCount(); i++) {
            m.getColumn(i).setPreferredWidth(w[i]);
        }
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        s.getViewport().setBackground(Theme.WHITE);
        return s;
    }

    // ---- dialogs and error handling ----------------------------------------------------------

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Golden Chemist POS", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Golden Chemist POS", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Runs an action; business errors become a friendly dialog instead of a stack trace. */
    public static boolean guard(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (PosException e) {
            error(parent, e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            error(parent, "Something went wrong: " + e);
        }
        return false;
    }

    /** Calls the runnable (debounced) whenever the text field changes. */
    public static void onTextChange(JTextField field, Runnable r) {
        final javax.swing.Timer timer = new javax.swing.Timer(280, e -> r.run());
        timer.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timer.restart();
            }
        });
    }

    /** Calls the runnable immediately whenever the text field changes (live totals). */
    public static void onTextChangeNow(JTextField field, Runnable r) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }
}
