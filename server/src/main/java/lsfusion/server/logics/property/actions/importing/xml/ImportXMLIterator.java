package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public abstract class ImportXMLIterator extends ImportIterator {
    private Iterator iterator;
    private List<Integer> columns = null;

    public ImportXMLIterator(byte[] file) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new ByteArrayInputStream(file));
        Element rootNode = document.getRootElement();
        
        iterator = rootNode.getChildren().iterator();
    }

    @Override
    public List<String> nextRow() {
        if (iterator.hasNext()) {
            List<Attribute> attributes = ((Element) iterator.next()).getAttributes();
            if (columns == null) {
                Map<String, Integer> mapping = new HashMap<String, Integer>();
                for (int i = 0; i < attributes.size(); i++) {
                    mapping.put(attributes.get(i).getName(), i);
                }
                columns = getColumns(mapping);
            }

            List<String> listRow = new ArrayList<String>();
            for (Integer column : columns) {
                if (column < attributes.size()) {
                    Attribute attribute = attributes.get(column);
                    listRow.add(attribute.getValue());
                }
            }
            return listRow;
        }
        
        return null;
    }

    @Override
    protected void release() {
    }

    public abstract List<Integer> getColumns(Map<String, Integer> mapping);
}
