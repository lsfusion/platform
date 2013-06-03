package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CreateExcelTemplateAllActionProperty extends ScriptingActionProperty {

    public CreateExcelTemplateAllActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            Map<String, byte[]> files = new HashMap<String, byte[]>();
            files.putAll(new CreateExcelTemplateItemsActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateGroupItemsActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateBanksActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateLegalEntitiesActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateStoresActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateDepartmentStoresActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateWarehousesActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateContractsActionProperty(LM).createFile());
            files.putAll(new CreateExcelTemplateUserInvoicesActionProperty(LM).createFile());

            context.delayUserInterfaction(new ExportFileClientAction(files));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RowsExceededException e) {
            throw new RuntimeException(e);
        } catch (WriteException e) {
            throw new RuntimeException(e);
        }
    }
}