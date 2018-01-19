package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.commons.collections.iterators.SingletonIterator;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

public abstract class ImportXMLIterator extends ImportIterator {
    private boolean attr;
    private Iterator iterator;
    private final List<LCP> properties;
    private final List<String> ids;

    public ImportXMLIterator(byte[] file, List<LCP> properties, List<String> ids, String root, boolean hasListOption, boolean attr) throws JDOMException, IOException {
        this.properties = properties;
        this.attr = attr;
        this.ids = ids;
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new ByteArrayInputStream(file));
        if (root != null) {
            Element rootNode = findRootNode(document.getRootElement(), root);
            if (rootNode != null) {
                //если hasListOption, то берём сам объект, иначе - его children.
                iterator = hasListOption ? new SingletonIterator(rootNode) : rootNode.getChildren().iterator();
            } else {
                throw new RuntimeException(String.format("Import XML error: root node %s not found", root));
            }
        } else {
            Element rootNode = document.getRootElement();
            iterator = hasListOption ? new SingletonIterator(rootNode) : rootNode.getChildren().iterator();
        }

    }

    private Element findRootNode(Element rootNode, String root) {
        if (rootNode.getName().equals(root))
            return rootNode;
        for (Object child : rootNode.getChildren()) {
            Element found = findRootNode((Element) child, root);
            if (found != null)
                return found;
        }
        return null;
    }

    @Override
    public List<String> nextRow() {
        if (iterator.hasNext()) {
            List<String> listRow = new ArrayList<>();
            if (attr) {
                Element element = (Element) iterator.next();
                List<Integer> columns = new ArrayList<Integer>();

                for (String id : ids) {
                    int column = ids.indexOf(id);
                    columns.add(column);

                    Attribute attribute = element.getAttribute(id);
                    listRow.add(attribute == null ? null : formatValue(properties, columns, column, attribute.getValue()));
                }
            } else {
                List<Element> children = ((Element) iterator.next()).getChildren();
                Map<String, Integer> mapping = new HashMap<>();
                for (int i = 0; i < children.size(); i++) {
                    mapping.put(children.get(i).getName(), i);
                }
                List<Integer> columns = getColumns(mapping);

                for (Integer column : columns) {
                    if (column < children.size()) {
                        Element child = children.get(column);
                        listRow.add(formatValue(properties, columns, column, child.getValue()));
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

    private String formatValue(List<LCP> properties, List<Integer> columns, Integer column, String value) {
        DateFormat dateFormat = getDateFormat(properties, columns, column);
        if (dateFormat != null && value != null) {
            value = parseFormatDate(dateFormat, value);
        }
        return value;
    }
}
