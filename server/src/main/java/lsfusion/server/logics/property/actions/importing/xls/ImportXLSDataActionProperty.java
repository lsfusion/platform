package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.Settings;
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
    private boolean sheetAll;
    
    public ImportXLSDataActionProperty(int paramsCount, List<String> ids, ImOrderSet<LCP> properties, List<Boolean> nulls, boolean sheetAll, BaseLogicsModule baseLM) {
        super(paramsCount, ids, properties, nulls, baseLM);
        this.sheetAll = sheetAll;
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
        if (file[0] == 80) {  //50 hex
            if (sheetAll) {
                return new ImportXLSXWorkbookIterator(file, getSourceColumns(XLSColumnsMapping), properties);
            } else {
                int minSize = Settings.get().getMinSizeForExcelStreamingReader();
                boolean hugeFile = minSize > 0 && file.length > minSize;
                if(hugeFile) {
                    return new ImportXLSXHugeSheetIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex);
                } else {
                    return new ImportXLSXSheetIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex);
                }
            }
        } else {
            if (sheetAll) {
                return new ImportXLSWorkbookIterator(file, getSourceColumns(XLSColumnsMapping), properties);
            } else {
                return new ImportXLSSheetIterator(file, getSourceColumns(XLSColumnsMapping), properties, sheetIndex);
            }
        }
    }
}
