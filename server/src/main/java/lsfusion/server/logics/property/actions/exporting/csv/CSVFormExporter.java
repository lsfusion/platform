package lsfusion.server.logics.property.actions.exporting.csv;

import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CSVFormExporter extends PlainFormExporter {
    private boolean noHeader;
    private String separator;
    private String charset;
    private Map<String, PrintWriter> writersMap = null;
    private Map<String, File> filesMap = null;

    public CSVFormExporter(ReportGenerationData reportData, boolean noHeader, String separator, String charset) {
        super(reportData);
        this.noHeader = noHeader;
        this.separator = separator == null ? "|" : separator;
        this.charset = charset == null ? "UTF-8" : charset;
    }

    @Override
    public Map<String, byte[]> exportNodes(List<Node> rootNodes) throws IOException {
        try {
            writersMap = new HashMap<>();
            filesMap = new LinkedHashMap<>();
            for (Node rootNode : rootNodes)
                exportNode(rootNode);
            closeWriters();
            return getFilesBytes(filesMap);
        } finally {
            closeWriters();
            deleteFiles(filesMap);
        }
    }

    private void exportNode(Node node) throws IOException {
        for (Map.Entry<String, List<AbstractNode>> child : node.getChildren()) {
            for (AbstractNode childNode : child.getValue())
                exportFile((Node) childNode);
        }
    }

    private void exportFile(Node node) throws IOException {
        for (Map.Entry<String, List<AbstractNode>> child : node.getChildren()) {
            for (AbstractNode childNode : child.getValue()) {
                exportRow((Node) childNode, child.getKey());
            }
        }
    }

    private void exportRow(Node node, String id) throws IOException {
        PrintWriter writer = writersMap.get(id);
        if (writer == null) { //объявляем
            File file = File.createTempFile("group_" + id, ".csv");
            writer = new PrintWriter(file, charset) {
                @Override
                public void println() {
                    write("\r\n");
                }
            };
            filesMap.put(id, file);
            writersMap.put(id, writer);
            if(!noHeader)
                writer.println(getHeaders(node, id));
        }
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, List<AbstractNode>> childEntry : node.getChildren()) {
            List<AbstractNode> childNode = childEntry.getValue();
            for (AbstractNode c : childNode) {
                if (c instanceof Leaf) {
                    if(((Leaf) c).getType().toDraw.equals(id)) {
                        values.add(((Leaf) c).getValue());
                    }
                } else
                    exportRow((Node) c, childEntry.getKey());
            }
        }
        if(!emptyRow(values)) {
            for (int i = 0; i < values.size(); i++) {
                writer.print((values.get(i) == null ? "" : String.valueOf(values.get(i)).trim()) + (values.size() == (i + 1) ? "" : separator));
            }
            if(!values.isEmpty())
                writer.println();
        }
    }

    private String getHeaders(Node node, String id) {
        String headers = "";
        for (Map.Entry<String, List<AbstractNode>> field : node.getChildren()) {
            AbstractNode leafNode = field.getValue().get(0);
            if (leafNode instanceof Leaf && ((Leaf) leafNode).getType().toDraw.equals(id)) {
                headers += (headers.isEmpty() ? "" : separator) + (((Leaf) field.getValue().get(0)).getKey());
            }
        }
        return headers;
    }

    private void closeWriters() {
        if (writersMap != null) {
            for (PrintWriter writer : writersMap.values())
                writer.close();
        }
    }
}