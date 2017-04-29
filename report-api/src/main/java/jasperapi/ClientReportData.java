package jasperapi;

import lsfusion.base.BaseUtils;
import lsfusion.base.ByteArray;
import lsfusion.base.DateConverter;
import lsfusion.base.Pair;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.interop.form.ReportGenerationDataType;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.interop.form.ReportConstants.*;

public class ClientReportData implements JRDataSource {
    private final List<String> objectNames = new ArrayList<>();
    private final List<String> propertyNames = new ArrayList<>();  
    private final Map<String, Integer> objects = new HashMap<>();
    private final Map<String, ReportPropertyData> properties = new HashMap<>();

    private final ListIterator<HashMap<Integer, Object>> iterator;
    private final List<HashMap<Integer, Object>> keyRows = new ArrayList<>();
    private HashMap<Integer, Object> currentKeyRow;
    private final Map<Map<Integer, Object>, Map<ReportPropertyData, Object>> rows =
            new HashMap<>();

    private Map<String, List<Integer>> compositeFieldsObjects;
    private Map<String, Map<List<Object>, Object>> compositeObjectValues;
    private Map<String, List<Integer>> compositeColumnObjects;
    private Map<String, List<List<Object>>> compositeColumnValues;

    private final Map<ByteArray, String> files;

    public ClientReportData(DataInputStream inStream, Map<ByteArray, String> files, ReportGenerationDataType reportType, boolean fixBoolean) throws IOException {

        if (!inStream.readBoolean() || !reportType.isPrintJasper()) {
            int objectCnt = inStream.readInt();
            for (int i = 0; i < objectCnt; i++) {
                String name = inStream.readUTF();
                objectNames.add(name);
                objects.put(name, inStream.readInt());
            }

            int propCnt = inStream.readInt();
            for (int i = 0; i < propCnt; i++) {
                String name = inStream.readUTF();
                int type = inStream.readInt();
                if (type == PropertyReadType.CAPTION) {
                    name += headerSuffix;
                } else if (type == PropertyReadType.FOOTER) {
                    name += footerSuffix;
                }
                propertyNames.add(name);
                properties.put(name, new ReportPropertyData(type, inStream.readInt(), inStream.readUTF(), inStream.readUTF(), inStream.readInt(), inStream.readInt()));
            }

            int rowCnt = inStream.readInt();
            for (int i = 0; i < rowCnt; i++) {
                HashMap<Integer, Object> objectValues = new HashMap<>();
                for (String objName : objectNames) {
                    Object objValue = BaseUtils.deserializeObject(inStream);
                    objectValues.put(objects.get(objName), objValue);
                }
                Map<ReportPropertyData, Object> propValues = new HashMap<>();
                for (String propName : propertyNames) {
                    Object propValue = BaseUtils.deserializeObject(inStream);
                    ReportPropertyData property = properties.get(propName);
                    if(fixBoolean && propValue == null && isBoolean(property))
                        propValue = false;
                    propValues.put(property, propValue);
                }
                keyRows.add(objectValues);
                rows.put(objectValues, propValues);
            }
        }
        iterator = keyRows.listIterator();

        this.files = files;
    }

    private boolean isBoolean(ReportPropertyData property) {
        return property != null && property.propertyType != null && property.propertyType.equals("BOOLEAN");
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public Map<String, ReportPropertyData> getProperties() {
        return properties;
    }

    public List<HashMap<Integer, Object>> getKeyRows() {
        return keyRows;
    }

    public Map<Map<Integer, Object>, Map<ReportPropertyData, Object>> getRows() {
        return rows;
    }

    public Map<String, List<Integer>> getCompositeFieldsObjects() {
        return compositeFieldsObjects;
    }

    public Map<String, Map<List<Object>, Object>> getCompositeObjectValues() {
        return compositeObjectValues;
    }

    public Map<String, List<Integer>> getCompositeColumnObjects() {
        return compositeColumnObjects;
    }

    public Map<String, List<List<Object>>> getCompositeColumnValues() {
        return compositeColumnValues;
    }

    public boolean next() throws JRException {
        if(!iterator.hasNext()) return false;
        currentKeyRow = iterator.next();
        return true;
    }

    public void revert() {
        assert iterator.hasPrevious();
        iterator.previous();
    }

    public void setCompositeData(Map<String, List<Integer>> fieldObjects,
                                 Map<String, Map<List<Object>, Object>> objectValues,
                                 Map<String, List<Integer>> columnObjects,
                                 Map<String, List<List<Object>>> columnValues) {
        compositeFieldsObjects = fieldObjects;
        compositeObjectValues = objectValues;
        compositeColumnObjects = columnObjects;
        compositeColumnValues = columnValues; 
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        
        Object value = null;
        
        Integer objectID = getObjectIdByFieldName(fieldName);
        if (objectID != null) {
            value = currentKeyRow.get(objectID);
        } else {
            ReportPropertyData  propertyID = properties.get(fieldName);
            if (propertyID != null) {
                value = rows.get(currentKeyRow).get(propertyID);
            } else if (fieldName.endsWith(endIndexMarker)) { 
                Pair<Integer, String> extractData = extractFieldData(fieldName);
                int index = extractData.first;
                String realFieldName = extractData.second;
                if (index != -1) {
                    String dataFieldName = realFieldName;
                    if (realFieldName.endsWith(headerSuffix)) {
                        dataFieldName = realFieldName.substring(0, realFieldName.length() - headerSuffix.length());
                    } else if (realFieldName.endsWith(footerSuffix)) {
                        dataFieldName = realFieldName.substring(0, realFieldName.length() - footerSuffix.length());
                    }
                    value = getCompositeFieldValue(dataFieldName, realFieldName, index);
                }
            }
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            if (value instanceof java.sql.Timestamp)
                value = DateConverter.stampToDate((java.sql.Timestamp) value);
            else
                value = DateConverter.sqlToDate((java.sql.Date) value);
        }

        if (value instanceof String) {
            value = BaseUtils.rtrim((String) value);
        }

        if(jrField.getDescription()!=null && Number.class.isAssignableFrom(jrField.getValueClass()) && jrField.getDescription().contains("@Z") && value == null) {
            value = jrField.getValueClass().cast(0);
        }

        if (value instanceof byte[]) {
            if (files != null) {
                ByteArray file = new ByteArray(((byte[])value));
                String fileName = files.get(file);
                if(fileName==null) {
                    fileName = "File " + (files.size()+1) + ".pdf";
                    files.put(file, fileName);
                }
                value = fileName;
            }
        }

        return value;
    }

    private Integer getObjectIdByFieldName(String fieldName) {
        if (fieldName != null && fieldName.endsWith(objectSuffix)) {
            String objectName = fieldName.substring(0, fieldName.length() - objectSuffix.length());
            return objects.get(objectName);
        }
        
        return null;
    }

    private Object getCompositeFieldValue(String dataFieldName, String realFieldName, int index) {
        List<Object> row = new ArrayList<>(Collections.nCopies(compositeFieldsObjects.get(dataFieldName).size(), null));

        Map<Integer, Integer> pos = new HashMap<>();
        int i = 0;
        for (Integer id : compositeFieldsObjects.get(dataFieldName)) {
            pos.put(id, i);
            ++i;
        }

        for (Map.Entry<Integer, Object> entry : currentKeyRow.entrySet()) {
            if (pos.containsKey(entry.getKey())) {
                row.set(pos.get(entry.getKey()), entry.getValue());
            }
        }
        List<Object> values = compositeColumnValues.get(dataFieldName).get(index);
        int j = 0;
        for (Integer id : compositeColumnObjects.get(dataFieldName)) {
            row.set(pos.get(id), values.get(j));
            ++j;
        }
        return compositeObjectValues.get(realFieldName).get(row);
    }

    public Object getKeyValueByIndex(int index) {
        return currentKeyRow.get(objects.get(objectNames.get(index)));
    }
    
    public int getObjectsCount() {
        return objectNames.size();
    }

    private Pair<Integer, String> extractFieldData(String id) {
        int markerPos = id.substring(0, id.length() - endIndexMarker.length()).lastIndexOf(beginIndexMarker);
        if (markerPos == -1) return new Pair<>(-1, "");
        String indexString = id.substring(markerPos + beginIndexMarker.length(), id.length() - endIndexMarker.length());
        String realFieldName = id.substring(0, markerPos);
        return new Pair<>(Integer.parseInt(indexString), realFieldName);
    }
}


