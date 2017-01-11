package lsfusion.server.logics.property.actions.exporting.csv;

import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CSVFormExporter extends PlainFormExporter {
    private String charset = "utf-8";
    private Map<String, PrintWriter> writersMap = null;
    private Map<String, File> filesMap = null;

    public CSVFormExporter(ReportGenerationData reportData) {
        super(reportData);
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
            writer = new PrintWriter(file, charset);
            filesMap.put(id, file);
            writersMap.put(id, writer);
        }
        for (Map.Entry<String, List<AbstractNode>> childEntry : node.getChildren()) {
            List<AbstractNode> childNode = childEntry.getValue();
            for (AbstractNode c : childNode) {
                if (c instanceof Leaf) {
                    Object value = ((Leaf) c).getValue();
                    writer.print((value == null ? "" : String.valueOf(value).trim()) + ";");
                } else
                    exportRow((Node) c, childEntry.getKey());
            }
        }
        writer.println();
    }

    private void closeWriters() {
        if (writersMap != null) {
            for (PrintWriter writer : writersMap.values())
                writer.close();
        }
    }
}