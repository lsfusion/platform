package roman;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import platform.server.integration.EDIInputTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BestsellerCompositionEDIInputTable extends EDIInputTable {
    public BestsellerCompositionEDIInputTable(ByteArrayInputStream inFile) {
        super(inFile);
        handler = new ScanningHandler("sid", "composition") {
            String ftx1 = "";

            public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
                String segmentID = atts.getValue("Id");

                if (segmentID != null && (segmentID.equals("LIN") || segmentID.equals("UNS"))) {
                    if (!row.isEmpty()) {
                        List<String> single = new ArrayList<String>();
                        for (String column : columns) {
                            single.add(row.get(column) == null ? "" : row.get(column));
                        }
                        data.add(single);
                        row = new HashMap<String, String>();
                    }
                }
                try {
                    if (segmentID != null) {
                        if (segmentID.equals("FTX01")) {
                            ftx1 = getTokenValue();
                        } else if (segmentID.equals("FTX04")) {
                            if (ftx1.equals("SIN")) {
                                row.put("composition", getTokenValue());
                            }
                        } else if (segmentID.equals("PIA02")) {
                            List<String> comp = getComposition();
                            if (comp.get(1).equals("SA")) {
                                row.put("sid", comp.get(0));
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
