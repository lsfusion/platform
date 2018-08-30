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
    private Set<String> attrs;

    public XMLFormExporter(ReportGenerationData reportData, Set<String> attrs) {
        super(reportData);
        this.attrs = attrs;
    }

    @Override
    public byte[] exportNodes(List<Node> rootNodes) throws IOException {
        File file = null;
        try {
            Element rootElement = new Element("export");
            for (Node rootNode : rootNodes) {
                for(Map.Entry<String, List<AbstractNode>> nodeEntry : rootNode.getChildren()) {
                    for(AbstractNode childNode : nodeEntry.getValue())
                        exportNode(rootElement, childNode);
                }
            }

            file = File.createTempFile("exportForm", ".xml");
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
                    if (!(childNode instanceof Leaf) || ((Leaf) childNode).getType().toDraw.equals(parentElement.getName())) {
                        if (childNode instanceof Leaf && attrs != null && (attrs.isEmpty() || attrs.contains(parentElement.getName()))) {
                            parentElement.setAttribute(child.getKey(), getLeafValue((Leaf) childNode));
                        } else {
                            Element element = new Element(child.getKey());
                            exportNode(element, childNode);
                            if (!element.getValue().isEmpty() || !element.getAttributes().isEmpty()) {
                                parentElement.addContent(element);
                            }
                        }
                    }
                }
            }
        }
    }
}