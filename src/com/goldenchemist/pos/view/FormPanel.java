package com.goldenchemist.pos.view;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Simple label + field form (labels above fields). */
public class FormPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private int row;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public <C extends JComponent> C add(String label, C field) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row++;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(row == 1 ? 0 : 8, 0, 2, 0);
        JLabel l = new JLabel(label);
        l.setFont(Theme.BOLD);
        l.setForeground(Theme.MUTED);
        super.add(l, g);

        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 0;
        f.gridy = row++;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(0, 0, 0, 0);
        super.add(field, f);
        return field;
    }

    /** Adds a component without a label (e.g. a checkbox). */
    public <C extends JComponent> C addPlain(C comp) {
        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 0;
        f.gridy = row++;
        f.weightx = 1;
        f.anchor = GridBagConstraints.WEST;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(8, 0, 0, 0);
        super.add(comp, f);
        return comp;
    }
}
