package lsfusion.server.logics.property.actions.importing.jdbc;

import com.sun.rowset.CachedRowSetImpl;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImportJDBCIterator extends ImportIterator {
    CachedRowSetImpl rs;
    List<Integer> sourceColumns;
    private final List<LCP> properties;

    public ImportJDBCIterator(CachedRowSetImpl rs, List<Integer> sourceColumns, List<LCP> properties) {
        this.rs = rs;
        this.sourceColumns = sourceColumns;
        this.properties = properties;
    }

    @Override
    public List<String> nextRow() {
        try {
            if (rs.next()) {
                List<String> listRow = new ArrayList<>();
                for (Integer column : sourceColumns) {
                    ValueClass valueClass = properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                    if(valueClass instanceof DateClass)
                        listRow.add(DateClass.getDateFormat().format(rs.getDate(column)));
                    else
                        listRow.add(rs.getString(column));
                }
                return listRow;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    protected void release() {
        if(rs != null)
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}
