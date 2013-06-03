package lsfusion.server.integration;

import com.berryworks.edireader.EDIReader;
import com.berryworks.edireader.EDIReaderFactory;
import com.berryworks.edireader.EDISyntaxException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import lsfusion.server.logics.DataObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EDIInputTable implements ImportInputTable {
    protected DataObject supplier;
    protected List<List<String>> data = new ArrayList<List<String>>();
    protected InputSource inputSource;
    protected EDIReader parser;
    protected ContentHandler handler;
    public final String PRICAT = "PRICAT";
    public final String INVOICE = "INVOIC";

    public EDIInputTable(ByteArrayInputStream inFile) {
        this(inFile, null);
    }

    public EDIInputTable(ByteArrayInputStream inFile, DataObject supplier) {
        this.supplier = supplier;
        inputSource = new InputSource(new InputStreamReader(inFile));
        init();
        read();
    }

    protected void init() {}

    protected void read() {
        try {
            while (true) {
                parser = EDIReaderFactory.createEDIReader(inputSource);
                if (parser == null) {
                    break;
                }
                parser.setContentHandler(handler);
                parser.parse(inputSource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        return data.get(0).size();
    }

    abstract protected class ScanningHandler extends DefaultHandler {
        protected Map<String, String> row = new HashMap<String, String>();
        protected String[] columns;
        protected String docType = null;
        protected String requiredDocType;

        public ScanningHandler(String requiredDocType, String... columns) {
            super();
            this.requiredDocType = requiredDocType;
            this.columns = columns;
        }

        public abstract void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException;

        public boolean isProperFile(Attributes atts) {
            if (docType == null) {
                docType = atts.getValue("DocType");
            } else if (!docType.equals(requiredDocType)) {
                return false;
            }
            return true;
        }

        public void addRow() {
            if (!row.isEmpty()) {
                List<String> single = new ArrayList<String>();
                for (String column : columns) {
                    single.add(row.get(column) == null ? "" : row.get(column));
                }
                data.add(single);
                row = new HashMap<String, String>();
            }
        }

        public String getTokenValue() throws IOException, EDISyntaxException {
            parser.getTokenizer().ungetToken();
            return parser.getTokenizer().nextToken().getValue();
        }

        public List<String> getComposition() {
            try {
                parser.getTokenizer().ungetToken();
                return parser.getTokenizer().nextCompositeElement();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (EDISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
