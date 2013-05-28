package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateLegalEntitiesActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateLegalEntitiesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importLegalEntitiesTemplate",
                Arrays.asList("Код организации", "Название", "Адрес", "Телефон", "E-mail", "Рассчётный счёт",
                        "Код банка", "Страна", "Является поставщиком", "Является компанией", "Является покупателем", "УНП", "ОКПО"),
                Arrays.asList(
                        Arrays.asList("ПС0010325", "Добрый день ООО", "ЛИДА,СОВЕТСКАЯ, 24,231300",
                                "1234567", "xxx@tut.by", "1234567890123", "123456789", "Беларусь",
                                "1", "0", "0", "123456789", "123456"),
                        Arrays.asList("ПС0010326", "Добрый вечер ОАО", "Новогрудок, Ленина, 23,231300",
                                "7654321", "yyy@tut.by", "1234567890124", "123456789", "Беларусь",
                                "0", "1", "1", "123456788", "123455")));
    }
}