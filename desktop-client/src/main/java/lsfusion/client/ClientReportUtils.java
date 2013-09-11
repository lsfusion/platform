package lsfusion.client;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;
import java.util.HashMap;
import java.util.Map;

public class ClientReportUtils {
    private static final Map<String, Sides> SIDES_VALUES = new HashMap<String, Sides>();
    private static final Map<String, MediaTray> TRAY_VALUES = new HashMap<String, MediaTray>();
    
    static {
        SIDES_VALUES.put("one-sided", Sides.ONE_SIDED);
        SIDES_VALUES.put("two-sided-long-edge", Sides.TWO_SIDED_LONG_EDGE);
        SIDES_VALUES.put("two-sided-short-edge", Sides.TWO_SIDED_SHORT_EDGE);
        
        TRAY_VALUES.put("top", MediaTray.TOP);
        TRAY_VALUES.put("middle", MediaTray.MIDDLE);
        TRAY_VALUES.put("bottom", MediaTray.BOTTOM);
        TRAY_VALUES.put("envelope", MediaTray.ENVELOPE);
        TRAY_VALUES.put("manual", MediaTray.MANUAL);
        TRAY_VALUES.put("large-capacity", MediaTray.LARGE_CAPACITY);
        TRAY_VALUES.put("main", MediaTray.MAIN);
        TRAY_VALUES.put("side", MediaTray.SIDE);
    }
    
    public static void autoprintReport(ReportGenerationData generationData) {
        try {
            JasperPrint print = new ReportGenerator(generationData, Main.timeZone).createReport(false, null);
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();

            PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
            printRequestAttributeSet.add(MediaSizeName.ISO_A4);

            String sidesProp = print.getProperty(ReportGenerator.SIDES_PROPERTY_NAME);
            Sides sides = SIDES_VALUES.get(sidesProp);
            if (sides != null) {
                printRequestAttributeSet.add(sides);
            }
            
            String trayProp = print.getProperty(ReportGenerator.TRAY_PROPERTY_NAME);
            MediaTray tray = TRAY_VALUES.get(trayProp);
            if (tray != null) {
                printRequestAttributeSet.add(tray);
            }

            PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();

            JRPrintServiceExporter exporter = new JRPrintServiceExporter();

            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE, defaultPrintService);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_REQUEST_ATTRIBUTE_SET, printRequestAttributeSet);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE_ATTRIBUTE_SET, printServiceAttributeSet);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG, Boolean.FALSE);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG, Boolean.FALSE);

            exporter.exportReport();

        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
