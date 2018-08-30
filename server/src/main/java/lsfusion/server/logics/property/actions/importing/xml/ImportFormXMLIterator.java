package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportFormXMLIterator extends ImportFormIterator {
    private Set<String> attrs;
    private List<Object> children;
    private int i;

    public ImportFormXMLIterator(Pair<String, Object> keyValueRoot, Set<String> attrs) {
        Element root = keyValueRoot.second instanceof Attribute ? null : (Element) keyValueRoot.second;
        this.attrs = attrs;
        this.children = new ArrayList<>();

        if(root != null) {
            for (Object child : ((Element) keyValueRoot.second).getChildren()) {
                if (notLeaf(child)) {
                    this.children.add(child);
                }
            }
            for(Object attribute : root.getAttributes()) {
                if(attribute instanceof Attribute && attrs.contains(((Attribute) attribute).getName())) {
                    children.add(attribute);
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

    private boolean notLeaf(Object child) {
        return !(child instanceof Element && ((Element) child).getChildren().isEmpty() && attrs.contains(((Element) child).getName()));
    }

}