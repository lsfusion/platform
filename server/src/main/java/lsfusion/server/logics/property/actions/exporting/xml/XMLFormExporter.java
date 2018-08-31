package lsfusion.server.logics.property.actions.exporting.xml;

import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XMLFormExporter extends HierarchicalFormExporter {
    private String formName;
    private Set<String> attrs;

    public XMLFormExporter(ReportGenerationData reportData, String formName, Map<String, List<String>> formObjectGroups, Map<String, List<String>> formPropertyGroups, Set<String> attrs) {
        super(reportData, formObjectGroups, formPropertyGroups);
        this.formName = formName;
        this.attrs = attrs;
    }

    @Override
    public byte[] exportNodes(List<Node> rootNodes) throws IOException {
        File file = null;
        try {
            Element rootElement = new Element(formName);
            for (Node rootNode : rootNodes) {
                for(Map.Entry<String, List<AbstractNode>> nodeEntry : rootNode.getChildren()) {
                    for(AbstractNode childNode : nodeEntry.getValue())
                        exportNode(rootElement, childNode);
                }
            }

            file = File.createTempFile(formName, ".xml");
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(ExternalUtils.defaultXMLJSONCharset));
            try(PrintWriter fw = new PrintWriter(file, ExternalUtils.defaultXMLJSONCharset)) {
                xmlOutput.output(new Document(rootElement), fw);
            }
            return IOUtils.getFileBytes(file);
        } finally {
            if (file != null && !file.delete())
                file.deleteOnExit();
        }
    }

    private void exportNode(Element parentElement, AbstractNode node) {
        if (node instanceof Leaf) {
            String leafValue = getLeafValue((Leaf) node);
            if (leafValue != null) {
                parentElement.addContent(leafValue);
            }
        } else if (node instanceof Node) {
            for (Map.Entry<String, List<AbstractNode>> child : ((Node) node).getChildren()) {
                for (AbstractNode childNode : child.getValue()) {
                    Element subParentElement = null;
                    boolean isLeaf = childNode instanceof Leaf;
                    if (!isLeaf || ((Leaf) childNode).getType().toDraw.equals(parentElement.getName())) {
                        List<String> groups = isLeaf ? formPropertyGroups.get(child.getKey()) : formObjectGroups.get(child.getKey());
                        if(groups != null) {
                            for (int i = groups.size() - 1; i >= 0; i--) {
                                Element subElement = findChild(subParentElement != null ? subParentElement : parentElement, groups.get(i));
                                if (subElement == null) {
                                    subElement = new Element(groups.get(i));
                                    if (subParentElement == null) {
                                        parentElement.addContent(subElement);
                                    } else {
                                        subParentElement.addContent(subElement);
                                    }

                                }
                                subParentElement = subElement;
                            }
                        }
                        if (isLeaf && attrs.contains(child.getKey())) {
                            if(subParentElement != null) {
                                subParentElement.setAttribute(child.getKey(), getLeafValue((Leaf) childNode));
                            } else {
                                parentElement.setAttribute(child.getKey(), getLeafValue((Leaf) childNode));
                            }
                        } else {
                            Element element = new Element(child.getKey());
                            exportNode(element, childNode);
                            if (!element.getValue().isEmpty() || !element.getAttributes().isEmpty()) {
                                if(subParentElement != null) {
                                    subParentElement.addContent(element);
                                } else {
                                    parentElement.addContent(element);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Element findChild(Element parent, String child) {
        Element result = null;
        for (Object c : parent.getChildren()) {
            if (((Element) c).getName().equals(child))
                result = (Element) c;
        }
        return result;
    }
}