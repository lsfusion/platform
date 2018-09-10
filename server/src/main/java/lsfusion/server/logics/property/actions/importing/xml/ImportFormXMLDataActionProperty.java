package lsfusion.server.logics.property.actions.importing.xml;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportFormHierarchicalDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ImportFormXMLDataActionProperty extends ImportFormHierarchicalDataActionProperty<Element> {
    private Set<String> attrs;

    public static Set<String> calcAttrs(FormEntity formEntity) {
        Set<String> attrs = new HashSet<>();
        for(PropertyDrawEntity<?> property : formEntity.getPropertyDrawsIt()) {
            if(property.attr)
                attrs.add(property.getShortSID());
        }
        return attrs;
    }
    @ManualLazy
    private Set<String> getAttrs() {
        if(attrs == null)
            attrs = calcAttrs(formEntity);
        return attrs;
    }

    public ImportFormXMLDataActionProperty(ValueClass[] classes, LCP<?> fileProperty, FormEntity formEntity) {
        super(classes, fileProperty, formEntity);
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
        return new ImportFormXMLIterator(rootElement, getObjectGroups(), getPropertyGroups(), getAttrs());
    }

    @Override
    public String getChildValue(Object child) {
        return child instanceof Attribute ? ((Attribute) child).getValue() : ((Element) child).getText();
    }

    @Override
    public boolean isLeaf(Object child) {
        return child instanceof Attribute || (child instanceof Element && ((Element) child).getChildren().isEmpty() && ((Element) child).getAttributes().isEmpty());
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