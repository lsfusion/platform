package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

public class PrintUtils {

    public static void printFile(RawFileData fileData, String filePath, String printerName, String trayName, boolean duplex) {
        try {

            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

            PrintService printer = null;
            if (printerName != null) {
                for (PrintService printService : PrintServiceLookup.lookupPrintServices(flavor, attributeSet)) {
                    if (printService.getName().equals(printerName)) {
                        printer = printService;
                        break;
                    }
                }
            } else {
                printer = PrintServiceLookup.lookupDefaultPrintService();
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
                        Media media = getTray(printer, flavor, attributeSet, trayName);
                        if (media != null) {
                            attrSet.add(media);
                        }
                        if (duplex && printer.isAttributeValueSupported(Sides.DUPLEX, flavor, attributeSet)) {
                            attrSet.add(Sides.DUPLEX);
                        }

                        job.print(attrSet);
                    }

                } finally {
                    if (fileData != null) {
                        FileUtils.safeDelete(file);
                    }
                }
            }
        } catch (PrinterException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static Media getTray(PrintService printer, DocFlavor flavor, PrintRequestAttributeSet attributeSet, String trayName) {
        if(trayName != null) {
            Media[] supportedMedia = (Media[]) printer.getSupportedAttributeValues(Media.class, flavor, attributeSet);
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
