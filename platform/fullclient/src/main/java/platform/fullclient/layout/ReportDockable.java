package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ReportDockable extends FormDockable {

    private String reportCaption;

    public ReportDockable(String formSID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        super(formSID, navigator, currentSession, factory);
    }

    public ReportDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable,?> factory) throws ClassNotFoundException, IOException {
        super(navigator, remoteForm, factory);
    }

    // из файла
    ReportDockable(String fileName,String directory, MultipleCDockableFactory<FormDockable,?> factory) throws JRException {
        super("", factory);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(directory+fileName))),fileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        try {
            ReportGenerator report = new ReportGenerator(remoteForm);
            JasperPrint print = report.createReport(false, false, null);
            reportCaption = print.getName();
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");
            return prepareViewer(new JRViewer(print));
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
}
