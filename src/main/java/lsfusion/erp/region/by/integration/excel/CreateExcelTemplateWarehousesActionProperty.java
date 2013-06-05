package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateWarehousesActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateWarehousesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importWarehousesTemplate",
                Arrays.asList("Код склада", "Имя склада", "Код группы складов", "Группа складов",
                        "Код организации", "Адрес склада"),
                Arrays.asList(
                        Arrays.asList("3333", "Основной склад", "123", "Собственные склады",
                                "ПС0010325", "ЛИДА,СОВЕТСКАЯ, 24,231300"),
                        Arrays.asList("4444", "Другой склад", "124", "Собственные склады",
                                "ПС0010326", "Новогрудок, Ленина, 23,231300")));
    }
}