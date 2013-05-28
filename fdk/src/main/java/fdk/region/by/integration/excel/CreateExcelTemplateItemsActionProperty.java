package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CreateExcelTemplateItemsActionProperty extends CreateExcelTemplateActionProperty {

    public CreateExcelTemplateItemsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile() throws IOException, WriteException {
        return createFile("importItemsTemplate",
                Arrays.asList("Код товара", "Код группы", "Наименование", "Ед.изм.", "Краткая ед.изм.",
                        "Код ед.изм.", "Название бренда", "Код бренда", "Страна", "Штрих-код", "Дата", "Весовой",
                        "Вес нетто", "Вес брутто", "Состав", "НДС, %", "Код посуды", "Цена посуды", "НДС посуды, %",
                        "Код нормы отходов", "Оптовая наценка", "Розничная наценка", "Кол-во в упаковке"),
                Arrays.asList(Arrays.asList("1111", "2222", "Товар 1", "Штука", "шт", "UOM1", "HugoBoss", "B1", "Беларусь",
                        "481011200174", "01.01.2012", "1", "0,1", "0,1", "100% cotton", "20", "", "", "", "RW1", "30", "20", "12")));
    }
}