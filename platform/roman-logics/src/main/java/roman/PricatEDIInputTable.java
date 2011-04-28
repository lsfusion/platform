package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.integration.EDIInputTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class PricatEDIInputTable extends EDIInputTable {
    public PricatEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
        ((ScanningHandler) handler).addRow();
    }

    protected void init() {
        handler = new ScanningHandler("barcode", "article", "colorCode", "color", "size", "originalName", "country", "netWeight", "composition", "price", "rrp") {
            String docType = null;
            String imd1 = "";
            String imd2 = "";
            String ftx1 = "";

            public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
                String segmentID = atts.getValue("Id");
                if (docType == null) {
                    docType = atts.getValue("DocType");
                } else if (!docType.equals("PRICAT")) {
                    return;
                }

                if (segmentID != null && (segmentID.equals("LIN") || segmentID.equals("UNS"))) {
                    addRow();
                }
                try {
                    if (segmentID != null) {
                        if (segmentID.equals("LIN03")) {
                            row.put("barcode", getTokenValue());
                        } else if (segmentID.equals("PIA02")) {
                            List<String> comp = getComposition();
                            if (comp.get(1).equals("SA")) {
                                row.put("article", comp.get(0));
                            }
                        } else if (segmentID.startsWith("IMD")) {
                            if (segmentID.equals("IMD01")) {
                                imd1 = getTokenValue();
                                imd2 = parser.getTokenizer().nextToken().getValue();
                            } else if (segmentID.equals("IMD03")) {
                                List<String> comp = getComposition();
                                if (imd2.equals("98")) {
                                    if (comp.get(comp.size() - 1).equals("91")) {
                                        row.put("size", comp.get(0));
                                    } else {
                                        row.put("size", comp.get(comp.size() - 1));
                                    }
                                } else if (imd2.equals("35")) {
                                    if (imd1.equals("C")) {
                                        if (comp.get(comp.size() - 1).equals("91")) {
                                            row.put("colorCode", comp.get(0));
                                        } else {
                                            row.put("colorCode", comp.get(comp.size() - 1));
                                        }
                                    } else if (imd1.equals("F")) {
                                        row.put("color", comp.get(comp.size() - 1));
                                    }
                                } else if (imd2.equals("ANM")) {
                                    row.put("originalName", comp.get(comp.size() - 1));
                                } else if (imd2.equals("MD")) {
                                    String composition = "";
                                    for (int i = 3; i< comp.size(); i++) {
                                        composition += comp.get(i);
                                    }
                                    row.put("composition", composition);
                                }
                            }
                        } else if (segmentID.equals("LOC02")) {
                            row.put("country", getTokenValue());
                        } else if (segmentID.equals("MEA03")) {
                            List<String> comp = getComposition();
                            if (comp.get(0).equals("NET")) {
                                row.put("netWeight", comp.get(1));
                            }
                        } else if (segmentID.equals("FTX01")) {
                            ftx1 = getTokenValue();
                        } else if (segmentID.equals("FTX04")) {
                            if (ftx1.equals("SIN")) {
                                row.put("composition", getTokenValue());
                            }
                        } else if (segmentID.equals("PRI01")) {
                            List<String> comp = getComposition();
                            if (comp.get(comp.size() - 1).equals("NTP") || comp.get(comp.size() - 1).equals("LIU")) {
                                row.put("price", comp.get(1));
                            } else if (comp.get(comp.size() - 1).equals("SRP")) {
                                row.put("rrp", comp.get(1));
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
