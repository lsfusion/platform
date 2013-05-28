package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateUserInvoicesActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateUserInvoicesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importUserInvoicesTemplate",
                Arrays.asList("Серия", "Номер", "Дата", "Код товара", "Кол-во", "Поставщик", "Склад покупателя",
                        "Склад поставщика", "Цена", "Цена услуг", "Розничная цена", "Розничная надбавка", "Сертификат"),
                Arrays.asList(Arrays.asList("AA", "12345678", "12.12.2012", "1111", "150", "ПС0010325", "4444", "3333",
                        "5000", "300", "7000", "30", "№123456789")));
    }
}