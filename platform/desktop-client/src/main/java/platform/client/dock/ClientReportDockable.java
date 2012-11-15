package platform.client.dock;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.base.OSUtils;
import platform.client.Main;
import platform.interop.form.ReportGenerationData;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class ClientReportDockable extends ClientDockable {
    public ClientReportDockable(String reportSID, ReportGenerationData generationData, DockableManager dockableManager) throws ClassNotFoundException, IOException {
        super(reportSID, dockableManager);

        try {
            JasperPrint print = new ReportGenerator(generationData, Main.timeZone).createReport(false, null);
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

            setContent(print.getName(), prepareViewer(new RViewer(print)));
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

    private static class RViewer extends JRViewer {
        public RViewer(JasperPrint print) {
            super(print);

            lastFolder = OSUtils.loadCurrentDirectory();

            ActionListener[] al = btnSave.getActionListeners();

            btnSave.removeActionListener(al[0]);

            btnSave.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    OSUtils.saveCurrentDirectory(lastFolder);
                }
            });
            btnSave.addActionListener(al[0]);
        }
    }
}
