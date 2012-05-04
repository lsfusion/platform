package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.classes.DateClass;
import platform.server.integration.EDIInputTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

public class SOliverInvoiceEDIInputTable extends EDIInputTable {
    public SOliverInvoiceEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
    }

    protected void init() {
        handler = new ScanningHandler(INVOICE, "barcode", "quantity", "numberSku", "invoiceSID", "boxNumber", "country", "price", "date") {
            String invoiceSID = "";
            String boxNumber = "";
            String date;

            @Override
            public void addRow() {
                if (!row.isEmpty()) {
                    row.put("invoiceSID", invoiceSID);
                    row.put("boxNumber", boxNumber);
                    row.put("date", date);
                    super.addRow();
                }
            }

            public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
                String segmentID = atts.getValue("Id");

                if (!isProperFile(atts)) {
                    return;
                }

                if (segmentID != null && (segmentID.equals("LIN") || segmentID.equals("UNS"))) {
                    addRow();
                }
                try {
                    if (segmentID != null) {
                        if (segmentID.equals("LIN01")) {
                            row.put("numberSku", getTokenValue());
                        } else if (segmentID.equals("LIN03")) {
                            row.put("barcode", getTokenValue());
                        } else if(segmentID.equals("ALI01")) {
                            row.put("country", getTokenValue());
                        } else if (segmentID.equals("QTY01")) {
                            List<String> comp = getComposition();
                            row.put("quantity", comp.get(1));
                        } else if (segmentID.equals("BGM02")) {
                            invoiceSID = getTokenValue();
                            boxNumber = getTokenValue();
                        } else if (segmentID.equals("PRI01")) {
                            List<String> comp = getComposition();
                            row.put("price", comp.get(1));
                        } else if(segmentID.equals("DTM01")) {
                            List<String> comp = getComposition();
                            String stringDate = comp.get(1);
                            Date sDate = new Date(Integer.parseInt(stringDate.substring(0,4))-1900, Integer.parseInt(stringDate.substring(4,6))-1, Integer.parseInt(stringDate.substring(6,8)));
                            date = DateClass.format(sDate);
                        }

                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        };
    }
}
