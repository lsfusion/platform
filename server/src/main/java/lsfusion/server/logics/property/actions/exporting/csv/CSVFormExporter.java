package lsfusion.server.logics.property.actions.exporting.csv;

import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;
import org.olap4j.impl.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CSVFormExporter extends PlainFormExporter {
    private String separator;
    private String charset;
    private Map<String, String> lastRecordsMap = new ArrayMap<>();
    private Map<String, PrintWriter> writersMap = null;
    private Map<String, File> filesMap = null;

    public CSVFormExporter(ReportGenerationData reportData, String separator, String charset) {
        super(reportData);
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
            writer = new PrintWriter(file, charset);
            filesMap.put(id, file);
            writersMap.put(id, writer);
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
        String currentRecord = String.valueOf(values);
        if(!currentRecord.equals(lastRecordsMap.get(id)) && !values.isEmpty()) {
            lastRecordsMap.put(id, currentRecord);
            for (int i = 0; i < values.size(); i++) {
                writer.print((values.get(i) == null ? "" : String.valueOf(values.get(i)).trim()) + (values.size() == (i + 1) ? "" : separator));
            }
            if(!values.isEmpty())
                writer.println();
        }
    }

    private void closeWriters() {
        if (writersMap != null) {
            for (PrintWriter writer : writersMap.values())
                writer.close();
        }
    }
}