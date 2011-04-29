package roman;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.SingleKeyTableUsage;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

/**
 * User: DAle
 * Date: 22.04.11
 * Time: 15:03
 */

public class InvoicePricatMergeInputTable implements ImportInputTable {
    private ImportInputTable invoiceTable;
    private RomanBusinessLogics BL;
    private List<List<String>> data = new ArrayList<List<String>>();

    public static enum ResultField {BARCODE, ARTICLE, INVOICE, BOXNUMBER, COLORCODE, COLOR, SIZE, ORIGINALNAME,
        COUNTRY, NETWEIGHT, COMPOSITION, PRICE, RRP, QUANTITY, NUMBERSKU, CUSTOMCODE, CUSTOMCODE6}

    public InvoicePricatMergeInputTable(RomanBusinessLogics BL, ImportInputTable invoiceTable, ResultField... invoiceFields) {
        this.BL = BL;
        this.invoiceTable = invoiceTable;

        List<ResultField> invoiceFieldsList = Arrays.asList(invoiceFields);
        Map<ResultField, Integer> invoiceFieldsPos = new HashMap<ResultField, Integer>();
        for (ResultField field : invoiceFieldsList) {
            invoiceFieldsPos.put(field, invoiceFieldsList.indexOf(field));
        }
        assert invoiceFieldsPos.containsKey(ResultField.BARCODE) && invoiceFieldsPos.containsKey(ResultField.INVOICE) &&
               invoiceFieldsPos.containsKey(ResultField.BOXNUMBER);

        Map<ResultField, LP<?>> propertyMap = createPricatFieldsMap();

        try {
            Map<String, Map<ResultField, Object>> pricatData = getDataFromPricat(invoiceFieldsList, propertyMap);
            int barcodeInvoiceIndex = invoiceFieldsList.indexOf(ResultField.BARCODE);

            //сливаем таблицу инвойса и данные из прайса
            for (int i = 0; i < invoiceTable.rowsCnt(); i++) {
                List<String> row = new ArrayList<String>();
                String barcode = invoiceTable.getCellString(i, barcodeInvoiceIndex).trim();
                boolean pricatContainsBarcode = pricatData.containsKey(barcode);

                for (ResultField field : ResultField.values()) {
                    String value = "";
                    if (invoiceFieldsPos.containsKey(field) && !invoiceTable.getCellString(i, invoiceFieldsPos.get(field)).trim().equals("")) {
                        value = invoiceTable.getCellString(i, invoiceFieldsPos.get(field));
                    } else if (pricatContainsBarcode && pricatData.get(barcode).containsKey(field)) {
                        Object data = pricatData.get(barcode).get(field);
                        value = (data == null ? "" : data.toString());
                    }
                    row.add(value);
                }

                data.add(row);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<ResultField, LP<?>> createPricatFieldsMap() {
        Map<ResultField, LP<?>> propertyMap = new OrderedMap<ResultField, LP<?>>();

        propertyMap.put(ResultField.BARCODE, BL.barcodePricat);
        propertyMap.put(ResultField.ARTICLE, BL.articleNumberPricat);
        propertyMap.put(ResultField.CUSTOMCODE, BL.customCategoryOriginalPricat);
        propertyMap.put(ResultField.COLORCODE, BL.colorCodePricat);
        propertyMap.put(ResultField.COLOR, BL.colorNamePricat);
        propertyMap.put(ResultField.SIZE, BL.sizePricat);
        propertyMap.put(ResultField.ORIGINALNAME, BL.originalNamePricat);
        propertyMap.put(ResultField.COUNTRY, BL.countryPricat);
        propertyMap.put(ResultField.NETWEIGHT, BL.netWeightPricat);
        propertyMap.put(ResultField.COMPOSITION, BL.compositionPricat);
        propertyMap.put(ResultField.PRICE, BL.pricePricat);
        propertyMap.put(ResultField.RRP, BL.rrpPricat);

        return propertyMap;
    }

    private Map<String, Map<ResultField, Object>> getDataFromPricat(List<ResultField> invoiceFields,
                                                                     Map<ResultField, LP<?>> propertyMap) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        SQLSession sqlSession = BL.createSQL();

        //читаем данные из прайса по импортированным из инвойса штрихкодам
        Result<ValueClass> resultClass = new Result<ValueClass>();
        BL.barcode.getCommonClasses(resultClass);
        Type keyType = resultClass.result.getType();
        SingleKeyTableUsage<ResultField> table = new SingleKeyTableUsage<ResultField>(keyType, new ArrayList<ResultField>(), null);

        for (int i = 0; i < invoiceTable.rowsCnt(); i++) {
            String barcodeStr = invoiceTable.getCellString(i, invoiceFields.indexOf(ResultField.BARCODE));
            table.insertRecord(sqlSession, new DataObject(barcodeStr), new HashMap<ResultField, ObjectValue>(), true, i == (invoiceTable.rowsCnt() - 1));
        }

        Map<PropertyInterface, KeyExpr> mapKeys = (Map<PropertyInterface,KeyExpr>) BL.barcodePricat.getMapKeys();
        Query<PropertyInterface, ResultField> query = new Query<PropertyInterface, ResultField>(mapKeys);
        query.and(table.join(BL.barcodePricat.property.getExpr(mapKeys)).getWhere());

        for (ResultField propertyName : propertyMap.keySet()) {
            PropertyImplement propertyImplement = propertyMap.get(propertyName).getMapping(BaseUtils.singleValue(mapKeys));
            query.properties.put(propertyName, propertyMap.get(propertyName).property.getExpr(propertyImplement.mapping));
        }

        OrderedMap<Map<PropertyInterface, Object>, Map<ResultField, Object>> result = query.execute(sqlSession);
        Map<String, Map<ResultField, Object>> pricatResult = new HashMap<String, Map<ResultField, Object>>();
        for (Map<PropertyInterface, Object> key : result.keySet()) {
            Map<ResultField, Object> value = result.get(key);
            pricatResult.put((String) value.get(ResultField.BARCODE), value);
        }
        return pricatResult;
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        if (data.isEmpty()) return 0;
        return data.get(0).size();
    }
}
