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
    public void executeCustom(ExecutionContext context) throws SQLException {

        String path = retailLM.getLCPByName("importLSTDirectory").read(context).toString().trim();
        if (!"".equals(path)) {
            Boolean importInactive = retailLM.getLCPByName("importInactive").read(context) != null;
            if (retailLM.getLCPByName("importGroupItems").read(context) != null) {
                importItemGroups(path + "//_sprgrt.dbf", context);
                importParentGroups(path + "//_sprgrt.dbf", context);
            }

            if (retailLM.getLCPByName("importBanks").read(context) != null)
                importBanks(path + "//_sprbank.dbf", context);

            if (retailLM.getLCPByName("importCompanies").read(context) != null)
                importCompanies(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLCPByName("importSuppliers").read(context) != null)
                importSuppliers(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLCPByName("importCustomers").read(context) != null)
                importCustomers(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLCPByName("importStores").read(context) != null)
                importStores(path + "//_sprana.dbf", importInactive, context);

            if (retailLM.getLCPByName("importDepartmentStores").read(context) != null)
                importStocks(path + "//_sprana.dbf", path + "//_storestr.dbf", importInactive, context);

            if (retailLM.getLCPByName("importRateWastes").read(context) != null)
                importRateWastes(path + "//_sprvgrt.dbf", context);

            if (retailLM.getLCPByName("importWares").read(context) != null) {
                importWares(path + "//_sprgrm.dbf", context);
            }

            if (retailLM.getLCPByName("importItems").read(context) != null) {
                Object numberOfItems = retailLM.getLCPByName("importNumberItems").read(context);
                Object numberOfItemsAtATime = retailLM.getLCPByName("importNumberItemsAtATime").read(context);
                importItems(path + "//_sprgrm.dbf", path + "//_postvar.dbf", path + "//_grmcen.dbf", importInactive, context,
                        numberOfItems == null ? 0 : (Integer) numberOfItems, numberOfItemsAtATime == null ? 5000 : (Integer) numberOfItemsAtATime);
            }

            if (retailLM.getLCPByName("importPrices").read(context) != null) {
                importPrices(path + "//_grmcen.dbf", context);
            }

            if (retailLM.getLCPByName("importAssortment").read(context) != null) {
                importAssortment(path + "//_strvar.dbf", context);
            }

            if (retailLM.getLCPByName("importShipment").read(context) != null) {
                importShipment(path + "//_ostn.dbf", context);
            }
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
            propsParent.add(new ImportProperty(parentGroupID, retailLM.getLCPByName("parentItemGroup").getMapping(itemGroupKey),
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

            Set<String> barcodes = new HashSet<String>();
            int amountOfImportIterations = (int) Math.ceil((double) numberOfItems / numberOfItemsAtATime);
            Integer rest = numberOfItems;
            for (int i = 0; i < amountOfImportIterations; i++) {
                importPackOfItems(itemsPath, quantityPath, warePath, importInactive, context, rest > numberOfItemsAtATime ? numberOfItemsAtATime : rest, i, barcodes);
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

    private void importPackOfItems(String itemsPath, String quantityPath, String warePath, Boolean importInactive, ExecutionContext context, Integer numberOfItemsAtATime, int i, Set<String> barcodes) throws SQLException, IOException, xBaseJException {
        List<List<Object>> data = importItemsFromDBF(itemsPath, quantityPath, warePath, importInactive, numberOfItemsAtATime, numberOfItemsAtATime * i, barcodes);
        if (data == null) return;

        ImportField itemIDField = new ImportField(BL.LM.extSID);
        ImportField itemGroupIDField = new ImportField(BL.LM.extSID);
        ImportField itemCaptionField = new ImportField(BL.LM.name);
        ImportField unitOfMeasureIDField = new ImportField(BL.LM.name);
        ImportField nameUnitOfMeasureField = new ImportField(BL.LM.name);
        ImportField brandIDField = new ImportField(BL.LM.name);
        ImportField nameBrandField = new ImportField(BL.LM.name);
        ImportField countryIDField = new ImportField(retailLM.getLCPByName("extSIDCountry"));
        ImportField nameCountryField = new ImportField(BL.LM.name);
        ImportField barcodeField = new ImportField(retailLM.getLCPByName("barcodeEx"));
        ImportField dateField = new ImportField(BL.LM.date);
        ImportField importerPriceField = new ImportField(retailLM.getLCPByName("importerPriceItemDate"));
        ImportField percentWholesaleMarkItemField = new ImportField(retailLM.getLCPByName("percentWholesaleMarkItem"));
        ImportField isFixPriceItemField = new ImportField(retailLM.getLCPByName("isFixPriceItem"));
        ImportField isLoafCutItemField = new ImportField(retailLM.getLCPByName("isLoafCutItem"));
        ImportField isWeightItemField = new ImportField(retailLM.getLCPByName("isWeightItem"));
        ImportField compositionField = new ImportField(retailLM.getLCPByName("compositionScalesItem"));
        ImportField dataSuppliersRangeItemField = new ImportField(retailLM.getLCPByName("dataRate"));
        ImportField dataRetailRangeItemField = new ImportField(retailLM.getLCPByName("dataRate"));
        ImportField quantityPackItemField = new ImportField(retailLM.getLCPByName("quantityPackItem"));
        ImportField wareIDField = new ImportField(BL.LM.extSID);
        ImportField priceWareField = new ImportField(retailLM.getLCPByName("priceWareDate"));
        ImportField ndsWareField = new ImportField(retailLM.getLCPByName("dataRate"));
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
                retailLM.getLCPByName("extSIDToCountry").getMapping(countryIDField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("barcode"),
                retailLM.getLCPByName("barcodeToDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLCPByName("dataActingRateRangeToRange").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLCPByName("dataActingRateRangeToRange").getMapping(dataRetailRangeItemField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ware"),
                BL.LM.extSIDToObject.getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                retailLM.getLCPByName("dataActingRateRangeToRange").getMapping(ndsWareField));

        ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("rateWaste"),
                BL.LM.extSIDToObject.getMapping(rateWasteIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, retailLM.getLCPByName("itemGroupSku").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, BL.LM.extSID.getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, retailLM.getLCPByName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(unitOfMeasureIDField, BL.LM.extSID.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, BL.LM.name.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, retailLM.getLCPByName("shortName").getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(unitOfMeasureIDField, retailLM.getLCPByName("unitOfMeasureItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("unitOfMeasure")).getMapping(unitOfMeasureKey)));

        props.add(new ImportProperty(nameBrandField, BL.LM.name.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, BL.LM.extSID.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, retailLM.getLCPByName("brandItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(countryIDField, retailLM.getLCPByName("extSIDCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, BL.LM.name.getMapping(countryKey)));
        props.add(new ImportProperty(countryIDField, retailLM.getLCPByName("countryItem").getMapping(itemKey),
                BL.LM.object(BL.LM.country).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, retailLM.getLCPByName("barcodeEx").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, retailLM.getLCPByName("dateUserBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, retailLM.getLCPByName("skuBarcode").getMapping(barcodeKey),
                BL.LM.object(retailLM.getClassByName("item")).getMapping(itemKey)));

        props.add(new ImportProperty(importerPriceField, retailLM.getLCPByName("importerPriceItemDate").getMapping(itemKey, dateField)));
        props.add(new ImportProperty(percentWholesaleMarkItemField, retailLM.getLCPByName("percentWholesaleMarkItem").getMapping(itemKey)));
        props.add(new ImportProperty(isFixPriceItemField, retailLM.getLCPByName("isFixPriceItem").getMapping(itemKey)));
        props.add(new ImportProperty(isLoafCutItemField, retailLM.getLCPByName("isLoafCutItem").getMapping(itemKey)));
        props.add(new ImportProperty(isWeightItemField, retailLM.getLCPByName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, retailLM.getLCPByName("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(dataSuppliersRangeItemField, retailLM.getLCPByName("suppliersRangeItemDate").getMapping(itemKey, dateField, supplierRangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(supplierRangeKey)));
        props.add(new ImportProperty(dataRetailRangeItemField, retailLM.getLCPByName("retailRangeItemDate").getMapping(itemKey, dateField, retailRangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(retailRangeKey)));
        props.add(new ImportProperty(quantityPackItemField, retailLM.getLCPByName("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, retailLM.getLCPByName("wareItem").getMapping(itemKey),
                BL.LM.object(retailLM.getClassByName("ware")).getMapping(wareKey)));

        props.add(new ImportProperty(wareIDField, BL.LM.extSID.getMapping(wareKey)));
        props.add(new ImportProperty(priceWareField, retailLM.getLCPByName("priceWareDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, retailLM.getLCPByName("rangeWareDate").getMapping(wareKey, dateField, rangeKey),
                BL.LM.object(retailLM.getClassByName("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(rateWasteIDField, retailLM.getLCPByName("rateWasteItem").getMapping(itemKey),
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

    private void importPrices(String path, ExecutionContext context) throws SQLException {

        try {
            int count = 200000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importPricesFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField itemIDField = new ImportField(BL.LM.extSID);
                ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
                ImportField dateField = new ImportField(BL.LM.date);
                ImportField priceField = new ImportField(retailLM.getLCPByName("retailPriceItemDepartmentOver"));
                ImportField markupField = new ImportField(retailLM.getLCPByName("markupItemDepartmentOver"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                        BL.LM.extSIDToObject.getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                        BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceField, retailLM.getLCPByName("retailPriceItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));
                props.add(new ImportProperty(markupField, retailLM.getLCPByName("markupItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));

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
                System.out.println("done " + start);
            }
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void importShipment(String path, ExecutionContext context) throws SQLException {

        try {
            int count = 20000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importShipmentFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField shipmentField = new ImportField(BL.LM.extSID);
                ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
                ImportField supplierIDField = new ImportField(BL.LM.extSID);

                ImportField waybillShipmentField = new ImportField(retailLM.getLCPByName("numberObject"));
                ImportField seriesWaybillShipmentField = new ImportField(retailLM.getLCPByName("seriesObject"));
                ImportField dateShipmentField = new ImportField(retailLM.getLCPByName("dateShipment"));

                ImportField itemIDField = new ImportField(BL.LM.extSID);
                ImportField shipmentDetailIDField = new ImportField(BL.LM.extSID);
                ImportField quantityShipmentDetailField = new ImportField(retailLM.getLCPByName("quantityShipmentDetail"));
                ImportField supplierPriceShipmentDetail = new ImportField(retailLM.getLCPByName("supplierPriceShipmentDetail"));
                ImportField importerPriceShipmentDetail = new ImportField(retailLM.getLCPByName("importerPriceShipmentDetail"));
                ImportField retailPriceShipmentDetailField = new ImportField(retailLM.getLCPByName("retailPriceShipmentDetail"));
                ImportField retailMarkupShipmentDetailField = new ImportField(retailLM.getLCPByName("retailMarkupShipmentDetail"));
                ImportField dataSuppliersRangeField = new ImportField(retailLM.getLCPByName("dataRate"));
                ImportField dataRetailRangeField = new ImportField(retailLM.getLCPByName("dataRate"));

                ImportKey<?> shipmentKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("shipment"),
                        retailLM.getLCPByName("numberSeriesToShipment").getMapping(waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                        BL.LM.extSIDToObject.getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                        BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

                ImportKey<?> shipmentDetailKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("shipmentDetail"),
                        retailLM.getLCPByName("sidNumberSeriesToShipmentDetail").getMapping(shipmentDetailIDField, waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                        BL.LM.extSIDToObject.getMapping(itemIDField));

                ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                        retailLM.getLCPByName("dataActingRateRangeToRange").getMapping(dataSuppliersRangeField));

                ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("range"),
                        retailLM.getLCPByName("dataActingRateRangeToRange").getMapping(dataRetailRangeField));


                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(waybillShipmentField, retailLM.getLCPByName("numberObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(seriesWaybillShipmentField, retailLM.getLCPByName("seriesObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(dateShipmentField, retailLM.getLCPByName("dateShipment").getMapping(shipmentKey)));
                props.add(new ImportProperty(departmentStoreIDField, retailLM.getLCPByName("departmentStoreShipment").getMapping(shipmentKey),
                        BL.LM.object(retailLM.getClassByName("departmentStore")).getMapping(departmentStoreKey)));
                props.add(new ImportProperty(supplierIDField, retailLM.getLCPByName("supplierShipment").getMapping(shipmentKey),
                        BL.LM.object(retailLM.getClassByName("supplier")).getMapping(supplierKey)));

                props.add(new ImportProperty(shipmentDetailIDField, retailLM.getLCPByName("sidShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(quantityShipmentDetailField, retailLM.getLCPByName("quantityShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(supplierPriceShipmentDetail, retailLM.getLCPByName("supplierPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(importerPriceShipmentDetail, retailLM.getLCPByName("importerPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailPriceShipmentDetailField, retailLM.getLCPByName("retailPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailMarkupShipmentDetailField, retailLM.getLCPByName("retailMarkupShipmentDetail").getMapping(shipmentDetailKey)));

                props.add(new ImportProperty(dataSuppliersRangeField, retailLM.getLCPByName("suppliersRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */supplierRangeKey),
                        BL.LM.object(retailLM.getClassByName("range")).getMapping(supplierRangeKey)));
                props.add(new ImportProperty(dataRetailRangeField, retailLM.getLCPByName("retailRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */retailRangeKey),
                        BL.LM.object(retailLM.getClassByName("range")).getMapping(retailRangeKey)));

                props.add(new ImportProperty(itemIDField, retailLM.getLCPByName("itemShipmentDetail").getMapping(shipmentDetailKey),
                        BL.LM.object(retailLM.getClassByName("item")).getMapping(itemKey)));

                props.add(new ImportProperty(shipmentField, retailLM.getLCPByName("shipmentShipmentDetail").getMapping(shipmentDetailKey),
                        BL.LM.object(retailLM.getClassByName("shipment")).getMapping(shipmentKey)));

                ImportTable table = new ImportTable(Arrays.asList(waybillShipmentField, seriesWaybillShipmentField,
                        departmentStoreIDField, supplierIDField, dateShipmentField, itemIDField, shipmentDetailIDField,
                        quantityShipmentDetailField, supplierPriceShipmentDetail, importerPriceShipmentDetail, retailPriceShipmentDetailField,
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
            }

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

    private void importAssortment(String path, ExecutionContext context) throws SQLException {

        try {

            int count = 50000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importAssortmentFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportField itemIDField = new ImportField(BL.LM.extSID);
                ImportField supplierIDField = new ImportField(BL.LM.extSID);
                ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
                ImportField isSupplierItemDepartmentField = new ImportField(BL.LM.name);
                ImportField priceSupplierItemDepartmentField = new ImportField(retailLM.getLCPByName("priceSupplierItemDepartmentOver"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"),
                        BL.LM.extSIDToObject.getMapping(itemIDField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                        BL.LM.extSIDToObject.getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("departmentStore"),
                        BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

                ImportKey<?> logicalKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("yesNo"),
                        retailLM.getLCPByName("classSIDToYesNo").getMapping(isSupplierItemDepartmentField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceSupplierItemDepartmentField, retailLM.getLCPByName("priceSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate)));
                props.add(new ImportProperty(isSupplierItemDepartmentField, retailLM.getLCPByName("isSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate),
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
            }

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
            ImportField unpField = new ImportField(retailLM.getLCPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLCPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLCPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLCPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLCPByName("dataAccount"));

            ImportField tradingNetworkIDField = new ImportField(BL.LM.extSID);
            ImportField nameTradingNetworkField = new ImportField(BL.LM.name);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLCPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLCPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> tradingNetworkKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("tradingNetwork"),
                    BL.LM.extSIDToObject.getMapping(tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(companyIDField, BL.LM.extSID.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLCPByName("fullNameLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLCPByName("addressLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLCPByName("UNPLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLCPByName("OKPOLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLCPByName("phoneLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLCPByName("emailLegalEntity").getMapping(companyKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("ownershipLegalEntity").getMapping(companyKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLCPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(companyIDField, retailLM.getLCPByName("legalEntityAccount").getMapping(accountKey),
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
            ImportField unpField = new ImportField(retailLM.getLCPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLCPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLCPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLCPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLCPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLCPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLCPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(supplierIDField, BL.LM.extSID.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLCPByName("fullNameLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLCPByName("addressLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLCPByName("UNPLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLCPByName("OKPOLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLCPByName("phoneLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLCPByName("emailLegalEntity").getMapping(supplierKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("ownershipLegalEntity").getMapping(supplierKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLCPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(supplierIDField, retailLM.getLCPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("supplier")).getMapping(supplierKey)));
            props.add(new ImportProperty(bankIDField, retailLM.getLCPByName("bankAccount").getMapping(accountKey),
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
            ImportField unpField = new ImportField(retailLM.getLCPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(retailLM.getLCPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(retailLM.getLCPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(retailLM.getLCPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(retailLM.getLCPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("customer"),
                    BL.LM.extSIDToObject.getMapping(customerIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("ownership"),
                    retailLM.getLCPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("account"),
                    retailLM.getLCPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(customerIDField, BL.LM.extSID.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, retailLM.getLCPByName("fullNameLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(legalAddressField, retailLM.getLCPByName("addressLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(unpField, retailLM.getLCPByName("UNPLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(okpoField, retailLM.getLCPByName("OKPOLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(phoneField, retailLM.getLCPByName("phoneLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(emailField, retailLM.getLCPByName("emailLegalEntity").getMapping(customerKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, retailLM.getLCPByName("ownershipLegalEntity").getMapping(customerKey),
                    BL.LM.object(retailLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, retailLM.getLCPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(customerIDField, retailLM.getLCPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(retailLM.getClassByName("customer")).getMapping(customerKey)));
            props.add(new ImportProperty(bankIDField, retailLM.getLCPByName("bankAccount").getMapping(accountKey),
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
                    retailLM.getLCPByName("nameToStoreType").getMapping(storeTypeField, tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, BL.LM.extSID.getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, BL.LM.name.getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, retailLM.getLCPByName("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, retailLM.getLCPByName("companyStore").getMapping(storeKey),
                    BL.LM.object(retailLM.getClassByName("company")).getMapping(companyKey)));

            props.add(new ImportProperty(storeTypeField, BL.LM.name.getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, retailLM.getLCPByName("storeTypeStore").getMapping(storeKey),
                    BL.LM.object(retailLM.getClassByName("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(tradingNetworkIDField, retailLM.getLCPByName("tradingNetworkStoreType").getMapping(storeTypeKey),
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
            props.add(new ImportProperty(storeIDField, retailLM.getLCPByName("storeDepartmentStore").getMapping(departmentStoreKey),
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
            ImportField departmentBankField = new ImportField(retailLM.getLCPByName("departmentBank"));
            ImportField mfoBankField = new ImportField(retailLM.getLCPByName("MFOBank"));
            ImportField cbuBankField = new ImportField(retailLM.getLCPByName("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, BL.LM.extSID.getMapping(bankKey)));
            props.add(new ImportProperty(nameBankField, BL.LM.name.getMapping(bankKey)));
            props.add(new ImportProperty(addressBankField, retailLM.getLCPByName("addressBankDate").getMapping(bankKey, defaultDate)));
            props.add(new ImportProperty(departmentBankField, retailLM.getLCPByName("departmentBank").getMapping(bankKey)));
            props.add(new ImportProperty(mfoBankField, retailLM.getLCPByName("MFOBank").getMapping(bankKey)));
            props.add(new ImportProperty(cbuBankField, retailLM.getLCPByName("CBUBank").getMapping(bankKey)));

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
            ImportField percentRateWasteField = new ImportField(retailLM.getLCPByName("percentRateWaste"));

            ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("rateWaste"),
                    BL.LM.extSIDToObject.getMapping(rateWasteIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(rateWasteIDField, BL.LM.extSID.getMapping(rateWasteKey)));
            props.add(new ImportProperty(nameRateWasteField, BL.LM.name.getMapping(rateWasteKey)));
            props.add(new ImportProperty(percentRateWasteField, retailLM.getLCPByName("percentRateWaste").getMapping(rateWasteKey)));

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
                                                  Integer numberOfItems, Integer startItem, Set<String> barcodes) throws IOException, xBaseJException {

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
                if (wareID.isEmpty())
                    wareID = null;
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

    private List<List<Object>> importPricesFromDBF(String path, int start, int count) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < Math.min(totalRecordCount, start + count); i++) {
            try {
                importFile.read();
                if (i < start) continue;
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

    private List<List<Object>> importShipmentFromDBF(String path, int start, int count) throws
            IOException, xBaseJException, ParseException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < Math.min(totalRecordCount, start + count); i++) {
            importFile.read();
            if (i < start) continue;

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
            Double supplierPriceShipmentDetail = new Double(new String(importFile.getField("N_IZG").getBytes(), "Cp1251").trim()); // пока одинаковые
            Double importerPriceShipmentDetail = new Double(new String(importFile.getField("N_IZG").getBytes(), "Cp1251").trim());
            Double retailPriceShipmentDetail = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            Double retailMarkupShipmentDetail = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());
            Double suppliersRange = new Double(new String(importFile.getField("NDSP").getBytes(), "Cp1251").trim());
            Double retailRange = new Double(new String(importFile.getField("NDSR").getBytes(), "Cp1251").trim());

            if (post_dok.length != 1)
                data.add(Arrays.asList((Object) number, series, departmentStoreID, supplierID, dateShipment, itemID,
                        shipmentDetailID, quantityShipmentDetail, supplierPriceShipmentDetail, importerPriceShipmentDetail, retailPriceShipmentDetail,
                        retailMarkupShipmentDetail, suppliersRange, retailRange));
        }
        return data;
    }


    private List<List<Object>> importAssortmentFromDBF(String path, int start, int count) throws
            IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < Math.min(totalRecordCount, start + count); i++) {
            importFile.read();
            if (i < start) continue;

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