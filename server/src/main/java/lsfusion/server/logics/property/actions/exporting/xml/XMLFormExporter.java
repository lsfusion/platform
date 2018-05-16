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

public class XMLFormExporter extends HierarchicalFormExporter {
    private String charset = "utf-8";
    private Map<String, String> headers;

    public XMLFormExporter(ReportGenerationData reportData, Map<String, String> headers) {
        super(reportData);
        this.headers = headers;
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
            xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(charset));
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
            Object value = ((Leaf) node).getValue();
            if(value != null)
                parentElement.addContent(String.valueOf(value));
        } else if (node instanceof Node) {
            for (Map.Entry<String, List<AbstractNode>> child : ((Node) node).getChildren()) {

                Element headerElement = null;
                String headerValue = headers.get(child.getKey());
                if (headerValue != null) {
                    headerElement = new Element(headerValue);
                    parentElement.addContent(headerElement);
                }

                for (AbstractNode childNode : child.getValue()) {
                    if (!(childNode instanceof Leaf) || ((Leaf) childNode).getType().toDraw.equals(parentElement.getName())) {
                        Element element = new Element(child.getKey());
                        exportNode(element, childNode);
                        if (!element.getValue().isEmpty()) {
                            if (headerElement != null) {
                                headerElement.addContent(element);
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