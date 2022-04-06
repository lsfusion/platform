package lsfusion.interop.action;

import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.export.XlsReportConfiguration;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static lsfusion.base.BaseUtils.nullEmpty;

public class ServerPrintAction implements Runnable {
    private final ReportGenerationData generationData;
    private String printerName;
    private final Consumer<Exception> exceptionLogger;
    private static final DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void autoPrintReport(ReportGenerationData generationData, String printerName, Consumer<Exception> exceptionLogger) {
        executorService.submit(new ServerPrintAction(generationData, printerName, exceptionLogger));
    }

    private ServerPrintAction(ReportGenerationData generationData, String printerName, Consumer<Exception> exceptionLogger) {
        this.generationData = generationData;
        this.printerName = printerName;
        this.exceptionLogger = exceptionLogger;
    }

    @Override
    public void run() {
        try {
            Map<String, String> printOptions = new HashMap<>();
            if (printerName != null) {
                String[] splitted = printerName.split(";");
                printerName = nullEmpty(splitted[0]);
                for (int i = 1; i < splitted.length; i++) {
                    String[] entry = splitted[i].split("=");
                    if (entry.length == 2) {
                        printOptions.put(entry[0], entry[1]);
                    }
                }
            }

            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printerName != null) {
                for (PrintService service : PrintServiceLookup.lookupPrintServices(flavor, new HashPrintRequestAttributeSet())) {
                    if (service.getName().equals(printerName)) {
                        printer = service;
                        break;
                    }
                }
            }

            JasperPrint print = new ReportGenerator(generationData).createReport(FormPrintType.PRINT, null);
            print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");

            PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

            String sidesValue = printOptions.getOrDefault(ReportGenerator.SIDES_PROPERTY_NAME, print.getProperty(ReportGenerator.SIDES_PROPERTY_NAME));
            Sides sides = ReportGenerator.SIDES_VALUES.get(sidesValue);
            if (sides != null) {
                attrSet.add(sides);
            }

            String trayValue = printOptions.getOrDefault(ReportGenerator.TRAY_PROPERTY_NAME, print.getProperty(ReportGenerator.TRAY_PROPERTY_NAME));
            Media tray = getTray(printer, trayValue);
            if (tray != null) {
                attrSet.add(tray);
            }

            String sheetCollateValue = printOptions.getOrDefault(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME, print.getProperty(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME));
            if ("true".equals(sheetCollateValue)) {
                attrSet.add(SheetCollate.COLLATED);
            } else if ("false".equals(sheetCollateValue)) {
                attrSet.add(SheetCollate.UNCOLLATED);
            }

            String copiesValue = printOptions.get("copies");
            if (copiesValue != null) {
                int copies = Integer.parseInt(copiesValue);
                if (copies > 0) {
                    attrSet.add(new Copies(copies));
                }
            }

            PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
            printServiceAttributeSet.add(new PrinterName(printer.getName(), null));

            JRPrintServiceExporter exporter = new JRPrintServiceExporter();

            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_REQUEST_ATTRIBUTE_SET, attrSet);
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE_ATTRIBUTE_SET, printServiceAttributeSet);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG, Boolean.FALSE);
            exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG, Boolean.FALSE);

            exporter.exportReport();
        } catch (Exception e) {
            exceptionLogger.accept(e);
        }
    }

    private static Media getTray(PrintService printer, String trayName) {
        if (trayName != null) {
            Media[] supportedMedia = (Media[]) printer.getSupportedAttributeValues(Media.class, DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
            if (supportedMedia != null) {
                for (Media media : supportedMedia) {
                    if (media instanceof MediaTray) {
                        if (media.toString().equals(trayName)) {
                            return media;
                        }
                    }
                }
            }
        }
        return null;
    }
}
