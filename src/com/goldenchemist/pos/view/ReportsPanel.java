package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.ReportController;
import com.goldenchemist.pos.controller.ReportController.Type;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Pick a Jasper report, set its parameters, preview it or export to PDF / Excel. */
public class ReportsPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final transient ReportController controller = new ReportController();
    private final JList<Type> list = new JList<>();
    private final JLabel description = Ui.muted(" ");
    private final DateField from = new DateField(LocalDate.now().withDayOfMonth(1));
    private final DateField to = new DateField();
    private final JSpinner days = new JSpinner(new SpinnerNumberModel(90, 1, 3650, 10));
    private final JLabel fromLabel = Ui.bold("From");
    private final JLabel toLabel = Ui.bold("To");
    private final JLabel daysLabel = Ui.bold("Within days");

    public ReportsPanel() {
        setLayout(new BorderLayout());
        JPanel page = Ui.page();
        add(page, BorderLayout.CENTER);
        page.add(Ui.title("Reports"), BorderLayout.NORTH);

        DefaultListModel<Type> lm = new DefaultListModel<>();
        for (Type t : Type.values()) {
            lm.addElement(t);
        }
        list.setModel(lm);
        list.setFixedCellHeight(34);
        list.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        list.setSelectedIndex(0);
        list.addListSelectionListener(e -> updateParams());
        JPanel left = Ui.card();
        left.setLayout(new BorderLayout());
        left.setPreferredSize(new Dimension(280, 100));
        left.add(list, BorderLayout.CENTER);
        page.add(left, BorderLayout.WEST);

        JPanel right = Ui.card();
        right.setLayout(new BorderLayout(0, 14));
        right.add(description, BorderLayout.NORTH);
        JPanel dates = Ui.row(fromLabel, from, toLabel, to, daysLabel, days);
        dates.setOpaque(false);
        JButton preview = Ui.primary("Preview");
        JButton pdf = Ui.neutral("Export PDF");
        JButton xls = Ui.neutral("Export Excel");
        preview.addActionListener(e -> preview());
        pdf.addActionListener(e -> export(true));
        xls.addActionListener(e -> export(false));
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(dates, BorderLayout.NORTH);
        center.add(Ui.buttonRow(preview, pdf, xls), BorderLayout.CENTER);
        right.add(center, BorderLayout.CENTER);
        JPanel rw = new JPanel(new BorderLayout());
        rw.setOpaque(false);
        rw.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(right, BorderLayout.NORTH);
        rw.add(top, BorderLayout.CENTER);
        page.add(rw, BorderLayout.CENTER);
        updateParams();
    }

    private void updateParams() {
        Type t = list.getSelectedValue();
        if (t == null) {
            return;
        }
        description.setText("<html><b>" + t.label + "</b><br>" + t.description + "</html>");
        from.setVisible(t.usesDateRange);
        to.setVisible(t.usesDateRange);
        fromLabel.setVisible(t.usesDateRange);
        toLabel.setVisible(t.usesDateRange);
        days.setVisible(t.usesDays);
        daysLabel.setVisible(t.usesDays);
    }

    private net.sf.jasperreports.engine.JasperPrint build() {
        Type t = list.getSelectedValue();
        return controller.run(t, from.getDate(), to.getDate(), (Integer) days.getValue());
    }

    private void preview() {
        Type t = list.getSelectedValue();
        Ui.guard(this, () -> ReportViewerDialog.show(this, t.label, build()));
    }

    private void export(boolean pdf) {
        Type t = list.getSelectedValue();
        JFileChooser fc = new JFileChooser();
        String ext = pdf ? "pdf" : "xls";
        fc.setFileFilter(new FileNameExtensionFilter(pdf ? "PDF document (*.pdf)" : "Excel 97-2003 (*.xls)", ext));
        fc.setSelectedFile(new File(t.template + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "." + ext));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ext)) {
            f = new File(f.getPath() + "." + ext);
        }
        if (f.exists() && !Ui.confirm(this, f.getName() + " already exists. Replace it?")) {
            return;
        }
        final File target = f;
        if (Ui.guard(this, () -> {
            if (pdf) {
                controller.exportPdf(build(), target);
            } else {
                controller.exportExcel(build(), target);
            }
        })) {
            Ui.info(this, "Saved " + target.getAbsolutePath());
        }
    }

    @Override
    public void onShow() {
        to.setDate(LocalDate.now());
    }
}
