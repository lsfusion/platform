package lsfusion.client.form.print;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.log.Log;
import lsfusion.client.controller.MainController;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.export.XlsReportConfiguration;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
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

                PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
                if(printerName != null) {
                    for (PrintService service : PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, new HashPrintRequestAttributeSet())) {
                        if(service.getName().equals(printerName)) {
                            printService = service;
                            break;
                        }
                    }
                }

                JasperPrint print = new ReportGenerator(generationData).createReport(FormPrintType.PRINT, MainController.remoteLogics);
                print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");

                PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
//                printRequestAttributeSet.add(MediaSizeName.ISO_A4);

                String sidesProp = printOptions.getOrDefault(ReportGenerator.SIDES_PROPERTY_NAME, print.getProperty(ReportGenerator.SIDES_PROPERTY_NAME));
                Sides sides = ReportGenerator.SIDES_VALUES.get(sidesProp);
                if (sides != null) {
                    printRequestAttributeSet.add(sides);
                }

                MediaTray tray = null;
                String trayProp = printOptions.getOrDefault(ReportGenerator.TRAY_PROPERTY_NAME, print.getProperty(ReportGenerator.TRAY_PROPERTY_NAME));
                Object o = printService.getSupportedAttributeValues(Media.class, DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
                if (o instanceof Media[]) {
                    for (Media media : (Media[]) o) {
                        if(media instanceof MediaTray) {
                            if(media.toString().equals(trayProp)) {
                                tray = (MediaTray) media;
                            }
                        }
                    }
                }

                if (tray != null) {
                    printRequestAttributeSet.add(tray);
                }

                String sheetCollate = printOptions.getOrDefault(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME, print.getProperty(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME));
                if ("true".equals(sheetCollate)) {
                    printRequestAttributeSet.add(SheetCollate.COLLATED);
                } else if ("false".equals(sheetCollate)) {
                    printRequestAttributeSet.add(SheetCollate.UNCOLLATED);
                }

                String copies = printOptions.get("copies");
                if(copies != null) {
                    printRequestAttributeSet.add(new Copies(Integer.parseInt(copies)));
                }

                PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
                printServiceAttributeSet.add(new PrinterName(printService.getName(), null));

                JRPrintServiceExporter exporter = new JRPrintServiceExporter();

                exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
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
