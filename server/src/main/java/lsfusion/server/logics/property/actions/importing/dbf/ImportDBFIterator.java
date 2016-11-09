package lsfusion.server.logics.property.actions.importing.dbf;

import com.google.common.base.Throwables;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

import java.io.IOException;
import java.util.*;

class ImportDBFIterator extends ImportIterator {
    private DbfReader reader;
    private List<Integer> sourceColumns;
    private String wheres;
    
    ImportDBFIterator(DbfReader reader, List<Integer> sourceColumns, String wheres) {
        this.reader = reader;
        this.sourceColumns = sourceColumns;
        this.wheres = wheres;
    }

    @Override
    public Object nextRow(List<List<String>> wheresList) {
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
            if (!ignoreRow(record, wheresList) && !record.isDeleted()) {
                for (Integer column : sourceColumns) {
                    //Пока charset захардкожена. Если потребуется другая, то добавить в язык как для CSV
                    listRow.add(record.getString(fieldMapping.get(column), "cp1251"));

                }
                return listRow;
            } else return "ignored"; //чтобы отличить пропускаемый ряд от null - конца чтения

        } catch (IOException e) {
            return null;
        }
    }

    private boolean ignoreRow(DbfRecord record, List<List<String>> wheresList) {
        boolean ignoreRow = false;
        for (List<String> where : wheresList) {
            String condition = where.get(0);
            boolean and = condition != null && condition.equals("AND");
            boolean or = condition != null && condition.equals("OR");
            boolean not = where.get(1) != null;
            String field = where.get(2) != null ? where.get(2).toUpperCase() : null;
            String sign = where.get(3);
            String value = where.get(4);

            if (record.getField(field) == null) {
                throw Throwables.propagate(new RuntimeException("Incorrect WHERE in IMPORT DBF: no such column"));
            }
            Object fieldValue = record.getString(field, "cp1251");
            boolean conditionResult = fieldValue == null || ignoreRowStringCondition(not, fieldValue, sign, value);
            ignoreRow = and ? (ignoreRow | conditionResult) : or ? (ignoreRow & conditionResult) : conditionResult;
        }
        return ignoreRow;
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
