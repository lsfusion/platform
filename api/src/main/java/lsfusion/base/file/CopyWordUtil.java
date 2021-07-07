package lsfusion.base.file;

import org.apache.poi.xwpf.usermodel.*;

/**
 * source: https://stackoverflow.com/questions/10208097/how-to-copy-a-paragraph-of-docx-to-another-docx-with-java-and-retain-the-style
 * @author Heeseok
 * modified by a.filipchik
 */
public final class CopyWordUtil {

    public static void copyXWPFDocument(XWPFDocument sourceDoc, XWPFDocument destinationDoc) {
        boolean first = true;
        for (IBodyElement bodyElement : sourceDoc.getBodyElements()) {
            BodyElementType elementType = bodyElement.getElementType();
            if (elementType.name().equals("PARAGRAPH")) {
                XWPFParagraph pr = (XWPFParagraph) bodyElement;
                if(first) { //start from new page
                    pr.setPageBreak(true);
                    first = false;
                }
                destinationDoc.createParagraph();

                int pos = destinationDoc.getParagraphs().size() - 1;
                destinationDoc.setParagraph(pr, pos);
            } else if (elementType.name().equals("TABLE")) {
                if(first) {
                    //start from new page
                    XWPFParagraph p = destinationDoc.createParagraph();
                    p.setPageBreak(true);
                    first = false;
                }
                XWPFTable table = (XWPFTable) bodyElement;
                destinationDoc.createTable();
                int pos = destinationDoc.getTables().size() - 1;
                destinationDoc.setTable(pos, table);
            }
        }
    }


}