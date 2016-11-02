package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.server.logics.property.actions.importing.ImportIterator;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ImportDBFIterator extends ImportIterator {
    private DbfReader reader;
    private List<Integer> sourceColumns;
    
    ImportDBFIterator(DbfReader reader, List<Integer> sourceColumns) {
        this.reader = reader;
        this.sourceColumns = sourceColumns;
    }

    @Override
    public List<String> nextRow() {
        try {
            DbfRecord record = reader.read();
            if(record == null)
                return null;
            Map<Integer, String> fieldMapping = new HashMap<>();
            int i = 1;
            for(DbfField field : record.getFields()) {
                fieldMapping.put(i, field.getName());
                i++;
            }

            List<String> listRow = new ArrayList<>();
            for (Integer column : sourceColumns) {
                //Пока charset захардкожена. Если потребуется другая, то добавить в язык как для CSV
                listRow.add(record.getString(fieldMapping.get(column), "cp1251"));
            }
            return listRow;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void release() {
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
