package lsfusion.server.logics.property.actions.importing.xml;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.property.actions.importing.ImportFormHierarchicalDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class ImportFormXMLDataActionProperty extends ImportFormHierarchicalDataActionProperty<Element> {
    private boolean attr; //пока не используется, для чтения attributes, а не children
    private Map<String, String> headers;

    public ImportFormXMLDataActionProperty(ValueClass[] classes, FormEntity formEntity, boolean attr, Map<String, String> headers) {
        super(classes, formEntity);
        this.attr = attr;
        this.headers = headers;
    }

    @Override
    public Element getRootElement(byte[] file) {
        try {
            return findRootNode(new SAXBuilder().build(new ByteArrayInputStream(file)).getRootElement(), root);
        } catch (JDOMException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImportFormIterator getIterator(Pair<String, Object> rootElement) {
        return new ImportFormXMLIterator(rootElement, headers);
    }

    @Override
    public String getChildValue(Object child) {
        return ((Element) child).getText();
    }

    @Override
    public boolean isLeaf(Object child) {
        return ((Element) child).getChildren().isEmpty();
    }

    private Element findRootNode(Element rootNode, String root) {
        if (root == null || rootNode.getName().equals(root))
            return rootNode;
        for (Object child : rootNode.getChildren()) {
            Element found = findRootNode((Element) child, root);
            if (found != null)
                return found;
        }
        throw new RuntimeException(String.format("Import XML error: root node %s not found", root));
    }
}