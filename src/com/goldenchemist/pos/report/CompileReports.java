package com.goldenchemist.pos.report;

import java.io.File;

/** Build helper: pre-compiles every reports/*.jrxml so the app starts fast. */
public final class CompileReports {

    private CompileReports() {
    }

    public static void main(String[] args) {
        File[] files = ReportService.reportsDir().listFiles((d, n) -> n.endsWith(".jrxml"));
        if (files == null) {
            System.err.println("No reports folder found: " + ReportService.reportsDir().getAbsolutePath());
            System.exit(1);
            return;
        }
        ReportService svc = new ReportService();
        for (File f : files) {
            String name = f.getName().replace(".jrxml", "");
            svc.load(name);
            System.out.println("Compiled " + name);
        }
    }
}
