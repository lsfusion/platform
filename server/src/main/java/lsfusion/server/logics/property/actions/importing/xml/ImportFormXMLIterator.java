package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportFormXMLIterator extends ImportFormIterator {
    private List<String> attrs;
    private List<Object> children;
    private int i;

    public ImportFormXMLIterator(Pair<String, Object> keyValueRoot, List<String> attrs, Map<String, String> headers) {
        Element root = keyValueRoot.second instanceof Attribute ? null : (Element) keyValueRoot.second;
        this.attrs = attrs;
        this.children = new ArrayList<>();

        if(root != null) {
            boolean isAttr = attrs != null && (attrs.isEmpty() || attrs.contains(root.getName()));

            if (isAttr) {
                for (Object child : ((Element) keyValueRoot.second).getChildren()) {
                    if (headers.containsKey(((Element) child).getName())) {
                        for (Object c : ((Element) child).getChildren()) {
                            if (!isLeaf(c)) {
                                this.children.add(c);
                            }
                        }
                    } else {
                        if (!isLeaf(child)) {
                            this.children.add(child);
                        }
                    }
                }
                children.addAll(root.getAttributes());
            } else {
                for (Object child : ((Element) keyValueRoot.second).getChildren()) {
                    if (headers.containsKey(((Element) child).getName())) {
                        this.children.addAll(((Element) child).getChildren());
                    } else {
                        this.children.add(child);
                    }
                }
            }
        }
        i = 0;
    }

    @Override
    public boolean hasNext() {
        return children != null && children.size() > i;
    }

    @Override
    public Pair<String, Object> next() {
        if (children != null) {
            Object child = children.get(i);
            Pair<String, Object> entry = Pair.create(child instanceof Attribute ? ((Attribute) child).getName() : ((Element) child).getName(), child);
            i++;
            return entry;
        } else
            return null;
    }

    @Override
    public void remove() {
    }

    private boolean isLeaf(Object child) {
        boolean isAttr = attrs != null && (attrs.isEmpty() || (child instanceof Element && attrs.contains(((Element) child).getName())));
        return child instanceof Attribute || (child instanceof Element && (isAttr ? ((Element) child).getAttributes().isEmpty() : ((Element) child).getChildren().isEmpty()));
    }

}