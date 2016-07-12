package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.commons.lang3.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportDBFIterator extends ImportIterator {
    DBF dbf;
    List<Integer> sourceColumns;
    List<LCP> properties;
    
    public ImportDBFIterator(DBF dbf, List<Integer> sourceColumns, List<LCP> properties) {
        this.dbf = dbf;
        this.sourceColumns = sourceColumns;
        this.properties = properties;
    }

    @Override
    public List<String> nextRow() {
        try {
            dbf.read();
            List<String> listRow = new ArrayList<String>();
            for (Integer column : sourceColumns) {
                ValueClass valueClass = properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                boolean isDate = valueClass instanceof DateClass;
                if (column <= dbf.getFieldCount()) {
                    String value = new String(dbf.getField(column).getBytes(), "Cp866").trim();
                    if (isDate) {
                        value = DateFormat.getDateInstance().format(DateUtils.parseDate(value, new String[]{"yyyyMMdd", "dd.MM.yyyy"}));
                    }
                    listRow.add(value);
                }
            }
            return listRow;
        } catch (xBaseJException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            return null;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    protected void release() {
        try {
            if (dbf != null)
                dbf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
