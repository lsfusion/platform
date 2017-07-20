package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Element;
import java.util.List;

public class ImportFormXMLIterator extends ImportFormIterator {

    private Element root;
    private List<Element> children;
    private int i;

    public ImportFormXMLIterator(Pair<String, Object> keyValueRoot) {
        this.root = (Element) keyValueRoot.second;
        this.children = ((Element)keyValueRoot.second).getChildren();
        i = 0;
    }

    @Override
    public boolean hasNext() {
        return children != null && children.size() > i;
    }

    @Override
    public Pair<String, Object> next() {
        if (children != null) {
            Element child = children.get(i);
            Pair<String, Object> entry = Pair.create(child.getName(), (Object) child);
            i++;
            return entry;
        } else
            return null;
    }

    @Override
    public void remove() {
    }
}