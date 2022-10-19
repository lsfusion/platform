package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.print.ReportGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.nullEmpty;

public class PrintUtils {

    public static void printFile(RawFileData fileData, String filePath, String printerName, String trayName, boolean duplex) {
        try {

            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

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

            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printerName != null) {
                for (PrintService printService : PrintServiceLookup.lookupPrintServices(flavor, attributeSet)) {
                    if (printService.getName().equals(printerName)) {
                        printer = printService;
                        break;
                    }
                }
            }

            if (printer != null) {
                File file = null;
                try {
                    if (fileData != null) {
                        file = File.createTempFile("print", ".pdf");
                        fileData.write(file);
                    } else {
                        file = new File(filePath);
                    }

                    try (PDDocument document = PDDocument.load(file)) {
                        PrinterJob job = PrinterJob.getPrinterJob();
                        job.setPageable(new PDFPageable(document));
                        job.setPrintService(printer);

                        HashPrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

                        String sidesValue = printOptions.getOrDefault(ReportGenerator.SIDES_PROPERTY_NAME, duplex ? "two-sided-long-edge" : null);
                        Sides sides = ReportGenerator.SIDES_VALUES.get(sidesValue);
                        if (sides != null) {
                            attrSet.add(sides);
                        }

                        String trayValue = printOptions.getOrDefault(ReportGenerator.TRAY_PROPERTY_NAME, trayName);
                        Media media = getTray(printer, trayValue);
                        if (media != null) {
                            attrSet.add(media);
                        }

                        String sheetCollateValue = printOptions.get(ReportGenerator.SHEET_COLLATE_PROPERTY_NAME);
                        if ("true".equals(sheetCollateValue)) {
                            attrSet.add(SheetCollate.COLLATED);
                        } else if ("false".equals(sheetCollateValue)) {
                            attrSet.add(SheetCollate.UNCOLLATED);
                        }

                        String copiesValue = printOptions.get("copies");
                        if(copiesValue != null) {
                            Integer copies = Integer.parseInt(copiesValue);
                            if(copies > 0) {
                                attrSet.add(new Copies(copies));
                            }
                        }

                        job.print(attrSet);
                    }

                } finally {
                    if (fileData != null) {
                        BaseUtils.safeDelete(file);
                    }
                }
            }
        } catch (PrinterException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static Media getTray(PrintService printer, String trayName) {
        if(trayName != null) {
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
