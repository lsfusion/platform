package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.classes.DateClass;
import platform.server.integration.EDIInputTable;

import java.io.*;
import java.sql.Date;
import java.util.List;

public class BestsellerInvoiceEDIInputTable extends EDIInputTable {

    public BestsellerInvoiceEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
    }

    protected void init() {
        handler = new ScanningHandler(INVOICE, "barcode", "quantity", "numberSku", "invoiceSID", "boxNumber", "country", "sid",
                /*"colour", "colourCode",*/ "size", "originalName", "netWeight", "price", "date") {
            String imd1 = ""
                    ,
                    imd2 = "";
            String date;
            String price = "";
            String country = "";
            String netWeight = "";
            String invoiceSID = "";
            String sid = "";
            String boxNumber = "";
            String barcode = "";


            @Override
            public void addRow() {
                if (row.get("LINtype") != null && !row.get("LINtype").equals("EN")) {
                    boolean barcodeAgain = false;
                    for (List<String> listRow : data) {
                        if (listRow.get(0).equals(barcode)) {
                            listRow.set(1, Double.toString(Double.parseDouble(listRow.get(1)) + Double.parseDouble(row.get("quantity"))));
                            barcodeAgain = true;
                            break;
                        }
                    }
                    if (!barcodeAgain) {
                        row.put("date", date);
                        row.put("price", price);
                        row.put("country", country);
                        row.put("netWeight", netWeight);
                        row.put("invoiceSID", invoiceSID);
                        row.put("sid", sid);
                        row.put("boxNumber", boxNumber);
                        row.put("barcode", barcode);

                        super.addRow();
                    }
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
                        } else if (segmentID.equals("DTM01")) {
                            List<String> comp = getComposition();
                            String stringDate="";
                            if (comp.get(0).equals("137"))
                            {
                            stringDate = comp.get(1);
                            Date sDate = new Date(Integer.parseInt(stringDate.substring(0, 4)) - 1900, Integer.parseInt(stringDate.substring(4, 6)) - 1, Integer.parseInt(stringDate.substring(6, 8)));
                            date = DateClass.format(sDate);
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
    }
}
