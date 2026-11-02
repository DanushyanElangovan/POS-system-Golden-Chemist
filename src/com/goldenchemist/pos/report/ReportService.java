package com.goldenchemist.pos.report;

import com.goldenchemist.pos.config.AppConfig;
import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.util.PosException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/** Loads Jasper templates (compiling them on first use), fills them from MySQL and exports. */
public class ReportService {

    public static File reportsDir() {
        return new File(AppConfig.get("reports.dir", "reports"));
    }

    /** Compiles name.jrxml to reports/compiled/name.jasper when missing or older than the template. */
    public JasperReport load(String name) {
        File jrxml = new File(reportsDir(), name + ".jrxml");
        if (!jrxml.isFile()) {
            throw new PosException("Report template not found: " + jrxml.getPath());
        }
        File compiledDir = new File(reportsDir(), "compiled");
        File jasper = new File(compiledDir, name + ".jasper");
        try {
            if (jasper.isFile() && jasper.lastModified() >= jrxml.lastModified()) {
                return (JasperReport) JRLoader.loadObject(jasper);
            }
            JasperReport report = JasperCompileManager.compileReport(jrxml.getPath());
            if (compiledDir.isDirectory() || compiledDir.mkdirs()) {
                try {
                    JRSaver.saveObject(report, jasper);
                } catch (JRException ignored) {
                    // read-only folder: keep the in-memory copy
                }
            }
            return report;
        } catch (JRException e) {
            throw new PosException("Could not load report '" + name + "': " + e.getMessage(), e);
        }
    }

    public JasperPrint fill(String name, Map<String, Object> params) {
        JasperReport report = load(name);
        try (Connection c = Database.get()) {
            return JasperFillManager.fillReport(report, params, c);
        } catch (SQLException e) {
            throw Database.wrap("run the report", e);
        } catch (JRException e) {
            throw new PosException("Could not generate the report: " + e.getMessage(), e);
        }
    }

    public void exportPdf(JasperPrint print, File target) {
        try {
            JasperExportManager.exportReportToPdfFile(print, target.getPath());
        } catch (JRException e) {
            throw new PosException("Could not save the PDF: " + e.getMessage(), e);
        }
    }

    public void exportXls(JasperPrint print, File target) {
        try {
            JRXlsExporter exporter = new JRXlsExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(target));
            SimpleXlsReportConfiguration cfg = new SimpleXlsReportConfiguration();
            cfg.setOnePagePerSheet(false);
            cfg.setDetectCellType(true);
            cfg.setWhitePageBackground(false);
            cfg.setRemoveEmptySpaceBetweenRows(true);
            exporter.setConfiguration(cfg);
            exporter.exportReport();
        } catch (JRException e) {
            throw new PosException("Could not save the Excel file: " + e.getMessage(), e);
        }
    }
}
