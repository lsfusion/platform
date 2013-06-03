package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateGroupItemsActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateGroupItemsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importGroupItemsTemplate",
                Arrays.asList("Код группы", "Наименование группы", "Код родительской группы"),
                Arrays.asList(
                        Arrays.asList("1111", "Группа 1", ""),
                        Arrays.asList("2222", "Группа 2", "1111")));
    }
}