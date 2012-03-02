package rublevski.actions;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.interop.action.MessageClientAction;
import platform.server.Message;
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
        if (rublevskiLM.getLPByName("importGroupItems").read(context) != null) {
            importItemGroups(path + "//_sprgrt.dbf", context);
            importParentGroups(path + "//_sprgrt.dbf", context);
        }
        if (rublevskiLM.getLPByName("importItems").read(context) != null) {
            Object numberOfItems = rublevskiLM.getLPByName("importNumberItems").read(context);
            importItems(path + "//_sprgrm.dbf", context, numberOfItems == null ? 0 : (Integer) numberOfItems);
        }

        if (rublevskiLM.getLPByName("importCompanies").read(context) != null)
            importCompanies(path + "//_sprana.dbf", context);

        if (rublevskiLM.getLPByName("importSuppliers").read(context) != null)
            importSuppliers(path + "//_sprana.dbf", context);

        if (rublevskiLM.getLPByName("importStores").read(context) != null)
            importStores(path + "//_sprana.dbf", context);
        
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
                   if(result!=null)
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
                if(result!=null)
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

    private void importItems(String path,ExecutionContext context, Integer numberOfItems) throws SQLException {

        try {
            List<List<Object>> data = importItemsFromDBF(path, numberOfItems);

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField itemGroupIDField = new ImportField(BL.LM.extSID);
            ImportField itemCaptionField = new ImportField(BL.LM.name);
            ImportField unitOfMeasureField = new ImportField(BL.LM.name);
            ImportField brandField = new ImportField(BL.LM.name);
            ImportField countryField = new ImportField(rublevskiLM.getLPByName("extSIDCountry"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupIDField));

            ImportKey<?> unitOfMeasureKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("unitOfMeasure"),
                    BL.LM.extSIDToObject.getMapping(unitOfMeasureField));

            ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("brand"),
                    BL.LM.extSIDToObject.getMapping(brandField));

            ImportKey<?> countryKey = new ImportKey(BL.LM.country,
                    rublevskiLM.getLPByName("extSIDToCountry").getMapping(countryField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(itemGroupIDField, rublevskiLM.getLPByName("itemGroupSku").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("itemGroup")).getMapping(itemGroupKey)));

            props.add(new ImportProperty(itemIDField, BL.LM.extSID.getMapping(itemKey)));
            props.add(new ImportProperty(itemCaptionField, rublevskiLM.getLPByName("captionItem").getMapping(itemKey)));

            props.add(new ImportProperty(unitOfMeasureField, BL.LM.extSID.getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField, BL.LM.name.getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField, rublevskiLM.getLPByName("shortName").getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField, rublevskiLM.getLPByName("unitOfMeasureItem").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("unitOfMeasure")).getMapping(unitOfMeasureKey)));

            props.add(new ImportProperty(brandField, BL.LM.name.getMapping(brandKey)));
            props.add(new ImportProperty(brandField, BL.LM.extSID.getMapping(brandKey)));
            props.add(new ImportProperty(brandField, rublevskiLM.getLPByName("brandItem").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("brand")).getMapping(brandKey)));

            props.add(new ImportProperty(countryField, rublevskiLM.getLPByName("extSIDCountry").getMapping(countryKey)));
            props.add(new ImportProperty(countryField, BL.LM.name.getMapping(countryKey)));
            props.add(new ImportProperty(countryField, rublevskiLM.getLPByName("countryItem").getMapping(itemKey),
                    BL.LM.object(BL.LM.country).getMapping(countryKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, unitOfMeasureField, brandField, countryField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, unitOfMeasureKey, brandKey, countryKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if(result!=null)
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

    private void importCompanies(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, "ЮР");

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

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("ownership"),
                    rublevskiLM.getLPByName("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("account"),
                    rublevskiLM.getLPByName("dataAccountToAccount").getMapping(accountField));


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

            ImportTable table = new ImportTable(Arrays.asList(companyIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(companyKey, ownershipKey, accountKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if(result!=null)
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

    private void importSuppliers(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, "ПС");

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
                if(result!=null)
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

    private void importStores(String path, ExecutionContext context) throws SQLException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, "МГ");

            ImportField storeIDField = new ImportField(BL.LM.extSID);
            ImportField nameStoreField = new ImportField(BL.LM.name);
            ImportField addressStoreField = new ImportField(BL.LM.name);
            ImportField companyIDField = new ImportField(BL.LM.extSID);

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("store"),
                    BL.LM.extSIDToObject.getMapping(storeIDField));

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("company"),
                    BL.LM.extSIDToObject.getMapping(companyIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, BL.LM.extSID.getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, BL.LM.name.getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, rublevskiLM.getLPByName("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, rublevskiLM.getLPByName("companyStore").getMapping(storeKey),
                    BL.LM.object(rublevskiLM.getClassByName("company")).getMapping(companyKey)));


            ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, companyIDField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, companyKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if(result!=null)
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

    private List<List<Object>> importItemsFromDBF(String path, Integer numberOfItems) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        int recordCount = (numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_grmat = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String unitOfMeasure = new String(importFile.getField("K_IZM").getBytes(), "Cp1251").trim();
            String brand = new String(importFile.getField("BRAND").getBytes(), "Cp1251").trim();
            String country = new String(importFile.getField("MANFR").getBytes(), "Cp1251").trim();

            if (!"".equals(k_grtov))
                data.add(Arrays.asList((Object) k_grmat, k_grtov, pol_naim, unitOfMeasure, brand, country));
        }
        return data;
    }

    private List<List<Object>> importLegalEntitiesFromDBF(String path, String type) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();
        String[][] ownershipsList = new String[][]{
                {"ОАОТ", "Открытое акционерное общество торговое"},
                {"ОАО", "Открытое акционерное общество"},
                {"СООО", "Совместное общество с ограниченной ответственностью"},
                {"ООО", "Общество с ограниченной ответственностью"},
                {"ЗАО", "Закрытое акционерное общество"}};

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String address = new String(importFile.getField("ADDRESS").getBytes(), "Cp1251").trim();
            String unp = new String(importFile.getField("UNN").getBytes(), "Cp1251").trim();
            String okpo = new String(importFile.getField("OKPO").getBytes(), "Cp1251").trim();
            String phone = new String(importFile.getField("TEL").getBytes(), "Cp1251").trim();
            String email = new String(importFile.getField("EMAIL").getBytes(), "Cp1251").trim();
            String account = new String(importFile.getField("ACCOUNT").getBytes(), "Cp1251").trim();
            String companyStore = new String(importFile.getField("K_JUR").getBytes(), "Cp1251").trim();

            if (type.equals(k_ana.substring(0, 2))) {
                String ownershipName = "";
                String ownershipShortName = "";
                for (String[] ownership : ownershipsList) {
                    if (name.contains(ownership[0])) {
                        ownershipName = ownership[1];
                        ownershipShortName = ownership[0];
                        name = name.replace(ownership[0], "");
                    }

                }
                if ("МГ".equals(type))
                    data.add(Arrays.asList((Object) k_ana, name, address, companyStore));
                else
                    data.add(Arrays.asList((Object) k_ana, name, address, unp, okpo, phone, email, ownershipName, ownershipShortName, account));
            }
        }
        return data;
    }

    private void addIfNotContains(List<Object> element) {
        if (!data.contains(element))
            data.add(element);
    }

}