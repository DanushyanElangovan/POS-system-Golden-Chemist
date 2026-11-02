package com.goldenchemist.pos.view;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

/** Shows a filled Jasper report with the standard viewer (print, save, zoom). */
public class ReportViewerDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public ReportViewerDialog(Window owner, String title, JasperPrint print) {
        super(owner, title, ModalityType.MODELESS);
        setLayout(new BorderLayout());
        add(new JRViewer(print), BorderLayout.CENTER);
        setPreferredSize(new Dimension(920, 720));
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public static void show(Component parent, String title, JasperPrint print) {
        Window w = javax.swing.SwingUtilities.getWindowAncestor(parent);
        new ReportViewerDialog(w, title, print).setVisible(true);
    }
}
