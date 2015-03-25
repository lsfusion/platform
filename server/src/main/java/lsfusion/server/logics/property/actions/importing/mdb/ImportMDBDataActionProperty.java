package lsfusion.server.logics.property.actions.importing.mdb;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportMDBDataActionProperty extends ImportDataActionProperty {
    public ImportMDBDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file) {

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) BaseUtils.deserializeCustomObject(file);
            List<List<String>> rowsList = new ArrayList<List<String>>();

            Map<String, Integer> fieldMapping = new HashMap<String, Integer>();
            int i = 0;
            for (Map.Entry<String, Object> entry : rows.get(0).entrySet()) {
                fieldMapping.put(entry.getKey(), i);
                i++;
            }

            for (Map<String, Object> row : rows) {
                List<String> entryList = new ArrayList<String>();
                for (Object entry : row.values())
                    entryList.add(entry == null ? null : String.valueOf(entry));
                rowsList.add(entryList);
            }

            List<Integer> sourceColumns = getSourceColumns(fieldMapping);

            return new ImportMDBIterator(rowsList, sourceColumns);
            
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }
}
