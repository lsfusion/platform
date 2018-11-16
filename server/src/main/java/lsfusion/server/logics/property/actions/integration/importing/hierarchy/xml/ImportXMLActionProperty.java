package lsfusion.server.logics.property.actions.integration.importing.hierarchy.xml;

import com.google.common.base.Throwables;
import lsfusion.base.RawFileData;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.property.actions.integration.hierarchy.xml.XMLNode;
import lsfusion.server.logics.property.actions.integration.importing.hierarchy.ImportHierarchicalActionProperty;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImportXMLActionProperty extends ImportHierarchicalActionProperty<XMLNode> {

    public ImportXMLActionProperty(int paramsCount, FormEntity formEntity) {
        super(paramsCount, formEntity);
    }

    @Override
    public XMLNode getRootNode(RawFileData fileData, String root) {
        try {
            return new XMLNode(findRootNode(fileData, root));
        } catch (JDOMException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Element findRootNode(RawFileData file, String root) throws JDOMException, IOException {
        Element rootNode = findRootNode(new SAXBuilder().build(file.getInputStream()).getRootElement(), root);
        if(rootNode == null)
            throw new RuntimeException(String.format("Import XML error: root node %s not found", root));
        return rootNode;
    }

    private static Element findRootNode(Element rootNode, String root) {
        if (root == null || rootNode.getName().equals(root))
            return rootNode;
        for (Object child : rootNode.getChildren()) {
            Element found = findRootNode((Element) child, root);
            if (found != null)
                return found;
        }
        return null;
    }
}