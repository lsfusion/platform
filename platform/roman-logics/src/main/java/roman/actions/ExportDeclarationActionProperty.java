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
            List<String> exportTitles = BaseUtils.toList("Номер", "Наименование товара", "Артикул товара", "Код товара по ТН ВЭД ТС",
                    "Цифровой код страны", "Буквенный код страны", "Количество товара", "Код единицы измерения",
                    "Краткое наименование единицы измерения", "Стоимость", "Вес нетто", "Вес брутто");

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

            for (Map<Object, Object> values : result.values()) {
                row = "";
                addStringCellToRow(values.get("numberGroupDeclaration"), ";");

                addPartStringCellToRow(values.get("nameCategoryGroupDeclaration"), null, " ", false);
                addPartStringCellToRow(values.get("sidGenderGroupDeclaration"), null, ",", false);
                addPartStringCellToRow(values.get("nameTypeFabricGroupDeclaration"), null, ",", false);
                addPartStringCellToRow(values.get("nameBrandGroupDeclaration"), "Торговая марка ", ",", false);
                addPartStringCellToRow(values.get("mainCompositionGroupDeclaration"), " Состав:", ";", true);

                addStringCellToRow("Арт. " + values.get("sidArticleGroupDeclaration"), ";");
                addStringCellToRow(values.get("sidCustomCategory10GroupDeclaration"), ";");
                addStringCellToRow(values.get("sidCountryGroupDeclaration"), ";");
                addStringCellToRow(values.get("sidOrigin2CountryGroupDeclaration"), ";");
                addDoubleCellToRow(values.get("quantityGroupDeclaration"), ";", 0);
                addStringCellToRow(values.get("sidUnitOfMeasureGroupDeclaration"), ";");
                addStringCellToRow(values.get("nameUnitOfMeasureGroupDeclaration"), ";");
                addDoubleCellToRow(values.get("sumGroupDeclaration"), ";", 7);
                addDoubleCellToRow(values.get("netWeightGroupDeclaration"), ";", 3);
                addDoubleCellToRow(values.get("grossWeightGroupDeclaration"), "", 3);

                writer.println(row);
            }

            writer.close();

            files.put("export.csv", IOUtils.getFileBytes(f));
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
