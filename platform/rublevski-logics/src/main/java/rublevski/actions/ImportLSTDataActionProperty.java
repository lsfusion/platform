package rublevski.actions;

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
import rublevski.RublevskiBusinessLogics;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;

public class ImportLSTDataActionProperty extends ScriptingActionProperty {
    private ScriptingLogicsModule rublevskiLM;

    public ImportLSTDataActionProperty(RublevskiBusinessLogics BL) {
        super(BL);
        rublevskiLM = BL.getLM();

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        String path = rublevskiLM.getLPByName("importLSTDirectory").read(context).toString().trim();
        if (!"".equals(path)) {
            Boolean importInactive = rublevskiLM.getLPByName("importInactive").read(context) != null;
            if (rublevskiLM.getLPByName("importGroupItems").read(context) != null) {
                importItemGroups(path + "//_sprgrt.dbf", context);
                importParentGroups(path + "//_sprgrt.dbf", context);
            }

            if (rublevskiLM.getLPByName("importWares").read(context) != null) {
                importWares(path + "//_sprgrm.dbf", context);
            }

            if (rublevskiLM.getLPByName("importItems").read(context) != null) {
                Object numberOfItems = rublevskiLM.getLPByName("importNumberItems").read(context);
                Object numberOfItemsAtATime = rublevskiLM.getLPByName("importNumberItemsAtATime").read(context);
                importItems(path + "//_sprgrm.dbf", path + "//_postvar.dbf", path + "//_grmcen.dbf", importInactive, context,
                        numberOfItems == null ? 0 : (Integer) numberOfItems, numberOfItemsAtATime == null ? 5000 : (Integer) numberOfItemsAtATime);
            }

            if (rublevskiLM.getLPByName("importPrices").read(context) != null) {
                Object numberOfItems = rublevskiLM.getLPByName("importNumberItems").read(context);
                importPrices(path + "//_grmcen.dbf", context, numberOfItems == null ? 0 : (Integer) numberOfItems);
            }

            if (rublevskiLM.getLPByName("importAssortment").read(context) != null) {
                Object numberOfItems = rublevskiLM.getLPByName("importNumberItems").read(context);
                importAssortment(path + "//_strvar.dbf", context, numberOfItems == null ? 0 : (Integer) numberOfItems);
            }

            if (rublevskiLM.getLPByName("importCompanies").read(context) != null)
                importCompanies(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importSuppliers").read(context) != null)
                importSuppliers(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importCustomers").read(context) != null)
                importCustomers(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importStores").read(context) != null)
                importStores(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importDepartmentStores").read(context) != null)
                importStocks(path + "//_sprana.dbf", path + "//_storestr.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importBanks").read(context) != null)
                importBanks(path + "//_sprbank.dbf", context);
        }

    }

    private void importParentGroups(String path, ExecutionContext context) {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(BL.LM.extSID);
            ImportField parentGroupID = new ImportField(BL.LM.extSID);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(parentGroupID));

            List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
            propsParent.add(new ImportProperty(parentGroupID, rublevskiLM.getLPByName("parentItemGroup").getMapping(itemGroupKey),
                    BL.LM.object(rublevskiLM.getClassByName("itemGroup")).getMapping(parentGroupKey)));
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

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
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

            ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ware"),
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

            for (int i = 0; i < amountOfImportIterations; i++) {
                importPackOfItems(itemsPath, quantityPath, warePath, importInactive, context, numberOfItemsAtATime, i);
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

        ImportField itemIDField = new ImportField(BL.LM.extSID);
        ImportField itemGroupIDField = new ImportField(BL.LM.extSID);
        ImportField itemCaptionField = new ImportField(BL.LM.name);
        ImportField unitOfMeasureIDField = new ImportField(BL.LM.name);
        ImportField nameUnitOfMeasureField = new ImportField(BL.LM.name);
        ImportField brandIDField = new ImportField(BL.LM.name);
        ImportField nameBrandField = new ImportField(BL.LM.name);
        ImportField countryIDField = new ImportField(rublevskiLM.getLPByName("extSIDCountry"));
        ImportField nameCountryField = new ImportField(BL.LM.name);
        ImportField barcodeField = new ImportField(rublevskiLM.getLPByName("barcodeEx"));
        ImportField dateField = new ImportField(BL.LM.date);
        ImportField importerPriceField = new ImportField(rublevskiLM.getLPByName("importerPriceItemDate"));
        ImportField percentWholesaleMarkItemField = new ImportField(rublevskiLM.getLPByName("percentWholesaleMarkItem"));
        ImportField isFixPriceItemField = new ImportField(rublevskiLM.getLPByName("isFixPriceItem"));
        ImportField isLoafCutItemField = new ImportField(rublevskiLM.getLPByName("isLoafCutItem"));
        ImportField isWeightItemField = new ImportField(rublevskiLM.getLPByName("isWeightItem"));
        ImportField compositionField = new ImportField(rublevskiLM.getLPByName("compositionScalesItem"));
        ImportField dataSuppliersRangeItemField = new ImportField(rublevskiLM.getLPByName("dataSuppliersRangeItemDate"));
        ImportField dataRetailRangeItemField = new ImportField(rublevskiLM.getLPByName("dataRetailRangeItemDate"));
        ImportField quantityPackItemField = new ImportField(rublevskiLM.getLPByName("quantityPackItem"));
        ImportField wareIDField = new ImportField(BL.LM.extSID);
        ImportField priceWareField = new ImportField(rublevskiLM.getLPByName("priceWareDate"));
        ImportField ndsWareField = new ImportField(rublevskiLM.getLPByName("dataActingRateRangeWareDate"));

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("item"),
                BL.LM.extSIDToObject.getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                BL.LM.extSIDToObject.getMapping(itemGroupIDField));

        ImportKey<?> unitOfMeasureKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("unitOfMeasure"),
                BL.LM.extSIDToObject.getMapping(unitOfMeasureIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("brand"),
                BL.LM.extSIDToObject.getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey(BL.LM.country,
                rublevskiLM.getLPByName("extSIDToCountry").getMapping(countryIDField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("barcode"),
                rublevskiLM.getLPByName("barcodeToDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("range"),
                rublevskiLM.getLPByName("dataActingRateRangeToRange").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("range"),
                rublevskiLM.getLPByName("dataActingRateRangeToRange").getMapping(dataRetailRangeItemField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ware"),
                BL.LM.extSIDToObject.getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("range"),
                rublevskiLM.getLPByName("dataActingRateRangeToRange").getMapping(ndsWareField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, rublevskiLM.getLPByName("itemGroupSku").getMapping(itemKey),
                BL.LM.object(rublevskiLM.getClassByName("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, BL.LM.extSID.getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, rublevskiLM.getLPByName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(unitOfMeasureIDField, BL.LM.extSID.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, BL.LM.name.getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(nameUnitOfMeasureField, rublevskiLM.getLPByName("shortName").getMapping(unitOfMeasureKey)));
        props.add(new ImportProperty(unitOfMeasureIDField, rublevskiLM.getLPByName("unitOfMeasureItem").getMapping(itemKey),
                BL.LM.object(rublevskiLM.getClassByName("unitOfMeasure")).getMapping(unitOfMeasureKey)));

        props.add(new ImportProperty(nameBrandField, BL.LM.name.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, BL.LM.extSID.getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, rublevskiLM.getLPByName("brandItem").getMapping(itemKey),
                BL.LM.object(rublevskiLM.getClassByName("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(countryIDField, rublevskiLM.getLPByName("extSIDCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, BL.LM.name.getMapping(countryKey)));
        props.add(new ImportProperty(countryIDField, rublevskiLM.getLPByName("countryItem").getMapping(itemKey),
                BL.LM.object(BL.LM.country).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, rublevskiLM.getLPByName("barcodeEx").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, rublevskiLM.getLPByName("dateUserBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, rublevskiLM.getLPByName("skuBarcode").getMapping(barcodeKey),
                BL.LM.object(rublevskiLM.getClassByName("item")).getMapping(itemKey)));

        props.add(new ImportProperty(importerPriceField, rublevskiLM.getLPByName("importerPriceItemDate").getMapping(itemKey, dateField)));
        props.add(new ImportProperty(percentWholesaleMarkItemField, rublevskiLM.getLPByName("percentWholesaleMarkItem").getMapping(itemKey)));
        props.add(new ImportProperty(isFixPriceItemField, rublevskiLM.getLPByName("isFixPriceItem").getMapping(itemKey)));
        props.add(new ImportProperty(isLoafCutItemField, rublevskiLM.getLPByName("isLoafCutItem").getMapping(itemKey)));
        props.add(new ImportProperty(isWeightItemField, rublevskiLM.getLPByName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, rublevskiLM.getLPByName("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(dataSuppliersRangeItemField, rublevskiLM.getLPByName("suppliersRangeItemDate").getMapping(itemKey, dateField, supplierRangeKey),
                BL.LM.object(rublevskiLM.getClassByName("range")).getMapping(supplierRangeKey)));
        props.add(new ImportProperty(dataRetailRangeItemField, rublevskiLM.getLPByName("retailRangeItemDate").getMapping(itemKey, dateField, retailRangeKey),
                BL.LM.object(rublevskiLM.getClassByName("range")).getMapping(retailRangeKey)));
        props.add(new ImportProperty(quantityPackItemField, rublevskiLM.getLPByName("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, rublevskiLM.getLPByName("wareItem").getMapping(itemKey),
                BL.LM.object(rublevskiLM.getClassByName("ware")).getMapping(wareKey)));

        props.add(new ImportProperty(priceWareField, rublevskiLM.getLPByName("priceWareDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, rublevskiLM.getLPByName(/*"dataActingRateRangeWareDate"*/"rangeWareDate").getMapping(wareKey, dateField, rangeKey),
                BL.LM.object(rublevskiLM.getClassByName("range")).getMapping(rangeKey)));

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, unitOfMeasureIDField,
                nameUnitOfMeasureField, nameBrandField, brandIDField, countryIDField, nameCountryField, barcodeField, dateField,
                importerPriceField, percentWholesaleMarkItemField, isFixPriceItemField, isLoafCutItemField, isWeightItemField,
                compositionField, dataSuppliersRangeItemField, dataRetailRangeItemField, quantityPackItemField, wareIDField, priceWareField, ndsWareField), data);

        DataSession session = BL.createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, unitOfMeasureKey,
                brandKey, countryKey, barcodeKey, supplierRangeKey, retailRangeKey, wareKey, rangeKey), props);
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
            ImportField priceField = new ImportField(rublevskiLM.getLPByName("retailPriceItemDepartmentOver"));
            ImportField markupField = new ImportField(rublevskiLM.getLPByName("markupItemDepartmentOver"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(priceField, rublevskiLM.getLPByName("retailPriceItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));
            props.add(new ImportProperty(markupField, rublevskiLM.getLPByName("markupItemDepartmentOver").getMapping(itemKey, departmentStoreKey, dateField)));

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

    private void importAssortment(String path, ExecutionContext context, Integer numberOfItems) throws SQLException {

        try {
            List<List<Object>> data = importAssortmentFromDBF(path, numberOfItems);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField supplierIDField = new ImportField(BL.LM.extSID);
            ImportField departmentStoreIDField = new ImportField(BL.LM.extSID);
            ImportField isSupplierItemDepartmentField = new ImportField(BL.LM.name);
            ImportField priceSupplierItemDepartmentField = new ImportField(rublevskiLM.getLPByName("priceSupplierItemDepartmentOver"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            ImportKey<?> logicalKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("yesNo"),
                    rublevskiLM.getLPByName("classSIDToYesNo").getMapping(isSupplierItemDepartmentField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(priceSupplierItemDepartmentField, rublevskiLM.getLPByName("priceSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate)));
            props.add(new ImportProperty(isSupplierItemDepartmentField, rublevskiLM.getLPByName("isSupplierItemDepartmentOver").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate),
                    BL.LM.object(rublevskiLM.getClassByName("yesNo")).getMapping(logicalKey)));

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
            ImportField unpField = new ImportField(rublevskiLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(rublevskiLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(rublevskiLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(rublevskiLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(rublevskiLM.getLPByName("dataAccount"));

            ImportField tradingNetworkIDField = new ImportField(BL.LM.extSID);
            ImportField nameTradingNetworkField = new ImportField(BL.LM.name);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ownership"),
                    rublevskiLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("account"),
                    rublevskiLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> tradingNetworkKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("tradingNetwork"),
                    BL.LM.extSIDToObject.getMapping(tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(companyIDField, BL.LM.extSID.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, rublevskiLM.getLPByName("fullNameLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(legalAddressField, rublevskiLM.getLPByName("addressLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(unpField, rublevskiLM.getLPByName("UNPLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(okpoField, rublevskiLM.getLPByName("OKPOLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(phoneField, rublevskiLM.getLPByName("phoneLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(emailField, rublevskiLM.getLPByName("emailLegalEntity").getMapping(companyKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("ownershipLegalEntity").getMapping(companyKey),
                    BL.LM.object(rublevskiLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, rublevskiLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(companyIDField, rublevskiLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(rublevskiLM.getClassByName("company")).getMapping(companyKey)));

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
            ImportField unpField = new ImportField(rublevskiLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(rublevskiLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(rublevskiLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(rublevskiLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(rublevskiLM.getLPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ownership"),
                    rublevskiLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("account"),
                    rublevskiLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(supplierIDField, BL.LM.extSID.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, rublevskiLM.getLPByName("fullNameLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(legalAddressField, rublevskiLM.getLPByName("addressLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(unpField, rublevskiLM.getLPByName("UNPLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(okpoField, rublevskiLM.getLPByName("OKPOLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(phoneField, rublevskiLM.getLPByName("phoneLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(emailField, rublevskiLM.getLPByName("emailLegalEntity").getMapping(supplierKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("ownershipLegalEntity").getMapping(supplierKey),
                    BL.LM.object(rublevskiLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, rublevskiLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(supplierIDField, rublevskiLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(rublevskiLM.getClassByName("supplier")).getMapping(supplierKey)));
            props.add(new ImportProperty(bankIDField, rublevskiLM.getLPByName("bankAccount").getMapping(accountKey),
                    BL.LM.object(rublevskiLM.getClassByName("bank")).getMapping(bankKey)));

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
            ImportField unpField = new ImportField(rublevskiLM.getLPByName("UNPLegalEntity"));
            ImportField okpoField = new ImportField(rublevskiLM.getLPByName("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(rublevskiLM.getLPByName("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(BL.LM.name);
            ImportField nameOwnershipField = new ImportField(BL.LM.name);
            ImportField shortNameOwnershipField = new ImportField(rublevskiLM.getLPByName("shortNameOwnership"));
            ImportField accountField = new ImportField(rublevskiLM.getLPByName("dataAccount"));
            ImportField bankIDField = new ImportField(BL.LM.extSID);

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("customer"),
                    BL.LM.extSIDToObject.getMapping(customerIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ownership"),
                    rublevskiLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("account"),
                    rublevskiLM.getLPByName("dataAccountToAccount").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(customerIDField, BL.LM.extSID.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, BL.LM.name.getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, rublevskiLM.getLPByName("fullNameLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(legalAddressField, rublevskiLM.getLPByName("addressLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(unpField, rublevskiLM.getLPByName("UNPLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(okpoField, rublevskiLM.getLPByName("OKPOLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(phoneField, rublevskiLM.getLPByName("phoneLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(emailField, rublevskiLM.getLPByName("emailLegalEntity").getMapping(customerKey)));

            props.add(new ImportProperty(nameOwnershipField, BL.LM.name.getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, rublevskiLM.getLPByName("ownershipLegalEntity").getMapping(customerKey),
                    BL.LM.object(rublevskiLM.getClassByName("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, rublevskiLM.getLPByName("dataAccount").getMapping(accountKey)));
            props.add(new ImportProperty(customerIDField, rublevskiLM.getLPByName("legalEntityAccount").getMapping(accountKey),
                    BL.LM.object(rublevskiLM.getClassByName("customer")).getMapping(customerKey)));
            props.add(new ImportProperty(bankIDField, rublevskiLM.getLPByName("bankAccount").getMapping(accountKey),
                    BL.LM.object(rublevskiLM.getClassByName("bank")).getMapping(bankKey)));

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

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("store"),
                    BL.LM.extSIDToObject.getMapping(storeIDField));

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> tradingNetworkKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("tradingNetwork"),
                    BL.LM.extSIDToObject.getMapping(tradingNetworkIDField));

            ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("storeType"),
                    rublevskiLM.getLPByName("nameToStoreType").getMapping(storeTypeField, tradingNetworkIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, BL.LM.extSID.getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, BL.LM.name.getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, rublevskiLM.getLPByName("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, rublevskiLM.getLPByName("companyStore").getMapping(storeKey),
                    BL.LM.object(rublevskiLM.getClassByName("company")).getMapping(companyKey)));

            props.add(new ImportProperty(storeTypeField, BL.LM.name.getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, rublevskiLM.getLPByName("storeTypeStore").getMapping(storeKey),
                    BL.LM.object(rublevskiLM.getClassByName("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(tradingNetworkIDField, rublevskiLM.getLPByName("tradingNetworkStoreType").getMapping(storeTypeKey),
                    BL.LM.object(rublevskiLM.getClassByName("tradingNetwork")).getMapping(tradingNetworkKey)));

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

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("departmentStore"),
                    BL.LM.extSIDToObject.getMapping(departmentStoreIDField));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("store"),
                    BL.LM.extSIDToObject.getMapping(storeIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(departmentStoreIDField, BL.LM.extSID.getMapping(departmentStoreKey)));
            props.add(new ImportProperty(nameDepartmentStoreField, BL.LM.name.getMapping(departmentStoreKey)));
            props.add(new ImportProperty(storeIDField, rublevskiLM.getLPByName("storeDepartmentStore").getMapping(departmentStoreKey),
                    BL.LM.object(rublevskiLM.getClassByName("store")).getMapping(storeKey)));


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
            ImportField departmentBankField = new ImportField(rublevskiLM.getLPByName("departmentBank"));
            ImportField mfoBankField = new ImportField(rublevskiLM.getLPByName("MFOBank"));
            ImportField cbuBankField = new ImportField(rublevskiLM.getLPByName("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("bank"),
                    BL.LM.extSIDToObject.getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, BL.LM.extSID.getMapping(bankKey)));
            props.add(new ImportProperty(nameBankField, BL.LM.name.getMapping(bankKey)));
            props.add(new ImportProperty(addressBankField, rublevskiLM.getLPByName("addressBankDate").getMapping(bankKey, defaultDate)));
            props.add(new ImportProperty(departmentBankField, rublevskiLM.getLPByName("departmentBank").getMapping(bankKey)));
            props.add(new ImportProperty(mfoBankField, rublevskiLM.getLPByName("MFOBank").getMapping(bankKey)));
            props.add(new ImportProperty(cbuBankField, rublevskiLM.getLPByName("CBUBank").getMapping(bankKey)));

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

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        Set<String> barcodes = new HashSet<String>();

        for (int j = 0; j < startItem; j++)
            itemsImportFile.read();

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
                Date date = new java.sql.Date(DateUtils.parseDate(new String(itemsImportFile.getField("P_TIME").getBytes(), "Cp1251").trim(), new String[]{"yyyyMMdd"}).getTime());
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

                if (!"".equals(k_grtov) && (!inactiveItem || importInactive) && !isWare)
                    data.add(Arrays.asList((Object) itemID, k_grtov, pol_naim, "U_" + unitOfMeasure, unitOfMeasure, brand, "B_" + brand, "C_" + country, country, barcode,
                            date, importerPrice, percentWholesaleMarkItem, isFixPriceItem ? isFixPriceItem : null, isLoafCutItem ? isLoafCutItem : null, isWeightItem ? isWeightItem : null,
                            "".equals(composition) ? null : composition, suppliersRange, retailRange, quantityPackItem,
                            wareID, wares.containsKey(itemID) ? wares.get(itemID)[0] : null, wares.containsKey(itemID) ? wares.get(itemID)[1] : null));
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

            data.add(Arrays.asList((Object) item, supplier, "СК" + departmentStore.substring(2, departmentStore.length()), "yes", price));
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