package platform.client.layout;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.client.navigator.ClientNavigator;
import platform.client.form.ClientReportData;
import platform.client.ClientObjectProxy;
import platform.interop.form.RemoteFormInterface;
import platform.interop.CompressingInputStream;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ReportDockable extends FormDockable {

    public ReportDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        super(iformID, navigator, currentSession);
    }

    public ReportDockable(int iformID, ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException, JRException {
        super(iformID, navigator, remoteForm);
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(0);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(Directory+FileName))),FileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {

        JasperDesign design = ClientObjectProxy.retrieveJasperDesign(remoteForm);
        JasperReport report = JasperCompileManager.compileReport(design);

        JasperPrint print = JasperFillManager.fillReport(report,new HashMap(),
                new ClientReportData(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(remoteForm.getReportDataByteArray())))));
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");
        return prepareViewer(new JRViewer(print));
    }

    private JRViewer prepareViewer(final JRViewer viewer) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                viewer.setFitPageZoomRatio();
            }
        });
        return viewer;
    }

    // закрываются пользователем
    void closed() {
        // пока ничего не делаем
    }
}
