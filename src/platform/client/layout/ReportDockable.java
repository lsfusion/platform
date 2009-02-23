package platform.client.layout;

import platform.client.navigator.ClientNavigator;
import platform.server.view.form.RemoteForm;
import platform.interop.ByteArraySerializer;

import java.sql.SQLException;
import java.awt.*;
import java.util.HashMap;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.*;

public class ReportDockable extends FormDockable {

    public ReportDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws SQLException {
        super(iformID, navigator, currentSession);
    }

    public ReportDockable(int iformID, ClientNavigator navigator, RemoteForm remoteForm) throws SQLException {
        super(iformID, navigator, remoteForm);
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(0);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(Directory+FileName))),FileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {

        JasperDesign design = ByteArraySerializer.deserializeReportDesign(remoteForm.getReportDesignByteArray());
        try {

            JasperReport report = JasperCompileManager.compileReport(design);

            // вызовем endApply, чтобы быть полностью уверенным в том, что мы работаем с последними данными
            remoteForm.runEndApply();

            JasperPrint print = JasperFillManager.fillReport(report,new HashMap(), ByteArraySerializer.deserializeReportData(remoteForm.getReportDataByteArray()));
            return prepareViewer(new JRViewer(print));

        } catch (JRException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
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
