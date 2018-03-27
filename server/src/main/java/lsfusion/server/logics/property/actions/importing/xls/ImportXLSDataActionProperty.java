package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.property.actions.importing.IncorrectFileException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ImportXLSDataActionProperty extends ImportDataActionProperty {
    protected Integer sheetIndex;
    
    public ImportXLSDataActionProperty(int paramsCount, List<String> ids, ImOrderSet<LCP> properties, BaseLogicsModule baseLM) {
        super(paramsCount, ids, properties, baseLM);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sheet = null;
        if (context.getDataKeys().size() == 2) {
            sheet = context.getDataKeys().getValue(1);
            assert sheet.getType() instanceof IntegerClass;
        }
        if (sheet != null) {
            sheetIndex = (Integer) sheet.object;
        }
        
        return super.aspectExecute(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) throws IOException, IncorrectFileException {
        return file[0] == 80 ?  //50 hex
                new ImportXLSXIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex) :
                new ImportXLSIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex);
    }
}
