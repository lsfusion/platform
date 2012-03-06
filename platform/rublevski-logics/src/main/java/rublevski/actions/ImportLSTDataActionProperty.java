package rublevski.actions;

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
            if (rublevskiLM.getLPByName("importItems").read(context) != null) {
                Object numberOfItems = rublevskiLM.getLPByName("importNumberItems").read(context);
                importItems(path + "//_sprgrm.dbf", importInactive, context, numberOfItems == null ? 0 : (Integer) numberOfItems);
            }

            if (rublevskiLM.getLPByName("importCompanies").read(context) != null)
                importCompanies(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importSuppliers").read(context) != null)
                importSuppliers(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importStores").read(context) != null)
                importStores(path + "//_sprana.dbf", importInactive, context);

            if (rublevskiLM.getLPByName("importDepartmentStores").read(context) != null)
                importStocks(path + "//_sprana.dbf", path + "//_storestr.dbf", importInactive, context);
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

    private void importItems(String path, Boolean importInactive, ExecutionContext context, Integer numberOfItems) throws SQLException {

        try {
            List<List<Object>> data = importItemsFromDBF(path, importInactive, numberOfItems);

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

            props.add(new ImportProperty(barcodeField, rublevskiLM.getLPByName("barcodeEx").getMapping(barcodeKey)));
            props.add(new ImportProperty(dateField, rublevskiLM.getLPByName("dateUserBarcode").getMapping(barcodeKey)));
            props.add(new ImportProperty(barcodeField, rublevskiLM.getLPByName("userBarcodeSku").getMapping(barcodeKey),
                    BL.LM.object(rublevskiLM.getClassByName("item")).getMapping(itemKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, unitOfMeasureIDField, nameUnitOfMeasureField, nameBrandField, brandIDField, countryIDField, nameCountryField, barcodeField, dateField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, unitOfMeasureKey, brandKey, countryKey, barcodeKey), props);
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

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("supplier"),
                    BL.LM.extSIDToObject.getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ownership"),
                    rublevskiLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("account"),
                    rublevskiLM.getLPByName("dataAccountToAccount").getMapping(accountField));


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

            ImportTable table = new ImportTable(Arrays.asList(supplierIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, ownershipKey, accountKey), props);
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

    private List<List<Object>> importItemsFromDBF(String path, Boolean importInactive, Integer numberOfItems) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            String k_grmat = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String unitOfMeasure = new String(importFile.getField("K_IZM").getBytes(), "Cp1251").trim();
            String brand = new String(importFile.getField("BRAND").getBytes(), "Cp1251").trim();
            String country = new String(importFile.getField("MANFR").getBytes(), "Cp1251").trim();
            String barcode = new String(importFile.getField("K_GRUP").getBytes(), "Cp1251").trim();
            if (!"".equals(k_grtov) && (!inactiveItem || importInactive))
                data.add(Arrays.asList((Object) k_grmat, k_grtov, pol_naim, "U_" + unitOfMeasure, unitOfMeasure, brand, "B_" + brand, "C_" + country, country, barcode,
                        new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance));
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


    private List<List<Object>> importLegalEntitiesFromDBF(String path, Boolean importInactive, String type) throws IOException, xBaseJException {

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
                String[] ownership = getAndTrimOwnershipFromName(name);

                if ("МГ".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                else if ("ПС".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account));
                else if ("ЮР".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, k_ana + "ТС", ownership[2]));
            }
        }
        return data;
    }


    private List<List<Object>> importDepartmentStoresFromDBF(String path, Boolean importInactive, String pathStores) throws IOException, xBaseJException {

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