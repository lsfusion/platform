package lsfusion.client.form.print.view;

import com.google.common.base.Throwables;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.MainController;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.XlsReportConfiguration;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ReportDialog extends JDialog {
    public static Integer pageCount;
    public ReportDialog(JFrame owner, String formCaption, ReportGenerationData generationData, String printerName, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException, JRException {
        super(owner, true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        ReportGenerator report = new ReportGenerator(generationData);
        JasperPrint print = report.createReport(FormPrintType.PRINT, MainController.remoteLogics);
        print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");
        pageCount = print.getPages().size();

        final ReportViewer viewer = new ReportViewer(print, printerName, editInvoker);
        double realZoom = viewer.getRealZoom();

        setTitle(formCaption);
        setSize(SwingUtils.clipToScreen(new Dimension((int)(print.getPageWidth() * realZoom + 100),
                                                      (int)(print.getPageHeight() * realZoom + 150))));
        setLocationRelativeTo(owner);

        getContentPane().add(viewer);
    }

    public static Integer showReportDialog(ReportGenerationData generationData, String formCaption, String printerName, EditReportInvoker editInvoker) throws ClassNotFoundException, IOException {
        try {
            ReportDialog dlg = new ReportDialog(MainFrame.instance, formCaption, generationData, printerName, editInvoker);
            dlg.setVisible(true);
            return pageCount;
        } catch (JRException e) {
            throw Throwables.propagate(e);
        }
    }
}
