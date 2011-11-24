package jasperapi;

import com.lowagie.text.*;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.codec.Base64;

import java.awt.*;
import java.awt.Font;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PdfUtils {

    public static File makeStamp(InputStream inputFile, String title) throws IOException, DocumentException {

        File outputFile = File.createTempFile("output", ".pdf");
        PdfReader reader = new PdfReader(inputFile);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputFile));
        BaseFont bf = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            PdfContentByte content = stamper.getOverContent(i);

            if (title != null) {
                content.beginText();
                content.setFontAndSize(bf, 10);
                content.showTextAligned(PdfContentByte.ALIGN_LEFT, title, 40, reader.getPageSizeWithRotation(i).getHeight() - 20, 0);
                content.endText();
            }
        }
        stamper.close();

        return outputFile;


    }
}
