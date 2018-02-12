package lsfusion.server.logics.property.actions.importing.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.ArrayList;
import java.util.List;

public class ImportTableIterator extends ImportIterator {
    private final JDBCTable rs;
    private int i=0;
    private final List<Integer> sourceColumns;
    private final List<LCP> properties;

    public ImportTableIterator(JDBCTable rs, List<Integer> sourceColumns, List<LCP> properties) {
        this.rs = rs;
        this.sourceColumns = sourceColumns;
        this.properties = properties;
    }

    @Override
    public List<String> nextRow() {
        if(i >= rs.set.size())
            return null;

        ImMap<String, Object> row = rs.set.get(i++);
        
        List<String> listRow = new ArrayList<>();
        for (Integer column : sourceColumns) {
            String columnName = rs.fields.get(column-1);
            Type columnType = rs.fieldTypes.get(columnName);
            Object columnValue = row.get(columnName);
            
            DataClass propertyClass = (DataClass)properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
            listRow.add(propertyClass.formatString(propertyClass.readCast(columnValue, (DataClass)columnType))); // может быть byte[], но тогда на parseString unsupported все равно свалится
//            listRow.add((String)columnType.format(columnValue)); // по идее как сверху надежнее, так иначе parseString и за конвертацию будет отвечать
        }
        return listRow;
    }

    @Override
    protected void release() {
    }
}
