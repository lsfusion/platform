package jasperapi;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.interop.form.ReportConstants.*;

public class ClientPropertyData {

    // property readers with suffixes values and objects
    public final Map<String, Set<Integer>> objects = new HashMap<>();
    public final Map<String, Map<Map<Integer, Object>, Object>> data = new HashMap<>();

    // property draws - column groups
    public final Map<String, Set<Integer>> columnParentObjects = new HashMap<>();
    public final Map<String, Map<Map<Integer, Object>, List<Map<Integer, Object>>>> columnData = new HashMap<>();

    public ClientPropertyData(DataInputStream inStream, Map<Map<Integer, Object>, Map<Integer, Object>> cache) throws IOException {

        int propCnt = inStream.readInt();
        for (int i = 0; i < propCnt; i++) {
            String propID = inStream.readUTF();

            List<Integer> objectList = deserializeObjects(inStream);
            objects.put(propID, new HashSet<>(objectList));

            int rowCnt = inStream.readInt();
            Map<Map<Integer, Object>, Object> row = new HashMap<>();
            for (int j = 0; j < rowCnt; j++)
                row.put(deserializeObjectValues(inStream, objectList, cache), BaseUtils.deserializeObject(inStream));
            data.put(propID, row);
        }

        propCnt = inStream.readInt();
        for (int i = 0; i < propCnt; i++) {
            String propID = inStream.readUTF();

            List<Integer> parentColumnObjects = deserializeObjects(inStream);
            List<Integer> thisColumnObjects = deserializeObjects(inStream);

            int rowCnt = inStream.readInt();
            Map<Map<Integer, Object>, List<Map<Integer, Object>>> values = new HashMap<>();
            for (int j = 0; j < rowCnt; j++) {
                Map<Integer, Object> parentObjectValues = deserializeObjectValues(inStream, parentColumnObjects, cache);

                List<Map<Integer, Object>> thisObjectValues = new ArrayList<>();
                int columnCnt = inStream.readInt();
                for(int k = 0; k < columnCnt; k++)
                    thisObjectValues.add(deserializeObjectValues(inStream, thisColumnObjects, cache));

                values.put(parentObjectValues, thisObjectValues);
            }
            columnParentObjects.put(propID, new HashSet<>(parentColumnObjects));
            columnData.put(propID, values);
        }
    }

    private Map<Integer, Object> deserializeObjectValues(DataInputStream inStream, List<Integer> objectList, Map<Map<Integer, Object>, Map<Integer, Object>> cache) throws IOException {
        Map<Integer, Object> objectValues = new HashMap<>();
        for (Integer object : objectList)
            objectValues.put(object, BaseUtils.deserializeObject(inStream));
        return ClientKeyData.cacheRow(objectValues, cache);
    }

    private List<Integer> deserializeObjects(DataInputStream inStream) throws IOException {
        int objectCnt = inStream.readInt();
        List<Integer> objectList = new ArrayList<>();
        for (int j = 0; j < objectCnt; j++) {
            int id = inStream.readInt();
            objectList.add(id);
        }
        return objectList;
    }

    public int getColumnsCount(String fieldName, Result<Integer> minColumnsCount) {
        String baseFieldName = ReportGenerator.getBaseFieldName(fieldName);
        int max = 0;
        int min = Integer.MAX_VALUE;
        for(List<Map<Integer, Object>> columns : columnData.get(baseFieldName).values()) {
            int columnsCount = columns.size();
            max = BaseUtils.max(max, columnsCount);
            min = BaseUtils.min(min, columnsCount);
        }
        if(minColumnsCount != null)
            minColumnsCount.set(min);
        return max;
    }
    private Map<Map<Integer, Object>, List<Map<Integer, Object>>> getColumns(String fieldName) {
        String baseFieldName = ReportGenerator.getBaseFieldName(fieldName);
        return columnData.get(baseFieldName);
    }

    private List<Map<Integer, Object>> getColumns(String fieldName, Map<Integer, Object> currentKeyRow) {
        String baseFieldName = ReportGenerator.getBaseFieldName(fieldName);
        List<Map<Integer, Object>> columns = columnData.get(baseFieldName).get(BaseUtils.filterInclKeys(currentKeyRow, columnParentObjects.get(baseFieldName)));
        if(columns == null)
            return Collections.emptyList();
        return columns;
    }

    public Object getFieldValue(Map<Integer, Object> currentKeyRow, String fieldName) {
        int columnIndex = 0;
        if (fieldName.endsWith(endIndexMarker)) { // group columns
            int markerPos = fieldName.substring(0, fieldName.length() - endIndexMarker.length()).lastIndexOf(beginIndexMarker);

            columnIndex = Integer.parseInt(fieldName.substring(markerPos + beginIndexMarker.length(), fieldName.length() - endIndexMarker.length()));
            fieldName = fieldName.substring(0, markerPos);
        }

        List<Map<Integer, Object>> columns = getColumns(fieldName, currentKeyRow);
        if(columnIndex >= columns.size())
            return null;
        Map<Integer, Object> columnRow = columns.get(columnIndex);
        currentKeyRow = BaseUtils.merge(currentKeyRow, columnRow);

        return data.get(fieldName).get(BaseUtils.filterInclKeys(currentKeyRow, objects.get(fieldName)));
    }
}
