package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ImportXLSDataActionProperty extends ImportDataActionProperty {
    protected Integer sheetIndex;
    
    public ImportXLSDataActionProperty(ValueClass[] valueClasses, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClasses, LM, ids, properties);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sheet = null;
        if (context.getDataKeys().size() == 2) {
            sheet = context.getDataKeys().getValue(1);
            assert sheet.getType() instanceof IntegerClass;
        }
        if (sheet != null) {
            sheetIndex = (Integer) sheet.object;
        }
        
        super.executeCustom(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException {
        return new ImportXLSIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex);
    }
}
