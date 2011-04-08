package roman;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.server.data.Field;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.ImportTable;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.SingleKeyTableUsage;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class SOliverInvoiceImporter extends EDIInvoiceImporter {
    int importFieldsQuantity;
    RomanBusinessLogics BL;
    Object[] fields;
    SOliverInvoiceEDIInputTable inputTable;

    public SOliverInvoiceImporter(RomanBusinessLogics BL, int importFieldsQuantity, ImportInputTable inputTable, Object... fields){
        super(inputTable, fields[0], fields[1], fields[2], fields[3], fields[4]);
        BaseUtils.toList(fields).subList(0, importFieldsQuantity - 1).toArray();
        this.importFieldsQuantity = importFieldsQuantity;
        this.inputTable = (SOliverInvoiceEDIInputTable) inputTable;
        this.BL = BL;
        this.fields = fields;
    }

    @Override
    public ImportTable getTable() throws ParseException, platform.server.data.type.ParseException {
        List<List<Object>> data = new ArrayList<List<Object>>();

        List<ImportField> importFields = new ArrayList<ImportField>();
        for (Object field : fields) {
            importFields.add((ImportField) field);
        }

        Map<String, LP> propertyMap = new OrderedMap<String, LP>();
        propertyMap.put("barcode", BL.barcodePricat);
        propertyMap.put("article", BL.articleNumberPricat);
        propertyMap.put("colorCode", BL.colorCodePricat);
        propertyMap.put("color", BL.colorNamePricat);
        propertyMap.put("size", BL.sizePricat);
        propertyMap.put("originalName", BL.originalNamePricat);
        propertyMap.put("country", BL.countryPricat);
        propertyMap.put("netWeight", BL.netWeightPricat);
        propertyMap.put("composition", BL.compositionPricat);
        propertyMap.put("price", BL.pricePricat);
        propertyMap.put("rrp", BL.rrpPricat);

        try {
            SQLSession sqlSession = BL.createSQL();

            //читаем данные из прайса по импортированным из инвойса штрихкодам
            SingleKeyTableUsage<PropertyField> table = new SingleKeyTableUsage<PropertyField>(importFields.get(0).getFieldClass(), new ArrayList<PropertyField>(), Field.<PropertyField>typeGetter());
            for (int i = 0; i < inputTable.rowsCnt(); i++) {
                String barcode = inputTable.getCellString(i, 0);
                table.insertRecord(sqlSession, new DataObject(barcode), new HashMap<PropertyField, ObjectValue>(), true, i == (inputTable.rowsCnt() - 1));
            }
            Map<PropertyInterface, KeyExpr> mapKeys = (Map<PropertyInterface,KeyExpr>) BL.barcodePricat.getMapKeys();
            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(mapKeys);
            query.and(table.join(BL.barcodePricat.property.getExpr(mapKeys)).getWhere());

            for (String propertyName : propertyMap.keySet()) {
                PropertyImplement propertyImplement = propertyMap.get(propertyName).getMapping(BaseUtils.singleValue(mapKeys));
                query.properties.put(propertyName, propertyMap.get(propertyName).property.getExpr(propertyImplement.mapping));
            }

            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> result = query.execute(sqlSession);
            List<Map<Object, Object>> priceResult = new ArrayList<Map<Object, Object>>();
            for (Map<PropertyInterface, Object> one : result.keySet()) {
                priceResult.add(result.get(one));
            }

            //сливаем две таблицы: из инвойса и из прайса
            for (int i = 0; i < inputTable.rowsCnt(); i++) {
                List<Object> row = new ArrayList<Object>();
                String barcode = "";

                //импортированное из инвойса
                for (Map.Entry<ImportField, Pair<Integer, Integer>> entry : fieldPosition.entrySet()) {
                    ImportField field = entry.getKey();
                    String cellValue = getCellString(field, i, entry.getValue().first);
                    if (field.equals(importFields.get(0))) {
                        barcode = cellValue;
                    }
                    String transformedValue = transformValue(i, entry.getValue().first, entry.getValue().second, cellValue);
                    row.add(getResultObject(field, transformedValue));
                }

                //найденное в прайсе
                int priceFieldQuantity = priceResult.isEmpty() ? 1 : priceResult.get(0).size();
                boolean appearsInPrice = false;
                for (Map<Object, Object> key : priceResult) {
                    if (key.get(propertyMap.keySet().toArray()[0]).toString().equals(barcode)) {
                        for (int j = 1; j < key.values().size(); j++) {
                            Object value = key.get(propertyMap.keySet().toArray()[j]);
                            String cellValue = value != null ? value.toString() : "";
                            String transformedValue = transformValue(i, importFieldsQuantity + j - 1, 0, cellValue);
                            row.add(getResultObject(importFields.get(importFieldsQuantity + j - 1), transformedValue));
                        }
                        appearsInPrice = true;
                    }
                }
                //если записи нет в прайсе, записавем пустые строки
                if (!appearsInPrice) {
                    for (int j = 1; j < priceFieldQuantity; j++) {
                        row.add(getResultObject(importFields.get(importFieldsQuantity + j - 1), ""));
                    }
                }

                //допмисываем пустыми строками оставшиеся поля
                for (int j = importFieldsQuantity + priceFieldQuantity - 1; j < importFields.size(); j++) {
                    row.add(getResultObject(importFields.get(j), ""));
                }
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new ImportTable(importFields, data);
    }
}
