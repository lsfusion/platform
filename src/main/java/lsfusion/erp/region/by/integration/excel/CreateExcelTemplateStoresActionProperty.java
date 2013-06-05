package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateStoresActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateStoresActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importStoresTemplate",
                Arrays.asList("Код магазина", "Имя", "Адрес магазина", "Код организации"),
                Arrays.asList(Arrays.asList("12345", "Магазин №1", "ЛИДА,СОВЕТСКАЯ, 24,231300", "ПС0010325")));
    }
}