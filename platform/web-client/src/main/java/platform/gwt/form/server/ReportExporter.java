package platform.gwt.form.server;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JasperExportManager;
import platform.base.BaseUtils;
import platform.interop.form.ReportGenerationData;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.TimeZone;

public class ReportExporter {
    public static String exportReport(HttpSession session, boolean toExcel, ReportGenerationData reportData) {
        try {
            TimeZone zone = Calendar.getInstance().getTimeZone();
            ReportGenerator generator = new ReportGenerator(reportData, zone);
            byte[] report = !toExcel ? JasperExportManager.exportReportToPdf(generator.createReport(false, null)) : ReportGenerator.exportToExcelByteArray(reportData, zone);
            File file = File.createTempFile("lsfReport", "");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(report);

            String reportSID = generateReportSID(session);
            session.setAttribute(reportSID, file.getAbsolutePath());
            return reportSID;
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }

    private static String generateReportSID(HttpSession session) {
        String sid;
        do {
            sid = BaseUtils.randomString(20);
        } while (session.getAttribute(sid) != null);
        return sid;
    }
}
