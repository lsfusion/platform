package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DateClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportExcelItemsActionProperty extends ExportExcelActionProperty {

    public ExportExcelItemsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportItems", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Код товара", "Код группы", "Наименование", "Ед.изм.", "Краткая ед.изм.",
                "Код ед.изм.", "Название бренда", "Код бренда", "Страна", "Штрих-код", "Весовой",
                "Вес нетто", "Вес брутто", "Состав", "НДС, %", "Код посуды", "Цена посуды", "НДС посуды, %",
                "Код нормы отходов", "Оптовая наценка", "Розничная наценка", "Кол-во в упаковке (закупка)",
                "Кол-во в упаковке (продажа)");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {
            ObjectValue retailCPLT = LM.findLCPByCompoundName("externalizableSID").readClasses(session, new DataObject("cplt_retail", StringClass.get(100)));
            ObjectValue wholesaleCPLT = LM.findLCPByCompoundName("externalizableSID").readClasses(session, new DataObject("cplt_wholesale", StringClass.get(100)));

            KeyExpr itemExpr = new KeyExpr("Item");
            ImRevMap<Object, KeyExpr> itemKeys = MapFact.singletonRev((Object) "Item", itemExpr);

            String[] itemProperties = new String[]{"itemGroupItem", "nameAttributeItem", "UOMItem",
                    "brandItem", "countryItem", "nameCountryCountryItem", "idBarcodeSku", "isWeightItem", "netWeightItem", "grossWeightItem",
                    "compositionItem", "wareItem", "Purchase.amountPackSku", "Sale.amountPackSku"};
            QueryBuilder<Object, Object> itemQuery = new QueryBuilder<Object, Object>(itemKeys);
            for (String iProperty : itemProperties) {
                itemQuery.addProperty(iProperty, getLCP(iProperty).getExpr(context.getModifier(), itemExpr));
            }

            itemQuery.and(getLCP("nameAttributeItem").getExpr(context.getModifier(), itemQuery.getMapExprs().get("Item")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> itemResult = itemQuery.execute(session.sql);

            for (int i = 0, size = itemResult.size(); i < size; i++) {

                ImMap<Object, Object> itemValue = itemResult.getValue(i);

                Integer itemID = (Integer) itemResult.getKey(i).get("Item");
                String name = (String) itemValue.get("nameAttributeItem");
                String nameCountry = (String) itemValue.get("nameCountryCountryItem");
                String idBarcodeSku = (String) itemValue.get("idBarcodeSku");
                Boolean isWeightItem = itemValue.get("idBarcodeSku") != null;
                Double netWeightItem = (Double) itemValue.get("netWeightItem");
                Double grossWeightItem = (Double) itemValue.get("grossWeightItem");
                String compositionItem = (String) itemValue.get("compositionItem");
                Double purchaseAmount = (Double) itemValue.get("Purchase.amountPackSku");
                Double saleAmount = (Double) itemValue.get("Sale.amountPackSku");
                Integer itemGroupID = (Integer) itemValue.get("itemGroupItem");

                Object uomItem = itemValue.get("UOMItem");
                String nameUOM = null;
                String shortNameUOM = null;
                if (uomItem != null) {
                    DataObject uomItemObject = new DataObject(uomItem, (ConcreteClass) LM.findClassByCompoundName("UOM"));
                    nameUOM = (String) LM.findLCPByCompoundName("nameUOM").read(session, uomItemObject);
                    shortNameUOM = (String) LM.findLCPByCompoundName("shortNameUOM").read(session, uomItemObject);
                }

                Object brandItem = itemValue.get("brandItem");
                String nameBrand = null;
                if (brandItem != null) {
                    DataObject brandObject = new DataObject(brandItem, (ConcreteClass) LM.findClassByCompoundName("Brand"));
                    nameBrand = (String) LM.findLCPByCompoundName("nameBrand").read(session, brandObject);
                }

                Object wareItem = itemValue.get("wareItem");
                Double priceWare = null;
                Double vatWare = null;
                if (wareItem != null) {
                    DataObject wareObject = new DataObject(wareItem, (ConcreteClass) LM.findClassByCompoundName("Ware"));
                    priceWare = (Double) LM.findLCPByCompoundName("warePrice").read(session, wareObject);
                    vatWare = (Double) LM.findLCPByCompoundName("valueCurrentRateRangeWare").read(session, wareObject);
                }


                DataObject itemObject = new DataObject(itemResult.getKey(i).get("Item"), (ConcreteClass) LM.findClassByCompoundName("Item"));
                Object countryItem = itemValue.get("countryItem");
                DataObject countryObject = countryItem == null ? null : new DataObject(countryItem, (ConcreteClass) LM.findClassByCompoundName("Country"));
                DataObject dateObject = new DataObject(new Date(System.currentTimeMillis()), DateClass.instance);
                Double vatItem = countryObject == null ? null : (Double) LM.findLCPByCompoundName("valueVATItemCountryDate").read(session, itemObject, countryObject, dateObject);

                Integer writeOffRateID = countryObject == null ? null : (Integer) LM.findLCPByCompoundName("writeOffRateCountryItem").read(session, countryObject, itemObject);

                Double retailMarkup = retailCPLT instanceof NullValue ? null : (Double) LM.findLCPByCompoundName("markupCalcPriceListTypeSku").read(session, (DataObject) retailCPLT, itemObject);
                Double wholesaleMarkup = wholesaleCPLT instanceof NullValue ? null : (Double) LM.findLCPByCompoundName("markupCalcPriceListTypeSku").read(session, (DataObject) wholesaleCPLT, itemObject);

                data.add(Arrays.asList(trimNotNull(itemID), trimNotNull(itemGroupID), trimNotNull(name), trimNotNull(nameUOM),
                        trimNotNull(shortNameUOM), trimNotNull(uomItem), trimNotNull(nameBrand), trimNotNull(brandItem),
                        trimNotNull(nameCountry), trimNotNull(idBarcodeSku), isWeightItem ? "True" : "False",
                        trimNotNull(netWeightItem), trimNotNull(grossWeightItem), trimNotNull(compositionItem),
                        trimNotNull(vatItem), trimNotNull(wareItem), trimNotNull(priceWare), trimNotNull(vatWare),
                        trimNotNull(writeOffRateID), trimNotNull(retailMarkup), trimNotNull(wholesaleMarkup),
                        trimNotNull(purchaseAmount), trimNotNull(saleAmount)));
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return data;
    }
}