package retail.actions;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DateClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.RetailBusinessLogics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ImportLSTDataActionProperty extends ScriptingActionProperty {
    private ScriptingLogicsModule retailLM;

    public ImportLSTDataActionProperty(RetailBusinessLogics BL) {
        super(BL);
        retailLM = BL.getLM();

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        String path = retailLM.getLPByName("importLSTDirectory").read(context).toString().trim();
        if (!"".equals(path)) {
            Boolean importInactive = retailLM.getLPByName("importInactive").read(context) != null;
            if (retailLM.getLPByName("importGroupItems").read(context) != null) {
                importItemGroups(path + "//_sprgrt.dbf", context);
                importParentGroups(path + "//_sprgrt.dbf", context);
            }

            if (retailLM.getLPByName("importWares").read(context) != null) {
                importWares(path + "//_sprgrm.dbf", context);
            }

            if (retailLM.getLPByName("importItems").read(context) != null) {
                Object numberOfItems = retailLM.getLPByName("importNumberItems").read(context);
                Object numberOfItemsAtATime = retailLM.getLPByName("importNumberItemsAtATime").read(context);
                importItems(path + "//_sprgrm.dbf", path + "//_postvar.dbf", path + "//_grmcen.dbf", importInactive, context,
                        numberOfItems == null ? 0 : (Integer) numberOfItems, numberOfItemsAtATime == null ? 5000 : (Integer) numberOfItemsAtATime);
            }

            if (retailLM.getLPByName("importPrices").read(context) != null) {
                Object numberOfItems = retailLM.getLPByName("importNumberItems").read(context);
                importPrices(path + "//_grmcen.dbf", context, numberOfItems == null ? 0 : (Integer) numberOfItems);
            }

            if (retailLM.getLPByName("importShipment").read(context) != null) {
                Object numberOfShipments = retailLM.getLPByName("importNumberShipments").read(context);
                importShipment(path + "//_ostn.dbf", context, numberOfShipments == null ? 0 : (Integer) numberOfShipments);
            }

            if (retailLM.getLPByName("importAssortment").read(context) != null) {
                Object numberOfItems = retailLM.getLPByName("importNumberItems").read(context);
                importAssortment(path + "//_strvar.dbf", context, numberOfItems == null ? 0 : (Integer) numberOfItems);
            }

            if (retailLM.getLPByName("importCompanies").read(context) != null)
                importCompanies(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLPByName("importSuppliers").read(context) != null)
                importSuppliers(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLPByName("importCustomers").read(context) != null)
                importCustomers(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLPByName("importStores").read(context) != null)
                importStores(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLPByName("importDepartmentStores").read(context) != null)
                importStocks(path + "//_sprana.dbf", path + "//_storestr.dbf", importInactive, context);

            if (retailLM.getLPByName("importBanks").read(context) != null)
                importBanks(path + "//_sprbank.dbf", context);

            if (retailLM.getLPByName("importRateWastes").read(context) != null)
                importRateWastes(path + "//_sprvgrt.dbf", context);
        }

    }

    private void importParentGroups(String path, ExecutionContext context) {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(BL.LM.extSID);
            ImportField parentGroupID = new ImportField(BL.LM.extSID);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(parentGroupID));

            List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
            propsParent.add(new ImportProperty(parentGroupID, retailLM.getLPByName("parentItemGroup").getMapping(itemGroupKey),
                    BL.LM.object(retailLM.getClassByName("itemGroup")).getMapping(parentGroupKey)));
            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importItemGroups(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, false);

            ImportField itemGroupID = new ImportField(BL.LM.extSID);
            ImportField itemGroupName = new ImportField(BL.LM.name);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupID));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            props.add(new ImportProperty(itemGroupID, BL.LM.extSID.getMapping(itemGroupKey)));
            props.add(new ImportProperty(itemGroupName, BL.LM.name.getMapping(itemGroupKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWares(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importWaresFromDBF(path);

            ImportField wareIDField = new ImportField(BL.LM.extSID);
            ImportField wareNameField = new ImportField(BL.LM.name);

            ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ware"),
                    BL.LM.extSIDToObject.getMapping(wareIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(wareIDField, BL.LM.extSID.getMapping(wareKey)));
            props.add(new ImportProperty(wareNameField, BL.LM.name.getMapping(wareKey)));

            ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importItems(String itemsPath, String quantityPath, String warePath, Boolean importInactive, ExecutionContext context, Integer numberOfItems, Integer numberOfItemsAtATime) throws SQLException {

        try {

            int amountOfImportIterations = (int) Math.ceil((double) numberOfItems / numberOfItemsAtATime);
            Integer rest = numberOfItems;
            for (int i = 0; i < amountOfImportIterations; i++) {
                importPackOfItems(itemsPath, quantityPath, warePath, importInactive, context, rest > numberOfItemsAtATime ? numberOfItemsAtATime : rest, i);
                rest -= numberOfItemsAtATime;
                System.gc();
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importPackOfItems(String itemsPath, String quantityPath, String warePath, Boolean importInactive, ExecutionContext context, Integer numberOfItemsAtATime, int i) throws SQLException, IOException, xBaseJException {
        List<List<Object>> data = importItemsFromDBF(itemsPath, quantityPath, warePath, importInactive, numberOfItemsAtATime, numberOfItemsAtATime * i);
        if (data == null) return;

        ImportField itemIDField = new ImportField(BL.LM.extSID);
        ImportField itemGroupIDField = new ImportField(BL.LM.extSID);
        ImportField itemCaptionField = new ImportField(BL.LM.name);
        ImportField unitOfMeasureIDField = new ImportField(BL.LM.name);
        ImportField nameUnitOfMeasureField = new ImportField(BL.LM.name);
        ImportField brandIDField = new ImportField(BL.LM.name);
        ImportField nameBrandField = new ImportField(BL.LM.name);
        ImportField countryIDField = new ImportField(retailLM.getLPByName("extSIDCountry"));
        ImportField nameCountryField = new ImportField(BL.LM.name);
        ImportField barcodeField = new ImportField(retailLM.getLPByName("barcodeEx"));
        ImportField dateField = new ImportField(BL.LM.date);
        ImportField importerPriceField = new ImportField(retailLM.getLPByName("importerPriceItemDate"));
        ImportField percentWholesaleMarkItemField = new ImportField(retailLM.getLPByName("percentWholesaleMarkItem"));
        ImportField isFixPriceItemField = new ImportField(retailLM.getLPByName("isFixPriceItem"));
        ImportField isLoafCutItemField = new ImportField(retailLM.getLPByName("isLoafCutItem"));
        ImportField isWeightItemField = new ImportField(retailLM.getLPByName("isWeightItem"));
        ImportField compositionField = new ImportField(retailLM.getLPByName("compositionScalesItem"));
        ImportField dataSuppliersRangeItemField = new ImportField(retailLM.getLPByName("dataRate"));
        ImportField dataRetailRangeItemField = new ImportField(retailLM.getLPByName("dataRate"));
        ImportField quantityPackItemField = new ImportField(retailLM.getLPByName("quantityPackItem"));
        ImportField wareIDField = new ImportField(BL.LM.extSID);
        ImportField priceWareField = new ImportField(retailLM.getLPByName("priceWareDate"));
        ImportField ndsWareField = new ImportField(retailLM.getLPByName("dataRate"));
        ImportField rateWasteIDField = new ImportField(BL.LM.extSID);

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                BL.LM.extSIDToObject.getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                BL.LM.extSIDToObject.getMapping(itemGroupIDField));

        ImportKey<?> unitOfMeasureKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("unitOfMeasure"),
                BL.LM.extSIDToObject.getMapping(unitOfMeasureIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("brand"),
                BL.LM.extSIDToObject.getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey(BL.LM.country,
                retailLM.getLPByName("extSIDToCountry").getMapping(countryIDField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("barcode"),
                retailLM.getLPByName("barcodeToDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLPByName("dataActingRateRangeToRange").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLPByName("dataActingRateRangeToRange").getMapping(dataRetailRangeItemField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ware"),
                BL.LM.extSIDToObject.getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLPByName("dataActingRateRangeToRange").getMapping(ndsWareField));

        ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("rateWaste"),
                BL.LM.extSIDToObject.getMapping(rateWasteIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, retailLM.getLPByName("itemGroupSku").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, BL.LM.extSID.getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, retailLM.getLPByName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(unitOfMeasureIDField, BL.LM.extSID.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, BL.LM.name.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, retailLM.getLPByName("shortName").getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(unitOfMeasureIDField, retailLM.getLPByName("unitOfMeasureItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("unitOfMeasure")).getMapping(unitOfMeasureKey)));

        props.add(new ImportProperty(nameBrandField, BL.LM.name.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, BL.LM.extSID.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, retailLM.getLPByName("brandItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(countryIDField, retailLM.getLPByName("extSIDCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, BL.LM.name.getMapping(countryKey)));
        props.add(new ImportProperty(countryIDField, retailLM.getLPByName("countryItem").getMapping(itemKey),
                BL.LM.object(BL.LM.country).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, retailLM.getLPByName("barcodeEx").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, retailLM.getLPByName("dateUserBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, retailLM.getLPByName("skuBarcode").getMapping(barcodeKey),
                BL.LM.object(retailLM.getClassByName("item")).getMapping(itemKey)));

        props.add(new ImportProperty(importerPriceField, retailLM.getLPByName("importerPriceItemDate").getMapping(itemKey, dateField)));
        props.add(new ImportProperty(percentWholesaleMarkItemField, retailLM.getLPByName("percentWholesaleMarkItem").getMapping(itemKey)));
        props.add(new ImportProperty(isFixPriceItemField, retailLM.getLPByName("isFixPriceItem").getMapping(itemKey)));
        props.add(new ImportProperty(isLoafCutItemField, retailLM.getLPByName("isLoafCutItem").getMapping(itemKey)));
        props.add(new ImportProperty(isWeightItemField, retailLM.getLPByName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, retailLM.getLPByName("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(dataSuppliersRangeItemField, retailLM.getLPByName("suppliersRangeItemDate").getMapping(itemKey, dateField, supplierRangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(supplierRangeKey)));
        props.add(new ImportProperty(dataRetailRangeItemField, retailLM.getLPByName("retailRangeItemDate").getMapping(itemKey, dateField, retailRangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(retailRangeKey)));
        props.add(new ImportProperty(quantityPackItemField, retailLM.getLPByName("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, retailLM.getLPByName("wareItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("ware")).getMapping(wareKey)));

        props.add(new ImportProperty(priceWareField, retailLM.getLPByName("priceWareDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, retailLM.getLPByName("rangeWareDate").getMapping(wareKey, dateField, rangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(rateWasteIDField, retailLM.getLPByName("rateWasteItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("rateWaste")).getMapping(rateWasteKey)));

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, unitOfMeasureIDField,
                nameUnitOfMeasureField, nameBrandField, brandIDField, countryIDField, nameCountryField, barcodeField, dateField,
                importerPriceField, percentWholesaleMarkItemField, isFixPriceItemField, isLoafCutItemField, isWeightItemField,
                compositionField, dataSuppliersRangeItemField, dataRetailRangeItemField, quantityPackItemField, wareIDField,
                priceWareField, ndsWareField, rateWasteIDField), data);

        DataSession session = BL.createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, unitOfMeasureKey,
                brandKey, countryKey, barcodeKey, supplierRangeKey, retailRangeKey, wareKey, rangeKey, rateWasteKey), props);
        service.synchronize(true, false);
        if (session.hasChanges()) {
            String result = session.apply(BL);
            if (result != null)
                context.addAction(new MessageClientAction(result, "Ошибка"));
        }
        session.close();
    }

    private void importPrices(String path, ExecutionContext context, Integer numberOfItems) throws SQLException {

        try {
            List<List<Object>> data = importPricesFromDBF(path, numberOfItems);

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
            ImportField dateField = new ImportField(BL.LM.date);
            ImportField priceField = new ImportField(retailLM.getLPByName("retailPriceItemDepartmentOver"));
            ImportField markupField = new ImportField(retailLM.getLPByName("markupItemDepartmentOver"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(priceField, retailLM.getLPByName("retailPriceItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));
            props.add(new ImportProperty(markupField, retailLM.getLPByName("markupItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));

            ImportTable table = new ImportTable(Arrays.asList(itemIDField, departmentStoreIDField, dateField, priceField, markupField/*priceWareField, ndsWareField*/), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, departmentStoreKey/*, wareKey, rangeKey*/), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void importShipment(String path, ExecutionContext context, Integer numberOfShipments) throws SQLException {

        try {
            List<List<Object>> data = importShipmentFromDBF(path, numberOfShipments);

            ImportField shipmentField = new ImportField(BL.LM.extSID);
            ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
            ImportField supplierIDField = new ImportField(BL.LM.extSID);

            ImportField waybillShipmentField = new ImportField(retailLM.getLPByName("numberObject"));
            ImportField seriesWaybillShipmentField = new ImportField(retailLM.getLPByName("seriesObject"));
            ImportField dateShipmentField = new ImportField(retailLM.getLPByName("dateShipment"));

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField shipmentDetailIDField = new ImportField(BL.LM.extSID);
            ImportField quantityShipmentDetailField = new ImportField(retailLM.getLPByName("quantityShipmentDetail"));
            ImportField priceShipmentDetailField = new ImportField(retailLM.getLPByName("priceShipmentDetail"));
            ImportField retailPriceShipmentDetailField = new ImportField(retailLM.getLPByName("retailPriceShipmentDetail"));
            ImportField retailMarkupShipmentDetailField = new ImportField(retailLM.getLPByName("retailMarkupShipmentDetail"));
            ImportField dataSuppliersRangeField = new ImportField(retailLM.getLPByName("dataRate"));
            ImportField dataRetailRangeField = new ImportField(retailLM.getLPByName("dataRate"));

            ImportKey<?> shipmentKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("shipment"),
                    retailLM.getLPByName("numberSeriesToShipment").getMapping(waybillShipmentField, seriesWaybillShipmentField));

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            ImportKey<?> shipmentDetailKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("shipmentDetail"),
                    retailLM.getLPByName("sidNumberSeriesToShipmentDetail").getMapping(shipmentDetailIDField, waybillShipmentField, seriesWaybillShipmentField));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                    retailLM.getLPByName("dataActingRateRangeToRange").getMapping(dataSuppliersRangeField));

            ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                    retailLM.getLPByName("dataActingRateRangeToRange").getMapping(dataRetailRangeField));


            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(waybillShipmentField, retailLM.getLPByName("numberObject").getMapping(shipmentKey)));
            props.add(new ImportProperty(seriesWaybillShipmentField, retailLM.getLPByName("seriesObject").getMapping(shipmentKey)));
            props.add(new ImportProperty(dateShipmentField, retailLM.getLPByName("dateShipment").getMapping(shipmentKey)));
            props.add(new ImportProperty(departmentStoreIDField, retailLM.getLPByName("departmentStoreShipment").getMapping(shipmentKey),
                    BL.LM.object(retailLM.getClassByName("departmentStore")).getMapping(departmentStoreKey)));
            props.add(new ImportProperty(supplierIDField, retailLM.getLPByName("supplierShipment").getMapping(shipmentKey),
                    BL.LM.object(retailLM.getClassByName("supplier")).getMapping(supplierKey)));

            props.add(new ImportProperty(shipmentDetailIDField, retailLM.getLPByName("sidShipmentDetail").getMapping(shipmentDetailKey)));
            props.add(new ImportProperty(quantityShipmentDetailField, retailLM.getLPByName("quantityShipmentDetail").getMapping(shipmentDetailKey)));
            props.add(new ImportProperty(priceShipmentDetailField, retailLM.getLPByName("priceShipmentDetail").getMapping(shipmentDetailKey)));
            props.add(new ImportProperty(retailPriceShipmentDetailField, retailLM.getLPByName("retailPriceShipmentDetail").getMapping(shipmentDetailKey)));
            props.add(new ImportProperty(retailMarkupShipmentDetailField, retailLM.getLPByName("retailMarkupShipmentDetail").getMapping(shipmentDetailKey)));

            props.add(new ImportProperty(dataSuppliersRangeField, retailLM.getLPByName("suppliersRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */supplierRangeKey),
                    BL.LM.object(retailLM.getClassByName("range")).getMapping(supplierRangeKey)));
            props.add(new ImportProperty(dataRetailRangeField, retailLM.getLPByName("retailRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */retailRangeKey),
                    BL.LM.object(retailLM.getClassByName("range")).getMapping(retailRangeKey)));

            props.add(new ImportProperty(itemIDField, retailLM.getLPByName("itemShipmentDetail").getMapping(shipmentDetailKey),
                    BL.LM.object(retailLM.getClassByName("item")).getMapping(itemKey)));

            props.add(new ImportProperty(shipmentField, retailLM.getLPByName("shipmentShipmentDetail").getMapping(shipmentDetailKey),
                    BL.LM.object(retailLM.getClassByName("shipment")).getMapping(shipmentKey)));

            ImportTable table = new ImportTable(Arrays.asList(waybillShipmentField, seriesWaybillShipmentField,
                    departmentStoreIDField, supplierIDField, dateShipmentField, itemIDField, shipmentDetailIDField,
                    quantityShipmentDetailField, priceShipmentDetailField, retailPriceShipmentDetailField,
                    retailMarkupShipmentDetailField, dataSuppliersRangeField, dataRetailRangeField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(shipmentKey, supplierKey, departmentStoreKey, shipmentDetailKey, itemKey, supplierRangeKey, retailRangeKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void importAssortment(String path, ExecutionContext context, Integer numberOfItems) throws SQLException {

        try {
            List<List<Object>> data = importAssortmentFromDBF(path, numberOfItems);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField supplierIDField = new ImportField(BL.LM.extSID);
            ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
            ImportField isSupplierItemDepartmentField = new ImportField(BL.LM.name);
            ImportField priceSupplierItemDepartmentField = new ImportField(retailLM.getLPByName("priceSupplierItemDepartmentOver"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            ImportKey<?> logicalKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("yesNo"),
                    retailLM.getLPByName("classSIDToYesNo").getMapping(isSupplierItemDepartmentField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(priceSupplierItemDepartmentField, retailLM.getLPByName("priceSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate)));
            props.add(new ImportProperty(isSupplierItemDepartmentField, retailLM.getLPByName("isSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate),
                    BL.LM.object(retailLM.getClassByName("yesNo")).getMapping(logicalKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemIDField, supplierIDField, departmentStoreIDField, isSupplierItemDepartmentField, priceSupplierItemDepartmentField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, itemKey, departmentStoreKey, logicalKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importCompanies(String path, Boolean importInactive, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ЮР");

            ImportField companyIDField = new ImportField(BL.LM.extSID);
            ImportField nameLegalEntityField = new ImportField(BL.LM.name);
            ImportField legalAddressField = new ImportField(BL.LM.name);
            ImportField unpField = new ImportField(retailLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLPByName("dataAccount"));

            ImportField tradingNetworkIDField = new ImportField(BL.LM.extSID);
            ImportField nameTradingNetworkField = new ImportField(BL.LM.name);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> tradingNetworkKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("tradingNetwork"),
                    BL.LM.extSIDToObject.getMapping(tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(companyIDField, BL.LM.extSID.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLPByName("fullNameLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLPByName("addressLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLPByName("UNPLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLPByName("OKPOLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLPByName("phoneLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLPByName("emailLegalEntity").getMapping(companyKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("ownershipLegalEntity").getMapping(companyKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(companyIDField, retailLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("company")).getMapping(companyKey)));

            props.add(new ImportProperty(tradingNetworkIDField, BL.LM.extSID.getMapping(tradingNetworkKey)));
            props.add(new ImportProperty(nameTradingNetworkField, BL.LM.name.getMapping(tradingNetworkKey)));

            ImportTable table = new ImportTable(Arrays.asList(companyIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, tradingNetworkIDField, nameTradingNetworkField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(companyKey, ownershipKey, accountKey, tradingNetworkKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }

            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importSuppliers(String path, Boolean importInactive, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ПС");

            ImportField supplierIDField = new ImportField(BL.LM.extSID);
            ImportField nameLegalEntityField = new ImportField(BL.LM.name);
            ImportField legalAddressField = new ImportField(BL.LM.name);
            ImportField unpField = new ImportField(retailLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(supplierIDField, BL.LM.extSID.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLPByName("fullNameLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLPByName("addressLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLPByName("UNPLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLPByName("OKPOLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLPByName("phoneLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLPByName("emailLegalEntity").getMapping(supplierKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("ownershipLegalEntity").getMapping(supplierKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(supplierIDField, retailLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("supplier")).getMapping(supplierKey)));
            props.add(new ImportProperty(bankIDField, retailLM.getLPByName("bankAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(supplierIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importCustomers(String path, Boolean importInactive, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ПК");

            ImportField customerIDField = new ImportField(BL.LM.extSID);
            ImportField nameLegalEntityField = new ImportField(BL.LM.name);
            ImportField legalAddressField = new ImportField(BL.LM.name);
            ImportField unpField = new ImportField(retailLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("customer"),
                    BL.LM.extSIDToObject.getMapping(customerIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(customerIDField, BL.LM.extSID.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLPByName("fullNameLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLPByName("addressLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLPByName("UNPLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLPByName("OKPOLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLPByName("phoneLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLPByName("emailLegalEntity").getMapping(customerKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLPByName("ownershipLegalEntity").getMapping(customerKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(customerIDField, retailLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("customer")).getMapping(customerKey)));
            props.add(new ImportProperty(bankIDField, retailLM.getLPByName("bankAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(customerIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(customerKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importStores(String path, Boolean importInactive, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "МГ");

            ImportField storeIDField = new ImportField(BL.LM.extSID);
            ImportField nameStoreField = new ImportField(BL.LM.name);
            ImportField addressStoreField = new ImportField(BL.LM.name);
            ImportField companyIDField = new ImportField(BL.LM.extSID);
            ImportField tradingNetworkIDField = new ImportField(BL.LM.extSID);
            ImportField storeTypeField = new ImportField(BL.LM.name);

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("store"),
                    BL.LM.extSIDToObject.getMapping(storeIDField));

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> tradingNetworkKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("tradingNetwork"),
                    BL.LM.extSIDToObject.getMapping(tradingNetworkIDField));

            ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("storeType"),
                    retailLM.getLPByName("nameToStoreType").getMapping(storeTypeField, tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, BL.LM.extSID.getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, BL.LM.name.getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, retailLM.getLPByName("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, retailLM.getLPByName("companyStore").getMapping(storeKey),
                    BL.LM.object(retailLM.getClassByName("company")).getMapping(companyKey)));

            props.add(new ImportProperty(storeTypeField, BL.LM.name.getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, retailLM.getLPByName("storeTypeStore").getMapping(storeKey),
                    BL.LM.object(retailLM.getClassByName("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(tradingNetworkIDField, retailLM.getLPByName("tradingNetworkStoreType").getMapping(storeTypeKey),
                    BL.LM.object(retailLM.getClassByName("tradingNetwork")).getMapping(tradingNetworkKey)));

            ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, companyIDField, storeTypeField, tradingNetworkIDField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, companyKey, tradingNetworkKey, storeTypeKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importStocks(String path, String storesPath, Boolean importInactive, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importDepartmentStoresFromDBF(path, importInactive, storesPath);

            ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
            ImportField nameDepartmentStoreField = new ImportField(BL.LM.name);
            ImportField storeIDField = new ImportField(BL.LM.extSID);

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("store"),
                    BL.LM.extSIDToObject.getMapping(storeIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(departmentStoreIDField, BL.LM.extSID.getMapping(departmentStoreKey)));
            props.add(new ImportProperty(nameDepartmentStoreField, BL.LM.name.getMapping(departmentStoreKey)));
            props.add(new ImportProperty(storeIDField, retailLM.getLPByName("storeDepartmentStore").getMapping(departmentStoreKey),
                    BL.LM.object(retailLM.getClassByName("store")).getMapping(storeKey)));


            ImportTable table = new ImportTable(Arrays.asList(departmentStoreIDField, nameDepartmentStoreField, storeIDField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importBanks(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importBanksFromDBF(path);

            ImportField bankIDField = new ImportField(BL.LM.extSID);
            ImportField nameBankField = new ImportField(BL.LM.name);
            ImportField addressBankField = new ImportField(BL.LM.name);
            ImportField departmentBankField = new ImportField(retailLM.getLPByName("departmentBank"));
            ImportField mfoBankField = new ImportField(retailLM.getLPByName("MFOBank"));
            ImportField cbuBankField = new ImportField(retailLM.getLPByName("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, BL.LM.extSID.getMapping(bankKey)));
            props.add(new ImportProperty(nameBankField, BL.LM.name.getMapping(bankKey)));
            props.add(new ImportProperty(addressBankField, retailLM.getLPByName("addressBankDate").getMapping(bankKey, defaultDate)));
            props.add(new ImportProperty(departmentBankField, retailLM.getLPByName("departmentBank").getMapping(bankKey)));
            props.add(new ImportProperty(mfoBankField, retailLM.getLPByName("MFOBank").getMapping(bankKey)));
            props.add(new ImportProperty(cbuBankField, retailLM.getLPByName("CBUBank").getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(bankIDField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importRateWastes(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importRateWastesFromDBF(path);

            ImportField rateWasteIDField = new ImportField(BL.LM.extSID);
            ImportField nameRateWasteField = new ImportField(BL.LM.name);
            ImportField percentRateWasteField = new ImportField(retailLM.getLPByName("percentRateWaste"));

            ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("rateWaste"),
                    BL.LM.extSIDToObject.getMapping(rateWasteIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(rateWasteIDField, BL.LM.extSID.getMapping(rateWasteKey)));
            props.add(new ImportProperty(nameRateWasteField, BL.LM.name.getMapping(rateWasteKey)));
            props.add(new ImportProperty(percentRateWasteField, retailLM.getLPByName("percentRateWaste").getMapping(rateWasteKey)));

            ImportTable table = new ImportTable(Arrays.asList(rateWasteIDField, nameRateWasteField, percentRateWasteField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(rateWasteKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if (result != null)
                    context.addAction(new MessageClientAction(result, "Ошибка"));
            }
            session.close();

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private List<List<Object>> data;

    private List<List<Object>> importItemGroupsFromDBF(String path, Boolean parents) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String group1 = new String(importFile.getField("GROUP1").getBytes(), "Cp1251").trim();
            String group2 = new String(importFile.getField("GROUP2").getBytes(), "Cp1251").trim();
            String group3 = new String(importFile.getField("GROUP3").getBytes(), "Cp1251").trim();

            if ((!"".equals(group1)) && (!"".equals(group2)) && (!"".equals(group3))) {
                if (!parents) {
                    //sid - name
                    addIfNotContains(Arrays.asList((Object) group3, group3));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3)), group2));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)), group1));
                    addIfNotContains(Arrays.asList((Object) k_grtov, pol_naim));
                } else {
                    //sid - parentSID
                    addIfNotContains(Arrays.asList((Object) group3, null));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3)), group3));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)), group2 + "/" + group3.substring(0, 3)));
                    addIfNotContains(Arrays.asList((Object) k_grtov, group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)));
                }
            }
        }
        return data;
    }

    private List<List<Object>> importWaresFromDBF(String path) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();
        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {
            importFile.read();

            Boolean isWare = "T".equals(new String(importFile.getField("LGRMSEC").getBytes(), "Cp1251").trim());
            String wareID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();

            if (!"".equals(wareID) && isWare)
                data.add(Arrays.asList((Object) wareID, pol_naim));
        }
        return data;
    }

    private List<List<Object>> importItemsFromDBF(String itemsPath, String quantityPath, String warePath, Boolean importInactive,
                                                  Integer numberOfItems, Integer startItem) throws IOException, xBaseJException {

        DBF wareImportFile = new DBF(warePath);
        int totalRecordCount = wareImportFile.getRecordCount();

        Map<String, Double[]> wares = new HashMap<String, Double[]>();

        for (int i = 0; i < totalRecordCount; i++) {
            wareImportFile.read();

            String itemID = new String(wareImportFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            Double priceWare = new Double(new String(wareImportFile.getField("CENUOSEC").getBytes(), "Cp1251").trim());
            Double ndsWare = new Double(new String(wareImportFile.getField("NDSSEC").getBytes(), "Cp1251").trim());

            if (!wares.containsKey(itemID) && (priceWare != 0 || ndsWare != 0)) {
                wares.put(itemID, new Double[]{priceWare, ndsWare});
            }
        }


        DBF quantityImportFile = new DBF(quantityPath);
        totalRecordCount = quantityImportFile.getRecordCount();

        Map<String, Double> quantities = new HashMap<String, Double>();

        for (int i = 0; i < totalRecordCount; i++) {
            quantityImportFile.read();

            String itemID = new String(quantityImportFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            Double quantityPackItem = new Double(new String(quantityImportFile.getField("PACKSIZE").getBytes(), "Cp1251").trim());

            if (!quantities.containsKey(itemID) || quantityPackItem != 0) {
                quantities.put(itemID, quantityPackItem);
            }
        }

        DBF itemsImportFile = new DBF(itemsPath);
        totalRecordCount = itemsImportFile.getRecordCount() - startItem;
        if (totalRecordCount <= 0) {
            return null;
        }

        data = new ArrayList<List<Object>>();

        Set<String> barcodes = new HashSet<String>();

        for (int j = 0; j < startItem; j++) {
            itemsImportFile.read();
        }

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            itemsImportFile.read();
            try {
                String barcode = new String(itemsImportFile.getField("K_GRUP").getBytes(), "Cp1251").trim();
                int counter = 1;
                if (barcodes.contains(barcode)) {
                    while (barcodes.contains(barcode + "_" + counter)) {
                        counter++;
                    }
                    barcode += "_" + counter;
                }
                barcodes.add(barcode);
                Boolean inactiveItem = "T".equals(new String(itemsImportFile.getField("LINACTIVE").getBytes(), "Cp1251"));
                String itemID = new String(itemsImportFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
                String pol_naim = new String(itemsImportFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String k_grtov = new String(itemsImportFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
                String unitOfMeasure = new String(itemsImportFile.getField("K_IZM").getBytes(), "Cp1251").trim();
                String brand = new String(itemsImportFile.getField("BRAND").getBytes(), "Cp1251").trim();
                String country = new String(itemsImportFile.getField("MANFR").getBytes(), "Cp1251").trim();
                String dateString = new String(itemsImportFile.getField("P_TIME").getBytes(), "Cp1251").trim();
                Date date = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
                Double importerPrice = new Double(new String(itemsImportFile.getField("N_IZG").getBytes(), "Cp1251").trim());
                Double percentWholesaleMarkItem = new Double(new String(itemsImportFile.getField("N_OPT").getBytes(), "Cp1251").trim());
                Boolean isFixPriceItem = "T".equals(new String(itemsImportFile.getField("LFIXEDPRC").getBytes(), "Cp1251").substring(0, 1));
                Boolean isLoafCutItem = "T".equals(new String(itemsImportFile.getField("LSPLIT").getBytes(), "Cp1251").substring(0, 1));
                Boolean isWeightItem = "T".equals(new String(itemsImportFile.getField("LWEIGHT").getBytes(), "Cp1251").substring(0, 1));
                String composition = null;
                if (itemsImportFile.getField("FORMULA").getBytes() != null) {
                    composition = new String(itemsImportFile.getField("FORMULA").getBytes(), "Cp1251").replace("\n", "").replace("\r", "");
                }
                Double suppliersRange = new Double(new String(itemsImportFile.getField("NDSP").getBytes(), "Cp1251").trim());
                Double retailRange = new Double(new String(itemsImportFile.getField("NDSR").getBytes(), "Cp1251").trim());
                Double quantityPackItem = quantities.containsKey(itemID) ? quantities.get(itemID) : null;
                Boolean isWare = "T".equals(new String(itemsImportFile.getField("LGRMSEC").getBytes(), "Cp1251").substring(0, 1));
                String wareID = new String(itemsImportFile.getField("K_GRMSEC").getBytes(), "Cp1251").trim();
                String rateWasteID = "RW_" + new String(itemsImportFile.getField("K_VGRTOV").getBytes(), "Cp1251").trim();

                if (!"".equals(k_grtov) && (!inactiveItem || importInactive) && !isWare)
                    data.add(Arrays.asList((Object) itemID, k_grtov, pol_naim, "U_" + unitOfMeasure, unitOfMeasure, brand, "B_" + brand, "C_" + country, country, barcode,
                            date, importerPrice, percentWholesaleMarkItem, isFixPriceItem ? isFixPriceItem : null, isLoafCutItem ? isLoafCutItem : null, isWeightItem ? isWeightItem : null,
                            "".equals(composition) ? null : composition, suppliersRange, retailRange, quantityPackItem, wareID,
                            wares.containsKey(itemID) ? wares.get(itemID)[0] : null, wares.containsKey(itemID) ? wares.get(itemID)[1] : null,
                            "RW_".equals(rateWasteID) ? null : rateWasteID));
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return data;
    }

    private List<List<Object>> importPricesFromDBF(String path, Integer numberOfItems) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            try {
                importFile.read();
                String item = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
                String departmentStore = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
                Date date = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_CEN").getBytes(), "Cp1251").trim(), new String[]{"yyyyMMdd"}).getTime());
                Double price = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
                Double markup = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());

                data.add(Arrays.asList((Object) item, departmentStore, date, price, markup));
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return data;
    }

    private List<List<Object>> importShipmentFromDBF(String path, Integer numberOfShipments) throws
            IOException, xBaseJException, ParseException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfShipments != 0 && numberOfShipments < totalRecordCount) ? numberOfShipments : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            importFile.read();

            String post_dok[] = new String(importFile.getField("POST_DOK").getBytes(), "Cp1251").trim().split("-");
            String number = post_dok[0];
            String series = post_dok.length == 1 ? null : post_dok[1];
            String departmentStoreID = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            String supplierID = new String(importFile.getField("K_POST").getBytes(), "Cp1251").trim();
            String dateString = new String(importFile.getField("D_PRIH").getBytes(), "Cp1251").trim();
            Date dateShipment = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
            String itemID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String shipmentDetailID = "SD_" + itemID;
            Double quantityShipmentDetail = new Double(new String(importFile.getField("N_MAT").getBytes(), "Cp1251").trim());
            Double priceShipmentDetail = new Double(new String(importFile.getField("N_IZG").getBytes(), "Cp1251").trim());
            Double retailPriceShipmentDetail = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            Double retailMarkupShipmentDetail = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());
            Double suppliersRange = new Double(new String(importFile.getField("NDSP").getBytes(), "Cp1251").trim());
            Double retailRange = new Double(new String(importFile.getField("NDSR").getBytes(), "Cp1251").trim());

            if (post_dok.length != 1)
                data.add(Arrays.asList((Object) number, series, departmentStoreID, supplierID, dateShipment, itemID,
                        shipmentDetailID, quantityShipmentDetail, priceShipmentDetail, retailPriceShipmentDetail,
                        retailMarkupShipmentDetail, suppliersRange, retailRange));
        }
        return data;
    }


    private List<List<Object>> importAssortmentFromDBF(String path, Integer numberOfItems) throws
            IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            importFile.read();
            String item = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String supplier = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String departmentStore = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            Double price = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());

            if (departmentStore.length() >= 2) {
                data.add(Arrays.asList((Object) item, supplier, "СК" + departmentStore.substring(2, departmentStore.length()), "yes", price));
            }
        }
        return data;
    }

    String[][] ownershipsList = new String[][]{
            {"ОАОТ", "Открытое акционерное общество торговое"},
            {"ОАО", "Открытое акционерное общество"},
            {"СООО", "Совместное общество с ограниченной ответственностью"},
            {"ООО", "Общество с ограниченной ответственностью"},
            {"ОДО", "Общество с дополнительной ответственностью"},
            {"ЗАО", "Закрытое акционерное общество"},
            {"ЧТУП", "Частное торговое унитарное предприятие"},
            {"ЧУТП", "Частное унитарное торговое предприятие"},
            {"ТЧУП", "Торговое частное унитарное предприятие"},
            {"ЧУП", "Частное унитарное предприятие"},
            {"РУП", "Республиканское унитарное предприятие"},
            {"РДУП", "Республиканское дочернее унитарное предприятие"},
            {"УП", "Унитарное предприятие"},
            {"ИП", "Индивидуальный предприниматель"},
            {"СПК", "Сельскохозяйственный производственный кооператив"},
            {"СП", "Совместное предприятие"}};


    private List<List<Object>> importLegalEntitiesFromDBF(String path, Boolean importInactive, String type) throws
            IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            if (type.equals(k_ana.substring(0, 2)) && (!inactiveItem || importInactive)) {
                String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String address = new String(importFile.getField("ADDRESS").getBytes(), "Cp1251").trim();
                String unp = new String(importFile.getField("UNN").getBytes(), "Cp1251").trim();
                String okpo = new String(importFile.getField("OKPO").getBytes(), "Cp1251").trim();
                String phone = new String(importFile.getField("TEL").getBytes(), "Cp1251").trim();
                String email = new String(importFile.getField("EMAIL").getBytes(), "Cp1251").trim();
                String account = new String(importFile.getField("ACCOUNT").getBytes(), "Cp1251").trim();
                String companyStore = new String(importFile.getField("K_JUR").getBytes(), "Cp1251").trim();
                String k_bank = new String(importFile.getField("K_BANK").getBytes(), "Cp1251").trim();
                String[] ownership = getAndTrimOwnershipFromName(name);

                if ("МГ".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                else if ("ПС".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, "BANK_" + k_bank));
                else if ("ЮР".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, k_ana + "ТС", ownership[2]));
                else if ("ПК".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, "BANK_" + k_bank));

            }
        }
        return data;
    }


    private List<List<Object>> importDepartmentStoresFromDBF(String path, Boolean importInactive, String
            pathStores) throws IOException, xBaseJException {

        DBF importStores = new DBF(pathStores);
        Map<String, String> storeDepartmentStoreMap = new HashMap<String, String>();
        for (int i = 0; i < importStores.getRecordCount(); i++) {

            importStores.read();
            storeDepartmentStoreMap.put(new String(importStores.getField("K_SKL").getBytes(), "Cp1251").trim(),
                    new String(importStores.getField("K_SKLP").getBytes(), "Cp1251").trim());
        }

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            if ("СК".equals(k_ana.substring(0, 2)) && (!inactiveItem || importInactive)) {
                String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String store = storeDepartmentStoreMap.get(k_ana);
                String[] ownership = getAndTrimOwnershipFromName(name);
                data.add(Arrays.asList((Object) k_ana, ownership[2], store));
            }
        }
        return data;
    }

    private List<List<Object>> importBanksFromDBF(String path) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_bank = new String(importFile.getField("K_BANK").getBytes(), "Cp1251").trim();
            String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String address = new String(importFile.getField("ADDRESS").getBytes(), "Cp1251").trim();
            String department = new String(importFile.getField("DEPART").getBytes(), "Cp1251").trim();
            String mfo = new String(importFile.getField("K_MFO").getBytes(), "Cp1251").trim();
            String cbu = new String(importFile.getField("CBU").getBytes(), "Cp1251").trim();
            data.add(Arrays.asList((Object) ("BANK_" + k_bank), name, address, department, mfo, cbu));
        }
        return data;
    }

    private List<List<Object>> importRateWastesFromDBF(String path) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String rateWasteID = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            Double coef = new Double(new String(importFile.getField("KOEFF").getBytes(), "Cp1251").trim());
            data.add(Arrays.asList((Object) ("RW_" + rateWasteID), name, coef));
        }
        return data;
    }

    private void addIfNotContains(List<Object> element) {
        if (!data.contains(element))
            data.add(element);
    }

    private String[] getAndTrimOwnershipFromName(String name) {
        String ownershipName = "";
        String ownershipShortName = "";
        for (String[] ownership : ownershipsList) {
            if (name.contains(ownership[0])) {
                ownershipName = ownership[1];
                ownershipShortName = ownership[0];
                name = name.replace(ownership[0], "");
            }
        }
        return new String[]{ownershipShortName, ownershipName, name};
    }

}