package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;
import platform.base.Pair;
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

    private JasperDesignWrapper design = null;

    public ReportDockable(int formID, ClientNavigator navigator, boolean currentSession, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        super(formID, navigator, currentSession, factory);
    }

    public ReportDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable,?> factory) throws ClassNotFoundException, IOException {
        super(navigator, remoteForm, factory);
    }

    // из файла
    ReportDockable(String fileName,String directory, MultipleCDockableFactory<FormDockable,?> factory) throws JRException {
        super(0, factory);

        setActiveComponent(prepareViewer(new JRViewer((JasperPrint) JRLoader.loadObject(directory+fileName))),fileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        try {
            design = retrieveJasperDesign(remoteForm, false);
            return prepareViewer(new JRViewer(createJasperPrint(remoteForm, design.getJasperDesign())));
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getCaption() {
        return design != null ? design.getCaption() : "";
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


    private static final Map<Pair<Integer, Boolean>, JasperDesignWrapper> cacheJasperDesign = new HashMap();

    private static JasperDesignWrapper retrieveJasperDesign(RemoteFormInterface remoteForm, boolean toExcel) throws IOException, ClassNotFoundException {
        Pair idExcel = new Pair(remoteForm.getID(), toExcel);

        if (!remoteForm.hasCustomReportDesign() || !cacheJasperDesign.containsKey(idExcel)) {

            byte[] state = remoteForm.getReportDesignByteArray(toExcel);
            Log.incrementBytesReceived(state.length);

            ObjectInputStream objIn = new ObjectInputStream(new CompressingInputStream(new ByteArrayInputStream(state)));
            String caption = objIn.readUTF();
            JasperDesign jasperDesign = (JasperDesign) objIn.readObject();
            cacheJasperDesign.put(idExcel, new JasperDesignWrapper(jasperDesign, caption));
        }

        return cacheJasperDesign.get(idExcel);
    }

    public static JasperPrint createJasperPrint(RemoteFormInterface remoteForm, JasperDesign design) throws ClassNotFoundException, IOException, JRException {
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
            JasperDesignWrapper design = retrieveJasperDesign(remoteForm, true);
            xlsExporter.setParameter(JRExporterParameter.JASPER_PRINT, createJasperPrint(remoteForm, design.getJasperDesign()));
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
