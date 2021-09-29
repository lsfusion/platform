package lsfusion.client.form.print;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.log.Log;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.export.XlsReportConfiguration;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import java.awt.print.PrinterAbortException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static lsfusion.base.BaseUtils.nullEmpty;

public class ClientReportUtils {

    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void autoprintReport(ReportGenerationData generationData, String printerName) {
        
        executorService.submit(new RunAutoPrintReport(generationData, printerName));
        
    }

    static class RunAutoPrintReport implements Runnable {
        ReportGenerationData generationData;
        String printerName;

        RunAutoPrintReport(ReportGenerationData generationData, String printerName) {
            this.generationData = generationData;
            this.printerName = printerName;
        }

        @Override
        public void run() {
            try {

                Map<String, String> printOptions = new HashMap<>();
                if(printerName != null) {
                    String[] splitted = printerName.split(";");
                    printerName = nullEmpty(splitted[0]);
                    for(int i = 1; i < splitted.length; i++) {
                        String[] entry = splitted[i].split("=");
                        if(entry.length == 2) {
                            printOptions.put(entry[0], entry[1]);
                        }
                    }
                }

                JasperPrint print = new ReportGenerator(generationData).createReport(FormPrintType.PRINT);
                print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");

                PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();

                PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
//                printRequestAttributeSet.add(MediaSizeName.ISO_A4);

                String sidesProp = printOptions.getOrDefault(ReportGenerator.SIDES_PROPERTY_NAME, print.getProperty(ReportGenerator.SIDES_PROPERTY_NAME));
                Sides sides = ReportGenerator.SIDES_VALUES.get(sidesProp);
                if (sides != null) {
                    printRequestAttributeSet.add(sides);
                }

                String trayProp = printOptions.getOrDefault(ReportGenerator.TRAY_PROPERTY_NAME, print.getProperty(ReportGenerator.TRAY_PROPERTY_NAME));
                MediaTray tray = ReportGenerator.TRAY_VALUES.get(trayProp);
                if (tray != null) {
                    printRequestAttributeSet.add(tray);
                }

                String sheetCollate = printOptions.getOrDefault(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME, print.getProperty(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME));
                if ("true".equals(sheetCollate)) {
                    printRequestAttributeSet.add(SheetCollate.COLLATED);
                } else if ("false".equals(sheetCollate)) {
                    printRequestAttributeSet.add(SheetCollate.UNCOLLATED);
                }

                PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
                if(printerName != null)
                    printServiceAttributeSet.add(new PrinterName(printerName, null));

                JRPrintServiceExporter exporter = new JRPrintServiceExporter();

                exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                if(printerName == null)
                    exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE, defaultPrintService);
                exporter.setParameter(JRPrintServiceExporterParameter.PRINT_REQUEST_ATTRIBUTE_SET, printRequestAttributeSet);
                exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE_ATTRIBUTE_SET, printServiceAttributeSet);
                exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG, Boolean.FALSE);
                exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG, Boolean.FALSE);

                exporter.exportReport();
            } catch (Exception e) {
                if (e instanceof JRException && e.getCause() instanceof PrinterAbortException) {
                    Log.message(ClientResourceBundle.getString("form.error.print.job.aborted"), false);
                } else {
                    ClientExceptionManager.handle(e, false);
                }
            }
        }
    }
}
