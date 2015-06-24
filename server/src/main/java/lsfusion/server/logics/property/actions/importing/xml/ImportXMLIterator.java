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
    private boolean attr;
    private Iterator iterator;
    private List<Integer> columns = null;

    public ImportXMLIterator(byte[] file, boolean attr) throws JDOMException, IOException {
        this.attr = attr;
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new ByteArrayInputStream(file));
        Element rootNode = document.getRootElement();
        
        iterator = rootNode.getChildren().iterator();
    }

    @Override
    public List<String> nextRow() {
        if (iterator.hasNext()) {
            List<String> listRow = new ArrayList<>();
            if(attr) {
                List<Attribute> attributes = ((Element) iterator.next()).getAttributes();
                if (columns == null) {
                    Map<String, Integer> mapping = new HashMap<>();
                    for (int i = 0; i < attributes.size(); i++) {
                        mapping.put(attributes.get(i).getName(), i);
                    }
                    columns = getColumns(mapping);
                }


                for (Integer column : columns) {
                    if (column < attributes.size()) {
                        Attribute attribute = attributes.get(column);
                        listRow.add(attribute.getValue());
                    }
                }
            } else {
                List<Element> children = ((Element) iterator.next()).getChildren();
                if (columns == null) {
                    Map<String, Integer> mapping = new HashMap<>();
                    for (int i = 0; i < children.size(); i++) {
                        mapping.put(children.get(i).getName(), i);
                    }
                    columns = getColumns(mapping);
                }

                for (Integer column : columns) {
                    if (column < children.size()) {
                        Element child = children.get(column);
                        listRow.add(child.getValue());
                    }
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
