package lsfusion.server.logics.property.actions.integration.importing.hierarchy.xml;

import com.google.common.base.Throwables;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.hierarchy.xml.XMLNode;
import lsfusion.server.logics.property.actions.integration.importing.hierarchy.ImportHierarchicalActionProperty;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImportXMLActionProperty extends ImportHierarchicalActionProperty<XMLNode> {

    public ImportXMLActionProperty(int paramsCount, LCP<?> fileProperty, FormEntity formEntity) {
        super(paramsCount, fileProperty, formEntity);
    }

    @Override
    public XMLNode getRootNode(byte[] file, String root) {
        try {
            return new XMLNode(findRootNode(file, root));
        } catch (JDOMException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Element findRootNode(byte[] file, String root) throws JDOMException, IOException {
        return findRootNode(new SAXBuilder().build(new ByteArrayInputStream(file)).getRootElement(), root);
    }

    private static Element findRootNode(Element rootNode, String root) {
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