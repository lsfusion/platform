package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

// what the cells of the first sheet of an Excel file show - each value, a formula's computed, formatted by its cell's
// format, as Excel formats it, in the US locale -, written into excelText by the cell's row and column, both from 0; a
// cell that shows nothing is left out
public class ReadExcelCellsAction extends InternalAction {

    public ReadExcelCellsAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FileData file = (FileData) getParam(0, context);
        try (Workbook workbook = WorkbookFactory.create(file.getRawFile().getInputStream())) {
            LP<?> text = findProperty("excelText[INTEGER,INTEGER]");
            DataFormatter formatter = new DataFormatter(Locale.US);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (Row row : workbook.getSheetAt(0))
                for (Cell cell : row) {
                    String shown = formatter.formatCellValue(cell, evaluator);
                    if (!shown.isEmpty())
                        text.change(shown, context, new DataObject(row.getRowNum()), new DataObject(cell.getColumnIndex()));
                }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
