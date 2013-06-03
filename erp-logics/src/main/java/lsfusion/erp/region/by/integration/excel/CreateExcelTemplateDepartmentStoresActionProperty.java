package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateDepartmentStoresActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateDepartmentStoresActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importDepartmentStoresTemplate",
                Arrays.asList("Код отдела магазина", "Имя отдела", "Код магазина"),
                Arrays.asList(Arrays.asList("678", "Продовольственный", "12345")));
    }
}