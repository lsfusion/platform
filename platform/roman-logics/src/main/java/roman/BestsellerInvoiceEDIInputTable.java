package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.integration.EDIInputTable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BestsellerInvoiceEDIInputTable extends EDIInputTable {

    public BestsellerInvoiceEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
        handler = new ScanningHandler("country", "colourCode", "colour", "size", "netWeight", "quantity", "price", "invoiceSID", "sid", "barcode",
                "boxNumber", "customCode", "customCode6", "composition", "originalName", "numberSku") {
            String imd1 = ""
                    ,
                    imd2 = "";
            String price = "";
            String country = "";
            String netWeight = "";
            String invoiceSID = "";
            String sid = "";
            String boxNumber = "";
            String barcode = "";

            public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
                String segmentID = atts.getValue("Id");

                if (segmentID != null && (segmentID.equals("LIN") || segmentID.equals("UNS"))) {
                    if (row.get("LINtype") != null && !row.get("LINtype").equals("EN")) {
                        boolean barcodeAgain = false;
                        for (List<String> listRow : data) {
                            if (listRow.get(9).equals(barcode)) {
                                listRow.set(5, Double.toString(Double.parseDouble(listRow.get(5)) + Double.parseDouble(row.get("quantity"))));
                                barcodeAgain = true;
                                break;
                            }
                        }
                        if (!barcodeAgain) {
                            row.put("price", price);
                            row.put("country", country);
                            row.put("netWeight", netWeight);
                            row.put("invoiceSID", invoiceSID);
                            row.put("sid", sid);
                            row.put("boxNumber", boxNumber);
                            row.put("barcode", barcode);
                            List<String> single = new ArrayList<String>();
                            for (String column : columns) {
                                single.add(row.get(column) == null ? "" : row.get(column));
                            }
                            data.add(single);
                            row = new HashMap<String, String>();
                        }
                    }
                }
                try {
                    if (segmentID != null) {
                        if (segmentID.equals("BGM02")) {
                            invoiceSID = getTokenValue();
                            boxNumber = getTokenValue();
                        } else if (segmentID.equals("PIA02")) {
                            sid = getTokenValue();
                        } else if (segmentID.equals("LIN01")) {
                            row.put("numberSku", getTokenValue());
                        } else if (segmentID.equals("LIN03")) {
                            barcode = getTokenValue();
                            if (atts.getValue("Composite") != null) {
                                row.put("LINtype", getComposition().get(1));
                            } else {
                                row.put("LINtype", "1");
                            }
                        } else if (segmentID.equals("QTY01")) {
                            List<String> comp = getComposition();
                            if (comp.get(0).equals("52")) {
                                row.put("quantity", comp.get(1));
                            }
                        } else if (segmentID.equals("PRI01")) {
                            List<String> comp = getComposition();
                            price = comp.get(1);
                        } else if (segmentID.startsWith("IMD")) {
                            if (segmentID.equals("IMD01")) {
                                imd1 = getTokenValue();
                                imd2 = parser.getTokenizer().nextToken().getValue();
                            } else if (segmentID.equals("IMD03")) {
                                List<String> comp = getComposition();
                                if (imd2.equals("98")) {
                                    row.put("size", comp.get(3));
                                } else if (imd2.equals("35")) {
                                    if (imd1.equals("C")) {
                                        row.put("colourCode", comp.get(3));
                                    } else if (imd1.equals("F")) {
                                        row.put("colour", comp.get(3));
                                    }
                                } else if (imd2.equals("ANM")) {
                                    row.put("originalName", comp.get(3));
                                }
                            }
                        } else if (segmentID.equals("LOC02")) {
                            country = getTokenValue();
                        } else if (segmentID.equals("MEA03")) {
                            List<String> comp = getComposition();
                            if (comp.get(0).equals("NET")) {
                                netWeight = comp.get(1);
                            }
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        };

        read();
    }
}
