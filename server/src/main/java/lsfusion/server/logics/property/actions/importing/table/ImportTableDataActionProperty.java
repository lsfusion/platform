package lsfusion.server.logics.property.actions.importing.table;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ImportTableDataActionProperty extends ImportDataActionProperty {
    public ImportTableDataActionProperty(List<String> ids, List<LCP> properties, BaseLogicsModule baseLM) {
        super(1, ids, properties, baseLM);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) {

        try {
            JDBCTable rs = JDBCTable.deserializeJDBC(file);
            return new ImportTableIterator(rs, getSourceColumns(getFieldMapping(rs)), properties);
            
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }

    public static Map<String, Integer> getFieldMapping(JDBCTable rs) {
        return rs.fields.mapOrderValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i + 1; // sourceColumns - one-base
            }
        }).toJavaMap();
    }
}