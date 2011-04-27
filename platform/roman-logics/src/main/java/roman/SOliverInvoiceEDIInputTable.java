package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.integration.EDIInputTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class SOliverInvoiceEDIInputTable extends EDIInputTable {
    public SOliverInvoiceEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
    }

    protected void init() {
        handler = new ScanningHandler("barcode", "quantity", "numberSku", "invoiceSID", "boxNumber") {
            String invoiceSID = "";
            String boxNumber = "";

            @Override
            public void addRow() {
                if (!row.isEmpty()) {
                    row.put("invoiceSID", invoiceSID);
                    row.put("boxNumber", boxNumber);
                    super.addRow();
                }
            }

            public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
                String segmentID = atts.getValue("Id");

                if (segmentID != null && (segmentID.equals("LIN") || segmentID.equals("UNS"))) {
                    addRow();
                }
                try {
                    if (segmentID != null) {
                        if (segmentID.equals("LIN01")) {
                            row.put("numberSku", getTokenValue());
                        } else if (segmentID.equals("LIN03")) {
                            row.put("barcode", getTokenValue());
                        } else if (segmentID.equals("QTY01")) {
                            List<String> comp = getComposition();
                            row.put("quantity", comp.get(1));
                        } else if (segmentID.equals("BGM02")) {
                            invoiceSID = getTokenValue();
                            boxNumber = getTokenValue();
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        };
    }
}
