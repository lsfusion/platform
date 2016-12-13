package lsfusion.server.logics.property.actions.exporting.dbf;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DBFFormExporter extends PlainFormExporter {
    private String charset = "utf-8";
    private Map<String, DBFWriter> writersMap = null;
    private Map<String, File> filesMap = null;

    public DBFFormExporter(ReportGenerationData reportData) {
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
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        } finally {
            closeWriters();
            deleteFiles(filesMap);
        }
    }

    private void exportNode(Node node) throws IOException, JDBFException {
        for (Map.Entry<String, List<AbstractNode>> child : node.getChildren()) {
            for (AbstractNode childNode : child.getValue())
                exportFile((Node) childNode);
        }
    }

    private void exportFile(Node node) throws IOException, JDBFException {
        for (Map.Entry<String, List<AbstractNode>> child : node.getChildren()) {
            for (AbstractNode childNode : child.getValue()) {
                exportRow((Node) childNode, child.getKey());
            }
        }
    }

    private void exportRow(Node node, String id) throws IOException, JDBFException {
        DBFWriter writer = writersMap.get(id);
        if (writer == null) { //объявляем
            File file = File.createTempFile("group_" + id, ".dbf");
            writer = new DBFWriter(file.getAbsolutePath(), getFields(node), charset);
            filesMap.put(id, file);
            writersMap.put(id, writer);
        }
        List<Object> values = new ArrayList();
        for (Map.Entry<String, List<AbstractNode>> childEntry : node.getChildren()) {
            List<AbstractNode> childNode = childEntry.getValue();
            for (AbstractNode c : childNode) {
                if (c instanceof Leaf) {
                    String value = ((Leaf) c).getValue();
                    values.add(value);
                } else
                    exportRow((Node) c, childEntry.getKey());
            }
        }
        writer.addRecord(values.toArray());
    }

    private OverJDBField[] getFields(Node node) throws JDBFException {
        List<OverJDBField> dbfFields = new ArrayList<>();
        Set<Map.Entry<String, List<AbstractNode>>> row = node.getChildren();
        List<String> fieldNames = new ArrayList<>();
        for (Map.Entry<String, List<AbstractNode>> field : row) {
            if (field.getValue().get(0) instanceof Leaf) {
                fieldNames.add(((Leaf) field.getValue().get(0)).getKey());
            }
        }
        for (String field : formatFieldNames(fieldNames)) {
            dbfFields.add(new OverJDBField(field, 'C', 100, 0));
        }
        return dbfFields.toArray(new OverJDBField[dbfFields.size()]);
    }

    private List<String> formatFieldNames(List<String> fieldNames) {
        int maxLength = 10;
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            if (fieldName.length() > maxLength) {
                fieldName = trim(fieldName, maxLength);
                fieldNames.set(i, fieldName);
            }
            Integer frequency = frequencyMap.get(fieldName);
            frequencyMap.put(fieldName, frequency == null ? 1 : ++frequency);
        }
        Map<String, Integer> countMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            if (frequencyMap.get(fieldName) > 1) {
                Integer count = countMap.get(fieldName);
                count = count == null ? 1 : ++count;
                countMap.put(fieldName, count);
                fieldNames.set(i, trim(fieldName, Math.min(fieldName.length(), maxLength - (int) (Math.log10(count) + 1))) + count);
            }
        }
        return fieldNames;
    }

    private String trim(String value, int len) {
        return value.substring(0, Math.min(value.length(), len));
    }

    private void closeWriters() {
        if (writersMap != null) {
            for (DBFWriter writer : writersMap.values())
                try {
                    writer.close();
                } catch (JDBFException e) {
                    e.printStackTrace();
                }
            writersMap = null;
        }
    }
}