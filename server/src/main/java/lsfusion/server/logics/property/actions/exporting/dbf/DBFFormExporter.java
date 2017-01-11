package lsfusion.server.logics.property.actions.exporting.dbf;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import jasperapi.ReportPropertyData;
import lsfusion.base.Pair;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;
import org.olap4j.impl.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DBFFormExporter extends PlainFormExporter {
    private String charset;
    private Map<String, String> lastRecordsMap = new ArrayMap<>();
    private Map<String, DBFWriter> writersMap = null;
    private Map<String, File> filesMap = null;

    public DBFFormExporter(ReportGenerationData reportData, String charset) {
        super(reportData);
        this.charset = charset == null ? "cp1251" : charset;
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
            OverJDBField[] fields = getFields(node, id);
            if(fields.length == 0) //не тот subReport
                return;
            File file = File.createTempFile("group_" + id, ".dbf");
            writer = new DBFWriter(file.getAbsolutePath(), fields, charset);
            filesMap.put(id, file);
            writersMap.put(id, writer);
        }
        List<Object> values = new ArrayList();
        for (Map.Entry<String, List<AbstractNode>> childEntry : node.getChildren()) {
            List<AbstractNode> childNode = childEntry.getValue();
            for (AbstractNode c : childNode) {
                if (c instanceof Leaf) {
                    if(((Leaf) c).getType().toDraw.equals(id))
                        values.add(((Leaf) c).getValue());
                } else
                    exportRow((Node) c, childEntry.getKey());
            }
        }
        String currentRecord = String.valueOf(values);
        if(!currentRecord.equals(lastRecordsMap.get(id)) && !values.isEmpty()) {
            lastRecordsMap.put(id, currentRecord);
            writer.addRecord(values.toArray());
        }
    }

    private OverJDBField[] getFields(Node node, String id) throws JDBFException {
        List<OverJDBField> dbfFields = new ArrayList<>();
        List<Map.Entry<String, List<AbstractNode>>> row = node.getChildren();
        List<Pair<String, ReportPropertyData>> fieldNames = new ArrayList<>();
        for (Map.Entry<String, List<AbstractNode>> field : row) {
            AbstractNode leafNode = field.getValue().get(0);
            if (leafNode instanceof Leaf && ((Leaf) leafNode).getType().toDraw.equals(id)) {
                Leaf leaf = (Leaf) field.getValue().get(0);
                fieldNames.add(Pair.create(leaf.getKey(), leaf.getType()));
            }
        }
        for (Pair<String, ReportPropertyData> field : formatFieldNames(fieldNames)) {
            if (field.second.propertyType.equals("DOUBLE"))
                dbfFields.add(new OverJDBField(field.first, 'F', 10, 3));
            else if (field.second.propertyType.equals("INTEGER") || field.second.propertyType.equals("NUMERIC"))
                dbfFields.add(new OverJDBField(field.first, 'N', Math.min(field.second.length, 253), field.second.precision));
            else if (field.second.propertyType.equals("DATE") || field.second.propertyType.equals("DATETIME")) {
                dbfFields.add(new OverJDBField(field.first, 'D', 8, 0));
            } else if (field.second.propertyType.equals("BOOLEAN"))
                dbfFields.add(new OverJDBField(field.first, 'L', 1, 0));
            else if (field.second.propertyType.contains("STRING"))
                dbfFields.add(new OverJDBField(field.first, 'C', Math.min(field.second.length, 253), 0));
            else if(field.second.propertyType.equals("VARTEXT"))
                dbfFields.add(new OverJDBField(field.first, 'C', 253, 0));
            else if(field.second.propertyType.equals("TIME"))
                dbfFields.add(new OverJDBField(field.first, 'C', 8, 0));
            else
                dbfFields.add(new OverJDBField(field.first, 'C', 253, 0));
        }
        return dbfFields.toArray(new OverJDBField[dbfFields.size()]);
    }

    private List<Pair<String, ReportPropertyData>> formatFieldNames(List<Pair<String, ReportPropertyData>> fieldNames) {
        int maxLength = 10;
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            Pair<String, ReportPropertyData> field = fieldNames.get(i);
            String fieldName = field.first;
            if (fieldName.length() > maxLength) {
                fieldName = trim(fieldName, maxLength);
                fieldNames.set(i, Pair.create(fieldName, field.second));
            }
            Integer frequency = frequencyMap.get(fieldName);
            frequencyMap.put(fieldName, frequency == null ? 1 : ++frequency);
        }
        Map<String, Integer> countMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            Pair<String, ReportPropertyData> field = fieldNames.get(i);
            String fieldName = field.first;
            if (frequencyMap.get(fieldName) > 1) {
                Integer count = countMap.get(fieldName);
                count = count == null ? 1 : ++count;
                countMap.put(fieldName, count);
                fieldNames.set(i, Pair.create(trim(fieldName, Math.min(fieldName.length(), maxLength - (int) (Math.log10(count) + 1))) + count, field.second));
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