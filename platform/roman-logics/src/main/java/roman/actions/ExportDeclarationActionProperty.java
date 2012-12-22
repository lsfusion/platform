package roman.actions;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class ExportDeclarationActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface declarationInterface;
    String row;

    public ExportDeclarationActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("declaration")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            List<String> exportProperties = BaseUtils.toList("numberGroupDeclaration", "nameBrandGroupDeclaration",
                    "nameCategoryGroupDeclaration", "sidGenderGroupDeclaration", "nameTypeFabricGroupDeclaration", "nameGroupDeclaration",
                    "sidArticleGroupDeclaration", "sidCustomCategory10GroupDeclaration", "mainCompositionGroupDeclaration",
                    "sidCountryGroupDeclaration", "sidOrigin2CountryGroupDeclaration", "quantityCoefficientGroupDeclaration", "sidUnitOfMeasureGroupDeclaration",
                    "genitiveNameUnitOfMeasureGroupDeclaration", "sumContractGroupDeclaration", "netWeightGroupDeclaration",
                    "grossWeightGroupDeclaration", "sidSpecUnitOfMeasureGroupDeclaration", "genitiveNameSpecUnitOfMeasureGroupDeclaration",
                    "quantitySpecGroupDeclaration", "sidAdditionalUnitOfMeasureGroupDeclaration", "genitiveNameAdditionalUnitOfMeasureGroupDeclaration",
                    "nameTypeContainerGroupDeclaration", "CRMDeclaration");

            List<String> exportTitlesTSware = BaseUtils.toList("Порядковый номер декларируемого товара", "Наименование товара", "Вес брутто",
                    "Вес нетто", "Вес нетто без упаковки", "Фактурная стоимость товара", "Таможенная стоимость",
                    "Статистическая стоимость", "Код товара по ТН ВЭД ТС", "Запреты и ограничения", "Интеллектуальная собственность",
                    "Цифровой код страны происхождения товара", "Буквенный код страны происхождения товара",
                    "Код метода определения таможенной стоимости", "Название географического пункта",
                    "Код условий поставки по Инкотермс", "Код вида поставки товаров", "Количество мест",
                    "Вид грузовых мест", "Код валюты квоты", "Остаток квоты в валюте", "Остаток квоты в единице измерения",
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
                    "Наименование региона переработки", "Наименование населенного пункта переработки", "Улица и дом переработки",
                    "Наименование лица (отправителя) переработки", "УНП лица (отправителя) переработки",
                    "Почтовый индекс лица (отправителя) переработки", "Наименование региона лица (отправителя) переработки",
                    "Населенный пункт лица (отправителя) переработки", "Улица и дом лица (отправителя) переработки",
                    "Код страны регистрации лица (отправителя) переработки", "Название страны регистрации лица (отправителя) переработки",
                    "Номер документа, удостоверяющего личность физического лица  переработки", "Идентификационный номер физического лица переработки",
                    "Дата выдачи документа, удостоверяющего личность физического лица переработки",
                    "Код документа, удостоверяющего личность физического лица переработки");

            List<String> exportTitlesTSmarkings = BaseUtils.toList(
                    "Номер товара", "Наименование изготовителя", "Товарный знак", "Марка товара", "Модель товара", "Артикул товара",
                    "Стандарт (ГОСТ, ОСТ, СПП, СТО, ТУ)", "Сорт (группа сортов)", "Дата выпуска", "Количество товара",
                    "Краткое наименование единицы измерения", "Код единицы измерения", "Группа товаров");

            List<String> exportTitlesTSDocs44 = BaseUtils.toList("Номер товара", "Номер документа", "Дата документа",
                    "Код таможенного органа", "Код вида представляемого документа", "Дата начала действия документа",
                    "Дата окончания действия документа", "Дата представления недостающего документа", "Код срока временного ввоза",
                    "Заявляемый срок временного ввоза", "Код вида платежа (льготы)", "ОПЕРЕЖАЮЩАЯ ПОСТАВКА",
                    "Запрашиваемый срок переработки", "Код страны (сертификат происхождения)", "Код вида упрощений (реестр УЭО)", "Наименование документа");

            DataObject declarationObject = context.getKeyValue(declarationInterface);

            Map<String, byte[]> files = new HashMap<String, byte[]>();
            File fileTSware = File.createTempFile("TSware", ".csv");
            PrintWriter writerTSware = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(fileTSware), "windows-1251"));
            File fileTSMarkings = File.createTempFile("TSmarkings", ".csv");
            PrintWriter writerTSmarkings = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(fileTSMarkings), "windows-1251"));
            File fileTSDocs44 = File.createTempFile("TSDocs44", ".csv");
            PrintWriter writerTSDocs44 = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(fileTSDocs44), "windows-1251"));

            row = "";
            for (String title : exportTitlesTSware)
                addStringCellToRow(title, ";");
            writerTSware.println(row);

            row = "";
            for (String title : exportTitlesTSmarkings)
                addStringCellToRow(title, ";");
            writerTSmarkings.println(row);

            row = "";
            for (String title : exportTitlesTSDocs44)
                addStringCellToRow(title, ";");
            writerTSDocs44.println(row);

            LCP<?> isGroupDeclaration = LM.is(getClass("groupDeclaration"));
            ImRevMap<Object, KeyExpr> keys = (ImRevMap<Object, KeyExpr>) isGroupDeclaration.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
            for (String propertySID : exportProperties)
                query.addProperty(propertySID, getLCP(propertySID).getExpr(context.getModifier(), key));
            query.and(isGroupDeclaration.getExpr(key).getWhere());
            query.and(getLCP("declarationGroupDeclaration").getExpr(context.getModifier(), key).compare(declarationObject.getExpr(), Compare.EQUALS));
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context.getSession().sql);

            TreeMap<Integer, Map<String, Object>> sortedRows = new TreeMap<Integer, Map<String, Object>>();

            for (int i=0,size=result.size();i<size;i++) {
                ImMap<Object, Object> values = result.getValue(i);
                Map<String, Object> valuesRow = new HashMap<String, Object>();
                for (String propertySID : exportProperties)
                    valuesRow.put(propertySID, values.get(propertySID));
                valuesRow.put("groupDeclarationID", result.getKey(i).getValue(0));
                sortedRows.put((Integer) values.get("numberGroupDeclaration"), valuesRow);
            }

            Double containerNumberDeclaration = (Double) LM.findLCPByCompoundName("containerNumberDeclaration").read(context.getSession(), declarationObject);
            boolean containerNumberFlag = false;
            String numberContractDeclaration = (String) LM.findLCPByCompoundName("numberContractDeclaration").read(context.getSession(), declarationObject);
            String CRMDeclaration = (String) LM.findLCPByCompoundName("CRMDeclaration").read(context.getSession(), declarationObject);
            Date dateFromContractDeclaration = (Date) LM.findLCPByCompoundName("dateFromContractDeclaration").read(context.getSession(), declarationObject);

            row = "";
            addConstantStringCellToRow("1", ";");
            addStringCellToRow(numberContractDeclaration, ";");
            if (dateFromContractDeclaration != null)
                addStringCellToRow(String.valueOf(dateFromContractDeclaration), ";");
            writerTSDocs44.println(row);
            for (int i = 0; i < 9; i++) {
                writerTSDocs44.println("");
            }

            for (Map.Entry<Integer, Map<String, Object>> entry : sortedRows.entrySet()) {

                //Creation of TSDocs44.csv
                KeyExpr innerInvoiceExpr = new KeyExpr("innerInvoice");
                ImRevMap<Object, KeyExpr> innerInvoiceKeys = MapFact.singletonRev((Object)"innerInvoice", innerInvoiceExpr);

                QueryBuilder<Object, Object> innerInvoiceQuery = new QueryBuilder<Object, Object>(innerInvoiceKeys);
                innerInvoiceQuery.addProperty("sidInnerInvoice", getLCP("sidInnerInvoice").getExpr(innerInvoiceExpr));
                innerInvoiceQuery.addProperty("dateInnerInvoice", getLCP("dateInnerInvoice").getExpr(innerInvoiceExpr));

                innerInvoiceQuery.and(getLCP("quantityGroupDeclarationInnerInvoice").getExpr(new DataObject(entry.getValue().get("groupDeclarationID")/*result.getKey(0).values().iterator().next()*/, (ConcreteClass) getClass("groupDeclaration")).getExpr(), innerInvoiceExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> innerInvoiceResult = innerInvoiceQuery.execute(context.getSession().sql);


                for (ImMap<Object, Object> innerInvoiceValues : innerInvoiceResult.valueIt()) {
                    row = "";
                    addStringCellToRow(entry.getKey(), ";");//numberGroupDeclaration
                    addStringCellToRow(innerInvoiceValues.get("sidInnerInvoice"), ";");
                    addStringCellToRow(innerInvoiceValues.get("dateInnerInvoice"), ";");
                    addStringCellToRow(null, ";");
                    addConstantStringCellToRow("04021", ";");

                    writerTSDocs44.println(row);
                }

                //Creation of TSware.csv
                row = "";
                Map<String, Object> values = entry.getValue();
                addStringCellToRow(entry.getKey(), ";"); //numberGroupDeclaration

                //addPartStringCellToRow(values.get("nameCategoryGroupDeclaration"), null, " ", false);
                //addPartStringCellToRow(values.get("sidGenderGroupDeclaration"), null, ",", false);
                //addPartStringCellToRow(values.get("nameTypeFabricGroupDeclaration"), null, ",", false);
                //addPartStringCellToRow(values.get("nameBrandGroupDeclaration"), "Торговая марка ", ",", false);
                //addPartStringCellToRow(values.get("mainCompositionGroupDeclaration"), " Состав:", ";", true);

                addStringCellToRow(values.get("nameGroupDeclaration"), ";");

                addDoubleCellToRow(values.get("grossWeightGroupDeclaration"), ";", 3);
                addDoubleCellToRow(values.get("netWeightGroupDeclaration"), ";", 3);
                addDoubleCellToRow(values.get("netWeightGroupDeclaration"), ";", 3); //Вес нетто без упаковки
                addDoubleCellToRow(values.get("sumContractGroupDeclaration"), ";", 7);
                addStringCellToRow(null, ";"); //Таможенная стоимость
                addStringCellToRow(null, ";"); //Статистическая стоимость
                addStringCellToRow(values.get("sidCustomCategory10GroupDeclaration"), ";");
                addStringCellToRow(null, ";"); //Запреты и ограничения
                addStringCellToRow(null, ";"); //Интеллектуальная собственность
                addStringCellToRow(values.get("sidCountryGroupDeclaration"), ";");
                addStringCellToRow(values.get("sidOrigin2CountryGroupDeclaration"), ";");
                addConstantStringCellToRow("1", ";"); //Код метода определения таможенной стоимости
                addStringCellToRow(null, ";"); //Название географического пункта
                addStringCellToRow(null, ";"); //Код условий поставки по Инкотермс
                addStringCellToRow(null, ";"); //Код вида поставки товаров
                if (!containerNumberFlag) {
                    addStringCellToRow(containerNumberDeclaration, ";"); //Количество мест
                    containerNumberFlag = true;
                } else {
                    addStringCellToRow(0, ";"); //Количество мест
                }
                String nameContainer = (String) values.get("nameTypeContainerGroupDeclaration");
                addStringCellToRow(null, ";"); //Код валюты квоты
                addStringCellToRow(null, ";"); //Остаток квоты в валюте
                addStringCellToRow(null, ";"); //Остаток квоты в единице измерения
                addStringCellToRow(null, ";"); //Код единицы измерения квоты
                addStringCellToRow(null, ";"); //Наименование единицы измерения квоты
                addDoubleCellToRow(values.get("quantitySpecGroupDeclaration"), ";", 3); //Количество товара в специфических единицах измерения
                addStringCellToRow(null, ";"); //Количество подакцизного товара
                String sidSpecUnit = (String) values.get("sidSpecUnitOfMeasureGroupDeclaration");
                String nameSpecUnit = (String) values.get("genitiveNameSpecUnitOfMeasureGroupDeclaration");
                addConstantStringCellToRow(sidSpecUnit != null ? sidSpecUnit : "166", ";");  //Код специфических единиц измерения
                addConstantStringCellToRow(nameSpecUnit != null ? nameSpecUnit : "КГ", ";");  //Краткое наименование специфических единиц измерения
                addStringCellToRow(null, ";"); //Код единицы измерения подакцизного товара
                addStringCellToRow(null, ";"); //Наименование единицы измерения подакцизного товара
                addDoubleCellToRow(values.get("quantityCoefficientGroupDeclaration"), ";", 0);
                addStringCellToRow(values.get("sidAdditionalUnitOfMeasureGroupDeclaration"), ";");
                addStringCellToRow(values.get("genitiveNameAdditionalUnitOfMeasureGroupDeclaration"), ";");

                addStringCellToRow(null, ";"); //Корректировки таможенной стоимости
                addStringCellToRow(null, ";"); //Количество акцизных марок
                addConstantStringCellToRow("00", ";"); //Код предшествующей таможенной процедуры
                addConstantStringCellToRow("ОО", ";"); //Преференция код 1
                addConstantStringCellToRow("ОО", ";"); //Преференция код 2
                addConstantStringCellToRow("-", ";"); //Преференция код 3
                addConstantStringCellToRow("ОО", ";"); //Преференция код 4
                addConstantStringCellToRow("000", ";"); //Код особенности перемещения товаров
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

                writerTSware.println(row);

                //Creation of TSmarkings.csv
                row = "";
                addStringCellToRow(entry.getKey(), ";"); //numberGroupDeclaration
                addConstantStringCellToRow("ПРОИЗВОДИТЕЛЬ НЕИЗВЕСТЕН", ";"); //Наименование изготовителя
                //addStringCellToRow(null, ";");
                addStringCellToRow(null, ";"); //Товарный знак
                addStringCellToRow(values.get("nameBrandGroupDeclaration"), ";"); //Марка товара
                addStringCellToRow(null, ";"); //Модель товара
                addStringCellToRow(values.get("sidArticleGroupDeclaration"), ";"); //Артикул товара
                addStringCellToRow(null, ";"); //Стандарт (ГОСТ, ОСТ, СПП, СТО, ТУ)
                addStringCellToRow(null, ";"); //Сорт (группа сортов)
                addStringCellToRow(null, ";"); //Дата выпуска
                addDoubleCellToRow(values.get("quantityCoefficientGroupDeclaration"), ";", 0); //Количество товара
                addStringCellToRow(values.get("genitiveNameUnitOfMeasureGroupDeclaration"), ";"); //Краткое наименование единицы измерения
                addStringCellToRow(values.get("sidUnitOfMeasureGroupDeclaration"), ";"); //Код единицы измерения
                addStringCellToRow(values.get("nameCategoryGroupDeclaration"), ";"); //Группа товаров

                writerTSmarkings.println(row);
            }

            writerTSware.close();
            writerTSmarkings.close();
            writerTSDocs44.close();

            files.put("TSware.csv", IOUtils.getFileBytes(fileTSware));
            files.put("TSmarkings.csv", IOUtils.getFileBytes(fileTSMarkings));
            files.put("TSDocs44.csv", IOUtils.getFileBytes(fileTSDocs44));
            context.delayUserInterfaction(new ExportFileClientAction(files));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void addCellToRow(Object cell, Boolean isDouble, Integer precision, String prefix, String separator, Boolean useSeparatorIfNull) {
        if (cell != null) {
            if (prefix != null)
                row += prefix;
            if (!isDouble)
                row += cell.toString().trim().replace('.', ',');
            else {
                String bigDecimal = new BigDecimal(cell.toString()).setScale(precision, BigDecimal.ROUND_HALF_DOWN).toString().replace('.', ',');
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

    public void addConstantStringCellToRow(String constant, String separator) {
        row += constant + separator;
    }

}
