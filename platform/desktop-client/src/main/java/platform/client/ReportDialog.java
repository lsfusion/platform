package platform.client;

import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.view.JRViewer;
import platform.interop.form.ReportGenerationData;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ReportDialog extends JDialog {
    public ReportDialog(JFrame owner, ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        super(owner, true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        ReportGenerator report = new ReportGenerator(generationData, Main.timeZone);
        JasperPrint print = report.createReport(false, null);
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

        final DialogReportViewer viewer = new DialogReportViewer(print);
        double realZoom = viewer.getRealZoom();

        setTitle(print.getName());
        setSize(SwingUtils.clipToScreen(new Dimension((int)(print.getPageWidth() * realZoom + 100),
                                                      (int)(print.getPageHeight() * realZoom + 150))));
        setLocationRelativeTo(owner);

        getContentPane().add(viewer);
    }

    private static class DialogReportViewer extends JRViewer {
        public DialogReportViewer(JasperPrint print) {
            super(print);
        }

        public double getRealZoom() {
            return realZoom;
        }
    }

    public static void showReportDialog(ReportGenerationData generationData) throws ClassNotFoundException, IOException {
        try {
            ReportDialog dlg = new ReportDialog(Main.frame, generationData);
            dlg.setVisible(true);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }
}
