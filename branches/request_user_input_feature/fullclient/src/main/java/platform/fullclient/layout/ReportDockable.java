package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.base.OSUtils;
import platform.client.Main;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportGenerationData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class ReportDockable extends FormDockable {
    private String reportCaption;
    private final ReportGenerationData generationData;

    public ReportDockable(String formSID, ReportGenerationData generationData, MultipleCDockableFactory<FormDockable,?> factory) throws ClassNotFoundException, IOException {
        super(formSID, factory);
        this.generationData = generationData;
        setActiveComponent(getActiveComponent(null, null, null), getCaption());
    }

    // из файла
    ReportDockable(File file, MultipleCDockableFactory<FormDockable,?> factory) throws JRException {
        super("", factory);
        this.generationData = null;
        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(file))), file.getName());
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException {
        try {
            ReportGenerator report = new ReportGenerator(generationData, Main.timeZone);
            JasperPrint print = report.createReport(false, null);
            reportCaption = print.getName();
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");
            return prepareViewer(new RViewer(print));
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getCaption() {
        return reportCaption;
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
