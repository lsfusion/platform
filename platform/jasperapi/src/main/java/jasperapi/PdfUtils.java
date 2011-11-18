package jasperapi;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.codec.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PdfUtils {

    public static void mergePDFs(List<InputStream> streamOfPDFFiles, List<String> titles, OutputStream outputStream, boolean paginate) {

        Document document = new Document();
        try {
            List<InputStream> pdfs = streamOfPDFFiles;
            List<PdfReader> readers = new ArrayList<PdfReader>();
            int totalPages = 0;
            Iterator<InputStream> iteratorPDFs = pdfs.iterator();
            Iterator<String> iteratorTitles = titles.iterator();

            // Create Readers for the pdfs.
            while (iteratorPDFs.hasNext()) {
                InputStream pdf = iteratorPDFs.next();
                PdfReader pdfReader = new PdfReader(pdf);
                readers.add(pdfReader);
                totalPages += pdfReader.getNumberOfPages();
            }
            // Create a writer for the outputstream
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();
            BaseFont bf = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            //BaseFont bf = BaseFont.createFont("D:\\platform\\jasperapi\\src\\main\\resource\\times.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            //BaseFont bf = BaseFont.createFont("Arial", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            PdfContentByte cb = writer.getDirectContent(); // Holds the PDF
            // data

            PdfImportedPage page;
            int currentPageNumber = 0;
            int pageOfCurrentReaderPDF = 0;
            Iterator<PdfReader> iteratorPDFReader = readers.iterator();

            // Loop through the PDF files and add to the output.
            while (iteratorPDFReader.hasNext()) {
                PdfReader pdfReader = iteratorPDFReader.next();
                String title = iteratorTitles.next();

                // Create a new page in the target for each source page.
                while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {

                    pageOfCurrentReaderPDF++;
                    currentPageNumber++;
                    document.setPageSize(pdfReader.getPageSizeWithRotation(pageOfCurrentReaderPDF));
                    document.newPage();
                    page = writer.getImportedPage(pdfReader, pageOfCurrentReaderPDF);
                    cb.addTemplate(page, 0, 0);

                    // Code for pagination.
                    if (paginate) {
                        cb.beginText();
                        cb.setFontAndSize(bf, 9);
                        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "" + currentPageNumber + " of " + totalPages, 520, 5, 0);
                        cb.endText();
                    }

                    if(title!=null) {
                        cb.beginText();
                        cb.setFontAndSize(bf, 10);
                        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, title, 50, page.getHeight()-20, 0);
                        cb.endText();
                    }
                }
                pageOfCurrentReaderPDF = 0;
            }
            outputStream.flush();
            document.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen())
                document.close();
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
