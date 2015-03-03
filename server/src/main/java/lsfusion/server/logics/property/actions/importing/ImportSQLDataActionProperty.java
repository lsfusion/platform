package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import com.sun.rowset.CachedRowSetImpl;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportSQLDataActionProperty extends ImportDataActionProperty {
    public ImportSQLDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public List<List<String>> getTable(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException {
        List<List<String>> result = new ArrayList<List<String>>();

        try {
            CachedRowSetImpl rs = BaseUtils.deserializeResultSet(file);
            ResultSetMetaData rsmd = rs.getMetaData();

            Map<String, Integer> fieldMapping = new HashMap<String, Integer>();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                fieldMapping.put(rsmd.getColumnName(i), i);
            }

            List<Integer> sourceColumns = getSourceColumns(fieldMapping);

            while (rs.next()) {
                List<String> listRow = new ArrayList<String>();
                for (Integer column : sourceColumns) {
                    listRow.add(rs.getString(column));
                }
                result.add(listRow);
            }

        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        return result;
    }
}
