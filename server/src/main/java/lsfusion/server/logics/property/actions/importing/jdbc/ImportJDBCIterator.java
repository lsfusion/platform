package lsfusion.server.logics.property.actions.importing.jdbc;

import com.sun.rowset.CachedRowSetImpl;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImportJDBCIterator extends ImportIterator {
    CachedRowSetImpl rs;
    List<Integer> sourceColumns;
    
    public ImportJDBCIterator(CachedRowSetImpl rs, List<Integer> sourceColumns) {
        this.rs = rs;
        this.sourceColumns = sourceColumns;
    }

    @Override
    public List<String> nextRow() {
        try {
            if (rs.next()) {
                List<String> listRow = new ArrayList<String>();
                for (Integer column : sourceColumns) {
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
