package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.EDIInputTable;
import platform.server.logics.DataObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class PricatEDIInputTable extends EDIInputTable {
    public PricatEDIInputTable(ByteArrayInputStream inFile, DataObject supplier) {
        super(inFile, supplier);
    }

    protected void init() {
        handler = new ScanningHandler(PRICAT, "barcode", "article", "customCode", "colorCode", "color", "size", "originalName",
                "country", "netWeight", "composition", "price", "rrp", "season", "gender", "brandName", "brandCode", "themeCode", "themeName") {
            String imd1 = "";
            String imd2 = "";
            String ftx1 = "";

            public void endDocument() {
                addRow();
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
                        if (segmentID.equals("LIN03")) {
                            row.put("barcode", getTokenValue());
                        } else if (segmentID.equals("PIA02")) {
                            List<String> comp = getComposition();
                            if (comp.get(1).equals("SA")) {
                                String article = comp.get(0);
                                if (supplier != null && ((ConcreteCustomClass) supplier.objectClass).getSID().equals("sOliverSupplier")) {
                                    article = new StringBuffer(article).insert(2, '.').insert(6, '.').insert(9, '.').toString();
                                }
                                row.put("article", article);
                            } else if (comp.get(1).equals("HS")) {
                                StringBuilder sb = new StringBuilder(comp.get(0));
                                for (int i = sb.length(); i < 10; i++) {
                                    sb.append(0);
                                }
                                row.put("customCode", sb.toString());
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
                                    } else if (imd1.equals("B")) {
                                        row.put("color", comp.get(comp.size() - 1));
                                    }
                                } else if ((imd1.equals("F"))&&(imd2.equals("FIC"))) {
                                        row.put("gender", comp.get(comp.size() - 1));
                                    }
                                else if (imd2.equals("ANM")) {
                                    row.put("originalName", comp.get(comp.size() - 1));
                                }  else if(imd2.equals(("TDS"))) {
                                   row.put("composition", comp.get(3));
                                } else if(imd2.equals(("XX6"))) {
                                   row.put("season", comp.get(3));
                                } else if(imd2.equals("BRN")){
                                    if (comp.size() >= 4) {
                                        row.put("brandName", comp.get(3));
                                        row.put("brandCode", comp.get(3));
                                    }
                                }
                            }
                            } else if (segmentID.equals("ALI01")) {
                            row.put("country", getTokenValue());
                        } else if (segmentID.equals("MEA03")) {
                            List<String> comp = getComposition();
                            if (comp.get(0).equals("NET")) {
                                row.put("netWeight", comp.get(1));
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
