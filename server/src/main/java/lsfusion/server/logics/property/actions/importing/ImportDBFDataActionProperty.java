package lsfusion.server.logics.property.actions.importing;

import com.google.common.io.Files;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportDBFDataActionProperty extends ImportDataActionProperty {
    public ImportDBFDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public List<List<String>> getTable(byte[] file) throws IOException, ParseException, xBaseJException {
        File tmpFile = null;
        DBF importFile = null;
        try {
            tmpFile = File.createTempFile("importDBF", ".dbf");
            Files.write(file, tmpFile);
            importFile = new DBF(tmpFile.getAbsolutePath());
            int recordCount = importFile.getRecordCount();

            List<List<String>> result = new ArrayList<List<String>>();
            
            Map<String, Integer> fieldMapping = new HashMap<String, Integer>();
            for (int i = 1; i <= importFile.getFieldCount(); i++) {
                fieldMapping.put(importFile.getField(i).getName().toLowerCase(), i);    
            }


            List<Integer> sourceColumns = getSourceColumns(fieldMapping);
            for (int i = 0; i < recordCount; i++) {
                importFile.read();
                List<String> listRow = new ArrayList<String>();
                for (Integer column : sourceColumns) {
                    ValueClass valueClass = properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                    boolean isDate = valueClass instanceof DateClass;
                    if (column <= importFile.getFieldCount()) {
                        String value = new String(importFile.getField(column).getBytes(), "Cp866").trim();
                        if (isDate) {
                            value = DateFormat.getDateInstance().format(DateUtils.parseDate(value, new String[]{"yyyyMMdd", "dd.MM.yyyy"}));
                        }
                        listRow.add(value);
                    }
                }
                result.add(listRow);
            }

            importFile.close();

            return result;
        } finally {
            if (tmpFile != null)
                tmpFile.delete();
            if (importFile != null)
                importFile.close();
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }
}
