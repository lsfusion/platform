package roman.actions;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import roman.RomanBusinessLogics;

import java.io.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class ExportDeclarationActionProperty extends ScriptingActionProperty {
    private RomanBusinessLogics BL;
    private ScriptingLogicsModule romanRB;
    private final ClassPropertyInterface declarationInterface;
    String row;

    public ExportDeclarationActionProperty(RomanBusinessLogics BL) {
        super(BL, new ValueClass[]{BL.RomanRB.getClassByName("declaration")});
        this.BL = BL;
        this.romanRB = BL.RomanRB;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void execute(ExecutionContext context) {
        try {
            List<String> exportProperties = BaseUtils.toList("numberGroupDeclaration", "nameBrandGroupDeclaration",
                    "nameCategoryGroupDeclaration", "sidGenderGroupDeclaration", "nameTypeFabricGroupDeclaration",
                    "sidArticleGroupDeclaration", "sidCustomCategory10GroupDeclaration", "mainCompositionGroupDeclaration",
                    "sidCountryGroupDeclaration", "sidOrigin2CountryGroupDeclaration", "quantityGroupDeclaration", "sidUnitOfMeasureGroupDeclaration",
                    "nameUnitOfMeasureGroupDeclaration", "sumGroupDeclaration", "netWeightGroupDeclaration",
                    "grossWeightGroupDeclaration");
            List<String> exportTitles = BaseUtils.toList("Порядковый номер декларируемого товара", "Наименование товара", "Вес брутто",
                    "Вес нетто", "Вес нетто без упаковки", "Фактурная стоимость товара", "Таможенная стоимость",
                    "Статистическая стоимость", "Код товара по ТН ВЭД ТС", "Запреты и ограничения", "Интеллектуальная собственность",
                    "Цифровой код страны происхождения товара", "Буквенный код страны происхождения товара",
                    "Код метода определения таможенной стоимости", "Название географического пункта",
                    "Код условий поставки по Инкотермс", "Код вида поставки товаров", "Количество мест",
                    "Вид грузовых мест","Код валюты квоты",	"Остаток квоты в валюте", "Остаток квоты в единице измерения",
                    "Код единицы измерения квоты", "Наименование единицы измерения квоты",
                    "Количество товара в специфических единицах измерения", "Количество подакцизного товара",
                    "Код специфических единиц измерения", "Краткое наименование специфических единиц измерения",
                    "Код единицы измерения подакцизного товара", "Наименование единицы измерения подакцизного товара",
                    "Количество товара в дополнительных единицах измерения", "Код дополнительной единицы измерения",
                    "Наименование дополнительной единицы измерения",
                    "Корректировки таможенной стоимости", "Количество акцизных марок", "Код предшествующей таможенной процедуры",
                    "Преференция код 1", "Преференция код 2", "Преференция код 3", "Преференция код 4",
                    "Код особенности перемещения товаров", "Запрашиваемый срок переработки", "Номер документа переработки",
                    "Дата документа переработки", "Место проведения операций переработки", "Количество товара переработки",
                    "Код единицы измерения количества товара переработки", "Краткое наименование единицы измерения количества товара переработки",
                    "Почтовый индекс организации осуществлявшей переработку", "Код страны переработки", "Наименование страны переработки",
                    "Наименование региона переработки",	"Наименование населенного пункта переработки",	"Улица и дом переработки",
                    "Наименование лица (отправителя) переработки",	"УНП лица (отправителя) переработки",
                    "Почтовый индекс лица (отправителя) переработки", "Наименование региона лица (отправителя) переработки",
                    "Населенный пункт лица (отправителя) переработки", "Улица и дом лица (отправителя) переработки",
                    "Код страны регистрации лица (отправителя) переработки", "Название страны регистрации лица (отправителя) переработки",
                    "Номер документа, удостоверяющего личность физического лица  переработки",	"Идентификационный номер физического лица переработки",
                    "Дата выдачи документа, удостоверяющего личность физического лица переработки",
                    "Код документа, удостоверяющего личность физического лица переработки",
                    "Наименование изготовителя", "Товарный знак", "Марка товара", "Модель товара", "Артикул товара",
                    "Стандарт (ГОСТ, ОСТ, СПП, СТО, ТУ)", "Сорт (группа сортов)", "Дата выпуска", "Количество товара",
                    "Краткое наименование единицы измерения", "Код единицы измерения", "Группа товаров");

            DataObject declarationObject = context.getKeyValue(declarationInterface);

            Map<String, byte[]> files = new HashMap<String, byte[]>();
            File f = File.createTempFile("temp", ".csv");
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(f), "windows-1251"));

            LP isGroupDeclaration = BL.LM.is(romanRB.getClassByName("groupDeclaration"));
            Map<Object, KeyExpr> keys = isGroupDeclaration.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);
            for (String propertySID : exportProperties)
                query.properties.put(propertySID, romanRB.getLPByName(propertySID).getExpr(context.getModifier(), key));
            query.and(isGroupDeclaration.getExpr(key).getWhere());
            query.and(romanRB.getLPByName("declarationGroupDeclaration").getExpr(context.getModifier(), key).compare(declarationObject.getExpr(), Compare.EQUALS));
            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(context.getSession().sql);

            row = "";
            for (String title : exportTitles)
                addStringCellToRow(title, ";");
            writer.println(row);

            TreeMap<Integer, Map<String, Object>> sortedRows = new TreeMap<Integer, Map<String, Object>>();

            for (Map<Object, Object> values : result.values()) {

                Map<String, Object> row = new HashMap<String, Object>();
                for (String propertySID : exportProperties)
                    row.put(propertySID, values.get(propertySID));
                sortedRows.put((Integer) values.get("numberGroupDeclaration"), row);
            }

            for (Map.Entry<Integer, Map<String, Object>> entry : sortedRows.entrySet()) {
                row = "";
                Map<String, Object> values = entry.getValue();
                addStringCellToRow(entry.getKey(), ";"); //numberGroupDeclaration

                addPartStringCellToRow(values.get("nameCategoryGroupDeclaration"), null, " ", false);
                addPartStringCellToRow(values.get("sidGenderGroupDeclaration"), null, ",", false);
                addPartStringCellToRow(values.get("nameTypeFabricGroupDeclaration"), null, ",", false);
                addPartStringCellToRow(values.get("nameBrandGroupDeclaration"), "Торговая марка ", ",", false);
                addPartStringCellToRow(values.get("mainCompositionGroupDeclaration"), " Состав:", ";", true);

                addDoubleCellToRow(values.get("grossWeightGroupDeclaration"), ";", 3);
                addDoubleCellToRow(values.get("netWeightGroupDeclaration"), ";", 3);
                addStringCellToRow(null, ";"); //Вес нетто без упаковки
                addDoubleCellToRow(values.get("sumGroupDeclaration"), ";", 7);
                addStringCellToRow(null, ";"); //Таможенная стоимость
                addStringCellToRow(null, ";"); //Статистическая стоимость
                addStringCellToRow(values.get("sidCustomCategory10GroupDeclaration"), ";");
                addStringCellToRow(null, ";"); //Запреты и ограничения
                addStringCellToRow(null, ";"); //Интеллектуальная собственность
                addStringCellToRow(values.get("sidCountryGroupDeclaration"), ";");
                addStringCellToRow(values.get("sidOrigin2CountryGroupDeclaration"), ";");
                addStringCellToRow(null, ";"); //Код метода определения таможенной стоимости
                addStringCellToRow(null, ";"); //Название географического пункта
                addStringCellToRow(null, ";"); //Код условий поставки по Инкотермс
                addStringCellToRow(null, ";"); //Код вида поставки товаров
                addStringCellToRow(null, ";"); //Количество мест
                addStringCellToRow(null, ";"); //Вид грузовых мест
                addStringCellToRow(null, ";"); //Код валюты квоты
                addStringCellToRow(null, ";"); //Остаток квоты в валюте
                addStringCellToRow(null, ";"); //Остаток квоты в единице измерения
                addStringCellToRow(null, ";"); //Код единицы измерения квоты
                addStringCellToRow(null, ";"); //Наименование единицы измерения квоты
                addDoubleCellToRow(values.get("netWeightGroupDeclaration"), ";", 3); //Количество товара в специфических единицах измерения
                addStringCellToRow(null, ";"); //Количество подакцизного товара
                addStringCellToRow(null, ";"); //Код специфических единиц измерения
                addStringCellToRow(null, ";"); //Краткое наименование специфических единиц измерения
                addStringCellToRow(null, ";"); //Код единицы измерения подакцизного товара
                addStringCellToRow(null, ";"); //Наименование единицы измерения подакцизного товара
                addDoubleCellToRow(values.get("quantityGroupDeclaration"), ";", 0);
                addStringCellToRow(values.get("sidUnitOfMeasureGroupDeclaration"), ";");
                addStringCellToRow(values.get("nameUnitOfMeasureGroupDeclaration"), ";");

                addStringCellToRow(null, ";"); //Корректировки таможенной стоимости
                addStringCellToRow(null, ";"); //Количество акцизных марок
                addStringCellToRow(null, ";"); //Код предшествующей таможенной процедуры
                addStringCellToRow(null, ";"); //Преференция код 1
                addStringCellToRow(null, ";"); //Преференция код 2
                addStringCellToRow(null, ";"); //Преференция код 3
                addStringCellToRow(null, ";"); //Преференция код 4
                addStringCellToRow(null, ";"); //Код особенности перемещения товаров
                addStringCellToRow(null, ";"); //Запрашиваемый срок переработки
                addStringCellToRow(null, ";"); //Номер документа переработки
                addStringCellToRow(null, ";"); //Дата документа переработки
                addStringCellToRow(null, ";"); //Место проведения операций переработки
                addStringCellToRow(null, ";"); //Количество товара переработки
                addStringCellToRow(null, ";"); //Код единицы измерения количества товара переработки
                addStringCellToRow(null, ";"); //Краткое наименование единицы измерения количества товара переработки
                addStringCellToRow(null, ";"); //Почтовый индекс организации осуществлявшей переработку
                addStringCellToRow(null, ";"); //Код страны переработки
                addStringCellToRow(null, ";"); //Наименование страны переработки
                addStringCellToRow(null, ";"); //Наименование региона переработки
                addStringCellToRow(null, ";"); //Наименование населенного пункта переработки
                addStringCellToRow(null, ";"); //Улица и дом переработки
                addStringCellToRow(null, ";"); //Наименование лица (отправителя) переработки
                addStringCellToRow(null, ";"); //УНП лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Почтовый индекс лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Наименование региона лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Населенный пункт лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Улица и дом лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Код страны регистрации лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Название страны регистрации лица (отправителя) переработки
                addStringCellToRow(null, ";"); //Номер документа, удостоверяющего личность физического лица  переработки
                addStringCellToRow(null, ";"); //Идентификационный номер физического лица переработки
                addStringCellToRow(null, ";"); //Дата выдачи документа, удостоверяющего личность физического лица переработки
                addStringCellToRow(null, ";"); //Код документа, удостоверяющего личность физического лица переработки

                addStringCellToRow(null, ";"); //Наименование изготовителя
                addStringCellToRow(null, ";"); //Товарный знак
                addStringCellToRow(values.get("nameBrandGroupDeclaration"), ";"); //Марка товара
                addStringCellToRow(null, ";"); //Модель товара
                addStringCellToRow(values.get("sidArticleGroupDeclaration"), ";"); //Артикул товара
                addStringCellToRow(null, ";"); //Стандарт (ГОСТ, ОСТ, СПП, СТО, ТУ)
                addStringCellToRow(null, ";"); //Сорт (группа сортов)
                addStringCellToRow(null, ";"); //Дата выпуска
                addStringCellToRow(null, ";"); //Количество товара
                addStringCellToRow(null, ";"); //Краткое наименование единицы измерения
                addStringCellToRow(null, ";"); //Код единицы измерения
                addStringCellToRow(null, ";"); //Группа товаров

                writer.println(row);
            }

            writer.close();

            files.put("TSware.csv", IOUtils.getFileBytes(f));
            context.addAction(new ExportFileClientAction(files));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void addCellToRow(Object cell, Boolean isDouble, Integer precision, String prefix, String separator, Boolean useSeparatorIfNull) {
        if (cell != null) {
            if (prefix != null)
                row += prefix;
            if (!isDouble)
                row += cell.toString().trim();
            else {
                String bigDecimal = new BigDecimal(cell.toString()).setScale(precision, BigDecimal.ROUND_HALF_DOWN).toString();
                while (bigDecimal.endsWith("0") && bigDecimal.length() > 1)
                    bigDecimal = bigDecimal.substring(0, bigDecimal.length() - 1);
                row += bigDecimal;
            }
            row += separator;
        } else if (useSeparatorIfNull)
            row += separator;
    }

    public void addPartStringCellToRow(Object cell, String prefix, String separator, Boolean useSeparatorIfNull) {
        addCellToRow(cell, false, null, prefix, separator, useSeparatorIfNull);
    }

    public void addStringCellToRow(Object cell, String separator) {
        addCellToRow(cell, false, null, null, separator, true);
    }

    public void addDoubleCellToRow(Object cell, String separator, int precision) {
        addCellToRow(cell, true, precision, null, separator, true);
    }

}
