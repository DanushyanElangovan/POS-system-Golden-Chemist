package com.goldenchemist.pos.view;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;

/** JTable with zebra stripes and optional per-row colours driven by the row's domain object. */
public class PosTable extends JTable {
    private static final long serialVersionUID = 1L;

    /** Decides row colours from the domain object (return null for the default). */
    public interface RowStyler {
        Color background(Object item);

        Color foreground(Object item);
    }

    private transient RowStyler styler;

    public PosTable(TableModel model) {
        super(model);
    }

    public void setRowStyler(RowStyler styler) {
        this.styler = styler;
    }

    /** The domain object shown in a view row (null if the model is not an ObjectTableModel). */
    public Object itemAt(int viewRow) {
        if (viewRow < 0 || !(getModel() instanceof ObjectTableModel)) {
            return null;
        }
        return ((ObjectTableModel<?>) getModel()).getItem(convertRowIndexToModel(viewRow));
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (c instanceof JComponent) {
            ((JComponent) c).setOpaque(true);
        }
        if (isRowSelected(row)) {
            c.setBackground(Theme.GREEN);
            c.setForeground(Theme.WHITE);
            return c;
        }
        Color bg = row % 2 == 0 ? Theme.WHITE : Theme.ZEBRA;
        Color fg = Theme.TEXT;
        if (styler != null) {
            Object item = itemAt(row);
            if (item != null) {
                Color b = styler.background(item);
                Color f = styler.foreground(item);
                bg = b != null ? b : bg;
                fg = f != null ? f : fg;
            }
        }
        c.setBackground(bg);
        c.setForeground(fg);
        return c;
    }
}
