package lsfusion.server.logics.property.actions.importing.jdbc;

import com.google.common.base.Throwables;
import com.sun.rowset.CachedRowSetImpl;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportJDBCDataActionProperty extends ImportDataActionProperty {
    public ImportJDBCDataActionProperty(List<String> ids, List<LCP> properties, BaseLogicsModule baseLM) {
        super(1, ids, properties, baseLM);
    }

    @Override
    public ImportIterator getIterator(byte[] file) {

        try {
            JDBCTable rs = JDBCTable.deserializeJDBC(file);
            Map<String, Integer> fieldMapping = rs.fields.mapOrderValues(new GetIndex<Integer>() {
                public Integer getMapValue(int i) {
                    return i+1; // sourceColumns - one-base
                }}).toJavaMap();
            List<Integer> sourceColumns = getSourceColumns(fieldMapping);
            
            return new ImportJDBCIterator(rs, sourceColumns, properties);
            
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }
}
