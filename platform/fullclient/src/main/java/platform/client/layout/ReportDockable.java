package platform.client.layout;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.client.Log;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ReportDockable extends FormDockable {

    public ReportDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException {
        super(iformID, navigator, currentSession);
    }

    public ReportDockable(ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        super(navigator, remoteForm);
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(0);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(Directory+FileName))),FileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        try {
            return prepareViewer(new JRViewer(createJasperPrint(remoteForm, false)));
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
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


    private static final Map<Integer, JasperDesign> cacheJasperDesign = new HashMap();

    private static JasperDesign retrieveJasperDesign(RemoteFormInterface remoteForm, boolean toExcel) throws IOException, ClassNotFoundException {

        int ID = remoteForm.getID();

        if (!remoteForm.hasCustomReportDesign() || !cacheJasperDesign.containsKey(ID)) {

            byte[] state = remoteForm.getReportDesignByteArray(toExcel);
            Log.incrementBytesReceived(state.length);

            cacheJasperDesign.put(ID, (JasperDesign) new ObjectInputStream(new CompressingInputStream(new ByteArrayInputStream(state))).readObject());
        }

        return cacheJasperDesign.get(ID);
    }

    public static JasperPrint createJasperPrint(RemoteFormInterface remoteForm, boolean toExcel) throws ClassNotFoundException, IOException, JRException {

        JasperDesign design = retrieveJasperDesign(remoteForm, toExcel);
        JasperReport report = JasperCompileManager.compileReport(design);

        JasperPrint print = JasperFillManager.fillReport(report,new HashMap(),
                new ClientReportData(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(remoteForm.getReportDataByteArray())))));
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

        return print;
    }

    public static void exportToExcel(RemoteFormInterface remoteForm) {

        try {

            File tempFile = File.createTempFile("lsf", ".xls");

            JExcelApiExporter xlsExporter = new JExcelApiExporter();
            xlsExporter.setParameter(JRExporterParameter.JASPER_PRINT, createJasperPrint(remoteForm, true));
            xlsExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
            xlsExporter.exportReport();

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            }

            tempFile.deleteOnExit();
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в Excel", e);
        }
    }
}
