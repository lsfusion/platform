package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImportDataActionProperty extends ScriptingActionProperty {
    private final ImportSourceFormat format;
    private final List<LCP> properties;

    public ImportDataActionProperty(ValueClass valueClass, ImportSourceFormat format, ScriptingLogicsModule LM, List<LCP> properties) {
        super(LM, valueClass);
        this.format = format;
        this.properties = properties;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof FileClass;
        
        Object file = value.object;
        if (file instanceof byte[]) {
            try {
                List<List<String>> table = null;
                if (format == ImportSourceFormat.XLS) {
                    table = getXLSTable((byte[]) file);
                }
                
                if (table != null) {
                    for (List<String> row : table) {
                        DataObject rowKey = new DataObject(table.indexOf(row), IntegerClass.instance);
                        LM.baseLM.imported.change(true, context, rowKey);
                        for (int i = 0; i < Math.min(properties.size(), row.size()); i++) {
                            LCP property = properties.get(i);
                            Type type = property.property.getType();
                            property.change(type.parseString(row.get(i)), context, rowKey);
                        }
                    }
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }   
    }
    
    private List<List<String>> getXLSTable(byte[] file) throws IOException, BiffException {
        Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = wb.getSheet(0);

        List<List<String>> result = new ArrayList<List<String>>();
        
        for (int i = 0; i < sheet.getRows(); i++) {
            Cell[] row = sheet.getRow(i);
            List<String> listRow = new ArrayList<String>();
            for (int j = 0; j < Math.min(properties.size(), row.length); j++) {
                listRow.add(row[j].getContents());
            }
            result.add(listRow);
        }
        
        return result;
    } 
}
