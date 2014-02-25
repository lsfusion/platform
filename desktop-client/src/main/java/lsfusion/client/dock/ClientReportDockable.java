package lsfusion.client.dock;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.ReportViewer;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ClientReportDockable extends ClientDockable {
    public ClientReportDockable(String reportSID, ReportGenerationData generationData, DockableManager dockableManager, EditReportInvoker editInvoker) throws ClassNotFoundException, IOException {
        super(reportSID, dockableManager);

        try {
            JasperPrint print = new ReportGenerator(generationData).createReport(false, null);
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

            setContent(print.getName(), prepareViewer(new ReportViewer(print, editInvoker)));
        } catch (JRException e) {
            Throwables.propagate(e);
        }
    }

    // из файла
    public ClientReportDockable(File file, DockableManager dockableManager) throws JRException {
        super("", dockableManager);
        setContent(file.getName(), prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(file))));
    }

    private JRViewer prepareViewer(final JRViewer viewer) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                viewer.setFitPageZoomRatio();
            }
        });
        return viewer;
    }
}
