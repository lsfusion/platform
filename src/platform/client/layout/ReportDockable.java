package platform.client.layout;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;

public class ReportDockable extends FormDockable {

    public ReportDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws SQLException {
        super(iformID, navigator, currentSession);
    }

    public ReportDockable(int iformID, ClientNavigator navigator, RemoteFormInterface remoteForm) throws SQLException {
        super(iformID, navigator, remoteForm);
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(0);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(Directory+FileName))),FileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) {

        JasperDesign design = DeSerializer.deserializeReportDesign(remoteForm.getReportDesignByteArray());
        try {

            JasperReport report = JasperCompileManager.compileReport(design);

            JasperPrint print = JasperFillManager.fillReport(report,new HashMap(), DeSerializer.deserializeReportData(remoteForm.getReportDataByteArray()));
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
