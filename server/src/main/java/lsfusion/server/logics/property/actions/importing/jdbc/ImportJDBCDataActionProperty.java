package lsfusion.server.logics.property.actions.importing.jdbc;

import com.google.common.base.Throwables;
import com.sun.rowset.CachedRowSetImpl;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportJDBCDataActionProperty extends ImportDataActionProperty {
    public ImportJDBCDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(new ValueClass[] {valueClass}, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file, Integer sheetIndex) {

        try {
            CachedRowSetImpl rs = BaseUtils.deserializeResultSet(file);
            ResultSetMetaData rsmd = rs.getMetaData();

            Map<String, Integer> fieldMapping = new HashMap<String, Integer>();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                fieldMapping.put(rsmd.getColumnName(i), i);
            }
            List<Integer> sourceColumns = getSourceColumns(fieldMapping);
            
            return new ImportJDBCIterator(rs, sourceColumns);
            
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }
}
