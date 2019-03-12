package jasperapi;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static jasperapi.ReportConstants.objectSuffix;

public class ClientKeyData {
    private final Map<String, Integer> objects = new HashMap<>(); // for getting object values by name

    private final List<Map<Integer, Object>> keyRows = new ArrayList<>();
    
    public ClientKeyData(DataInputStream inStream, Map<Map<Integer, Object>, Map<Integer, Object>> cache) throws IOException {

        int objectCnt = inStream.readInt();
        List<Integer> objectList = new ArrayList<>();
        for (int i = 0; i < objectCnt; i++) {
            String name = inStream.readUTF();
            int id = inStream.readInt();
            objects.put(name, id);
            objectList.add(id);
        }

        int rowCnt = inStream.readInt();
        for (int i = 0; i < rowCnt; i++) {
            Map<Integer, Object> objectValues = new HashMap<>();
            for (Integer object : objectList)
                objectValues.put(object, BaseUtils.deserializeObject(inStream));                
            keyRows.add(cacheRow(objectValues, cache));
        }
    }

    // optimization to reduce memory usage
    public static Map<Integer, Object> cacheRow(Map<Integer, Object> row, Map<Map<Integer, Object>, Map<Integer, Object>> cache) {
        Map<Integer, Object> cachedRow = cache.get(row);
        if(cachedRow == null) {
            cache.put(row, row);
            cachedRow = row;
        }
        return cachedRow;
    }

    public ListIterator<Map<Integer, Object>> listIterator() {
        return keyRows.listIterator();
    }
    
    public Integer getColumnsCount(String fieldName, Result<Integer> minColumnsCount) {
        if(fieldName.endsWith(objectSuffix)) {
            if(minColumnsCount != null)
                minColumnsCount.set(1);
            return 1;
        }
        return null;
    }

    public Object getFieldValue(Map<Integer, Object> currentKeyRow, String fieldName) {
        if (fieldName.endsWith(objectSuffix)) {
            String objectName = fieldName.substring(0, fieldName.length() - objectSuffix.length());
            return currentKeyRow.get(objects.get(objectName));            
        }
        
        return null;
    }
    
    public boolean keyRowsIsEmpty() {
        return keyRows.isEmpty();
    }
    
    public Map<Integer, Object> getKeyRowsFirst() {
        return keyRows.get(0);
    }
}


