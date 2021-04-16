package lsfusion.server.logics.classes.data.utils.excel;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.nvl;

public class ColumnsCountExcelAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface sheetInterface;

    public ColumnsCountExcelAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        sheetInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue fileObject = context.getKeyValue(fileInterface);
        Integer sheetIndex = nvl((Integer) context.getKeyValue(sheetInterface).getValue(), 0);

        int lastColumn = -1;
        if (fileObject instanceof DataObject) {
            try {
                RawFileData file = (RawFileData) fileObject.getValue();
                String extension = ((StaticFormatFileClass) ((DataObject) fileObject).objectClass.getType()).getOpenExtension(file);
                if (extension != null) {
                    Workbook wb = extension.equals("xls") ? new HSSFWorkbook(file.getInputStream()) : extension.equals("xlsx") ? new XSSFWorkbook(file.getInputStream()) : null;
                    if (wb != null) {
                        Iterator<Row> rowIterator = wb.getSheetAt(sheetIndex).rowIterator();
                        while (rowIterator.hasNext()) {
                            lastColumn = Math.max(lastColumn, rowIterator.next().getLastCellNum());
                        }
                    }
                }
                findProperty("columnsCount[]").change(lastColumn, context);
            } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}