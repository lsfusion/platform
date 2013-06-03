package lsfusion.erp.region.by.integration.excel;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CreateExcelTemplateActionProperty extends ScriptingActionProperty {

    public abstract Map<String, byte[]> createFile() throws IOException, WriteException;

    public static Map<String, byte[]> createFile(String fileName, List<String> columns, List<List<String>> defaultRows) throws IOException, WriteException {
        File file = File.createTempFile(fileName, ".xls");
        WritableWorkbook workbook = Workbook.createWorkbook(file);
        WritableSheet sheet = workbook.createSheet("List 1", 0);
        CellView cv = new CellView();
        cv.setAutosize(true);

        for (int i = 0; i < columns.size(); i++) {
            sheet.addCell(new jxl.write.Label(i, 0, columns.get(i)));
            sheet.setColumnView(i, cv);
        }

        for (int j = 0; j < defaultRows.size(); j++)
            for (int i = 0; i < defaultRows.get(j).size(); i++) {
                sheet.addCell(new jxl.write.Label(i, j + 1, defaultRows.get(j).get(i)));
            }

        workbook.write();
        workbook.close();

        Map<String, byte[]> result = new HashMap<String, byte[]>();
        result.put(fileName + ".xls", IOUtils.getFileBytes(file));

        return result;
    }

    public CreateExcelTemplateActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            context.delayUserInterfaction(new ExportFileClientAction(createFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }
}