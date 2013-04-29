package fdk.region.by.integration.excel;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import platform.base.IOUtils;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExportExcelActionProperty extends ScriptingActionProperty {

    public abstract Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException;

    public static Map<String, byte[]> createFile(String fileName, List<String> columns, List<List<String>> rows) throws IOException, WriteException {
        File file = File.createTempFile(fileName, ".xls");
        WritableWorkbook workbook = Workbook.createWorkbook(file);
        WritableSheet sheet = workbook.createSheet("List 1", 0);
        CellView cv = new CellView();
        cv.setAutosize(true);

        for (int i = 0; i < columns.size(); i++) {
            sheet.addCell(new jxl.write.Label(i, 0, columns.get(i)));
            sheet.setColumnView(i, cv);
        }

        for (int j = 0; j < rows.size(); j++)
            for (int i = 0; i < rows.get(j).size(); i++) {
                sheet.addCell(new jxl.write.Label(i, j + 1, rows.get(j).get(i)));
            }

        workbook.write();
        workbook.close();

        Map<String, byte[]> result = new HashMap<String, byte[]>();
        result.put(fileName + ".xls", IOUtils.getFileBytes(file));

        return result;
    }

    public ExportExcelActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            context.delayUserInterfaction(new ExportFileClientAction(createFile(context)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }

    protected String trimNotNull(Object value) {
        if (value == null)
            return "";
        if (value instanceof String)
            return ((String) value).trim();
        else
            return String.valueOf(value);
    }
}