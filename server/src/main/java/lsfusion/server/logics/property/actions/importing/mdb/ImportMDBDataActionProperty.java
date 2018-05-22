package lsfusion.server.logics.property.actions.importing.mdb;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.IOException;
import java.util.*;

public class ImportMDBDataActionProperty extends ImportDataActionProperty {
    public ImportMDBDataActionProperty(List<String> ids, ImOrderSet<LCP> properties, List<Boolean> nulls, BaseLogicsModule baseLM) {
        super(1, ids, properties, nulls, baseLM);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) {
        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) BaseUtils.deserializeCustomObject(file);
            return new ImportMDBIterator(getRowsList(rows), getSourceColumns(getFieldMapping(rows)));
        } catch (IOException | ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Map<String, Integer> getFieldMapping(List<Map<String, Object>> rows) {
        Map<String, Integer> fieldMapping = new HashMap<>();
        int i = 0;
        if(!rows.isEmpty()) {
            for (Map.Entry<String, Object> entry : rows.get(0).entrySet()) {
                fieldMapping.put(entry.getKey(), i);
                i++;
            }
        }
        return fieldMapping;
    }

    public static List<List<String>> getRowsList(List<Map<String, Object>> rows) {
        List<List<String>> rowsList = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> entryList = new ArrayList<>();
            for (Object entry : row.values()) {
                if(entry instanceof Date)
                    entryList.add(DateTimeClass.getDateTimeFormat().format(entry));
                else
                    entryList.add(entry == null ? null : String.valueOf(entry));
            }
            rowsList.add(entryList);
        }
        return rowsList;
    }
}