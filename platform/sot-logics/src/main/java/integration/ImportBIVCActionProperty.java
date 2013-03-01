package integration;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportBIVCActionProperty extends ScriptingActionProperty {

    public ImportBIVCActionProperty(ScriptingLogicsModule LM) {
        super(LM);

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            String path = (String) getLCP("importBIVCDirectory").read(context);
            if (path != null && !path.isEmpty()) {

                path = path.trim();

                ImportData importData = new ImportData();

                importData.setNumberOfItemsAtATime((Integer) getLCP("importBIVCNumberItemsAtATime").read(context));

                importData.setItemGroupsList((getLCP("importBIVCGroupItems").read(context) != null) ?
                        importItemGroups(path + "//stmc", false) : null);

                importData.setParentGroupsList((getLCP("importBIVCGroupItems").read(context) != null) ?
                        importItemGroups(path + "//stmc", true) : null);

                importData.setLegalEntitiesList((getLCP("importBIVCLegalEntities").read(context) != null) ?
                        importLegalEntities(path + "//swtp") : null);

                importData.setWarehousesList((getLCP("importBIVCWarehouses").read(context) != null) ?
                        importWarehouses(path + "//smol", path + "//swtp") : null);

                importData.setContractsList((getLCP("importBIVCContracts").read(context) != null) ?
                        importContracts(path + "//swtp") : null);

                importData.setItemsList((getLCP("importBIVCItems").read(context) != null) ?
                        importItems(path + "//stmc", (Integer) getLCP("importBIVCNumberItems").read(context)) : null);

                importData.setUserInvoicesList((getLCP("importBIVCUserInvoices").read(context) != null) ?
                        importUserInvoices(path + "//stmc") : null);

                new ImportActionProperty(LM, importData, context).makeImport();

                if ((getLCP("importBIVCItems").read(context) != null))
                    importSotUOM(path + "//stmc");
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    List<String> itemGroups;
    List<ItemGroup> itemGroupsList;

    private List<ItemGroup> importItemGroups(String path, Boolean parents) throws IOException, xBaseJException {

        itemGroups = new ArrayList<String>();
        itemGroupsList = new ArrayList<ItemGroup>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
        String line;
        String itemID;
        String group1ID = null;
        String group1Name = null;
        String group2ID = null;
        String group2Name = null;
        String group3ID = null;
        String group3Name = null;
        String group4 = "ВСЕ";
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                itemID = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 12:
                        String[] splittedLine = reader.readLine().split(":");
                        group3ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        group3Name = splittedLine.length > 1 ? splittedLine[1] : null;
                        break;
                    case 13:
                        splittedLine = reader.readLine().split(":");
                        group2ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        group2Name = splittedLine.length > 1 ? splittedLine[1] : null;
                        break;
                    case 14:
                        splittedLine = reader.readLine().split(":");
                        group1ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        group1Name = splittedLine.length > 1 ? splittedLine[1] : null;
                        break;
                    case 18:
                        String itemName = reader.readLine().trim();

                        if ((group2Name != null) && (group3Name != null)) {
                            if (!parents) {
                                //sid - name - parentSID(null)
                                addIfNotContains(new ItemGroup(group4, group4, null));
                                addIfNotContains(new ItemGroup((group3ID + "/" + group4), group3Name, null));
                                addIfNotContains(new ItemGroup((group2ID + "/" + group3ID + "/" + group4), group2Name, null));
                                if (group1Name != null) {
                                    addIfNotContains(new ItemGroup((group1ID + "/" + group2ID + "/" + group3ID + "/" + group4), group1Name, null));
                                    addIfNotContains(new ItemGroup(itemID, itemName, null));
                                }
                            } else {
                                //sid - name(null) - parentSID
                                addIfNotContains(new ItemGroup(group4, null, null));
                                addIfNotContains(new ItemGroup((group3ID + "/" + group4), null, group4));
                                addIfNotContains(new ItemGroup((group2ID + "/" + group3ID + "/" + group4), null, group3ID + "/" + group4));
                                if (group1ID != null) {
                                    addIfNotContains(new ItemGroup((group1ID + "/" + group2ID + "/" + group3ID + "/" + group4), null, group2ID + "/" + group3ID + "/" + group4));
                                    addIfNotContains(new ItemGroup(itemID, null, group1ID + "/" + group2ID + "/" + group3ID + "/" + group4));
                                }
                            }
                        }
                        break;
                }
            }
        }
        reader.close();
        return itemGroupsList;
    }

    private void addIfNotContains(ItemGroup element) {
        String itemGroup = element.sid.trim() + "/" + (element.name == null ? "" : element.name.trim()) + "/" + (element.parent == null ? "" : element.parent.trim());
        if (!itemGroups.contains(itemGroup)) {
            itemGroupsList.add(element);
            itemGroups.add(itemGroup);
        }
    }

    private List<Item> importItems(String path, Integer numberOfItems) throws IOException, xBaseJException {

        List<Item> itemsList = new ArrayList<Item>();
        String nameCountry = "БЕЛАРУСЬ";

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
        String line;
        String itemID;
        Double baseMarkup = null;
        Double retailMarkup = null;
        String uomID = null;
        String uomName = null;
        String uomShortName = null;
        String group1ID = null;
        String group2ID = null;
        String group3ID = null;
        Double retailVAT = null;
        String itemName = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && itemsList.size() > numberOfItems)
                break;
            if (line.startsWith("^stmc")) {
                itemID = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 2:
                        String baseMarkupValue = reader.readLine().trim();
                        baseMarkup = baseMarkupValue.isEmpty() ? null : Double.parseDouble(baseMarkupValue);
                        break;
                    case 3:
                        String retailMarkupValue = reader.readLine().trim();
                        retailMarkup = retailMarkupValue.isEmpty() ? null : Double.parseDouble(retailMarkupValue);
                        break;
                    case 11:
                        String[] splittedLine = reader.readLine().split(":");
                        uomID = splittedLine.length > 0 ? splittedLine[0] : null;
                        uomName = splittedLine.length > 1 ? splittedLine[1] : null;
                        uomShortName = uomName == null ? null : uomName.length() <= 5 ? uomName : uomName.substring(0, 5);
                        break;
                    case 12:
                        splittedLine = reader.readLine().split(":");
                        group3ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 13:
                        splittedLine = reader.readLine().split(":");
                        group2ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 14:
                        splittedLine = reader.readLine().split(":");
                        group1ID = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 15:
                        retailVAT = Double.parseDouble(reader.readLine().trim());
                        break;
                    case 18:
                        itemName = reader.readLine().trim();
                        break;
                    case 38:
                        if ((itemName != null) && (!"".equals(itemName))) {
                            String groupID = (group1ID == null ? "" : (group1ID + "/")) + group2ID + "/" + group3ID + "/" + "ВСЕ";
                            String d = reader.readLine().trim();
                            java.sql.Date date = "".equals(d) ? null : new java.sql.Date(Long.parseLong(d));
                            itemsList.add(new Item(itemID, groupID, itemName, uomName, uomShortName, uomID, null, null,
                                    nameCountry, null, null, date, null, null, null, null, retailVAT, null, null, null, null,
                                    baseMarkup, retailMarkup, null, null, null));
                            group1ID = null;
                            group2ID = null;
                            group3ID = null;
                            itemName = null;
                            uomName = null;
                            uomShortName = null;
                            uomID = null;
                            retailVAT = null;
                        }
                        break;
                }
            }
        }
        reader.close();
        return itemsList;
    }

    private List<UserInvoiceDetail> importUserInvoices(String path) throws IOException {

        List<UserInvoiceDetail> userInvoiceDetailsList = new ArrayList<UserInvoiceDetail>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
        String line;
        String sid;
        String itemID;
        Double quantity = null;
        Double price = null;
        String warehouseID = null;
        String supplierWarehouse = null;
        Double chargePrice = null;
        String supplierID = null;
        String number = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                sid = line.split("\\(|\\)|,")[1];
                itemID = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 0:
                        quantity = Double.parseDouble(reader.readLine().trim());
                        break;
                    case 23:
                        price = Double.parseDouble(reader.readLine().trim());
                        break;
                    case 27:
                        warehouseID = reader.readLine().trim();
                        break;
                    case 30:
                        supplierID = reader.readLine().trim();
                        supplierWarehouse = supplierID;
                        break;
                    case 32:
                        String cp = reader.readLine().trim();
                        if (!"".equals(cp))
                            chargePrice = Double.valueOf((cp.startsWith(".") ? "0" : "") + cp);
                        break;
                    case 37:
                        number = reader.readLine().trim();
                        break;
                    case 38:
                        if ((number != null) && (!"".equals(number))) {
                            String d = reader.readLine().trim();
                            java.sql.Date date = "".equals(d) ? null : new java.sql.Date(Long.parseLong(d));
                            userInvoiceDetailsList.add(new UserInvoiceDetail(number, "AA", null, true, sid,
                                    date, itemID, quantity, supplierID, warehouseID, supplierWarehouse, price,
                                    chargePrice, null, null, null));
                            quantity = null;
                            supplierID = null;
                            warehouseID = null;
                            supplierWarehouse = null;
                            price = null;
                        }
                        break;
                }
            }
        }
        reader.close();
        return userInvoiceDetailsList;
    }

    private List<Warehouse> importWarehouses(String path, String path2) {

        List<Warehouse> warehousesList = new ArrayList<Warehouse>();

        try {
            String defaultLegalEntitySID = "sle";
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("^SMOL")) {
                    String warehouseID = line.split("\\(|\\)")[1];
                    String[] dataWarehouse = reader.readLine().split(":");
                    String name = dataWarehouse.length > 0 ? dataWarehouse[0].trim() : null;
                    warehousesList.add(new Warehouse(defaultLegalEntitySID, "own", warehouseID, name, null));
                }
            }
            reader.close();

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path2), "windows-1251"));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("^SWTP")) {
                    String legalEntityID = line.split("\\(|\\)")[1];
                    String[] dataLegalEntity = reader.readLine().split(":");
                    String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : null;
                    String warehouseAddress = dataLegalEntity.length > 13 ? dataLegalEntity[13].trim() : null;
                    if (name != null && !"".equals(name))
                        warehousesList.add(new Warehouse(legalEntityID, "contractor", "swtp_" + legalEntityID, name, warehouseAddress));
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return warehousesList;
    }

    private List<LegalEntity> importLegalEntities(String path) {

        List<LegalEntity> legalEntitiesList = new ArrayList<LegalEntity>();
        String nameCountry = "БЕЛАРУСЬ";
        try {

            legalEntitiesList.add(new LegalEntity("sle", "Стандартная Организация", null, null, null,
                    null, null, null, null, null, null, null, null, nameCountry, null, true, null));

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("^SWTP")) {
                    String legalEntityID = line.split("\\(|\\)")[1];
                    String[] dataLegalEntity = reader.readLine().split(":");
                    String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : "";
                    String unp = dataLegalEntity.length > 4 ? dataLegalEntity[4].trim() : "";
                    String address = dataLegalEntity.length > 5 ? dataLegalEntity[5].trim() : "";
                    String okpo = dataLegalEntity.length > 10 ? dataLegalEntity[10].trim() : "";

                    if (!name.isEmpty())
                        legalEntitiesList.add(new LegalEntity(legalEntityID, name,
                                address.isEmpty() ? null : address, unp.isEmpty() ? null : unp,
                                okpo.isEmpty() ? null : okpo, null, null, null, null, null, null, null, null,
                                nameCountry, true, null, true));
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return legalEntitiesList;
    }

    private List<Contract> importContracts(String path) throws IOException {

        String shortNameCurrency = "BLR";

        List<Contract> contractsList = new ArrayList<Contract>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
        String line;
        String[] patterns = new String[]{"(.*)ОТ(\\d{2}\\.\\d{2}\\.\\d{2})ДО(\\d{2}\\.\\d{2}\\.\\d{2}).*",
                "()()ДО\\s(\\d{2}-\\d{2}-\\d{2}).*",
                "(.*)\\s?ОТ\\s?(\\d{1,2}\\.?\\d{1,2}\\.?\\d{1,4})(?:Г|Н)?$",
                "()(\\d{1,2}\\/\\d{1,2}\\-\\d{2,4})",
                "(.*?)\\s?(\\d{1,2}\\.?\\/?\\d{1,2}\\.?\\d{2,4})Г?$",
                "(ЛИЦ.*)\\s(\\d{6})",
                "((?:ДОГ|КОНТР).*)", /*ловим договоры вообще без дат*/
                "(.*)/?\\s?(?:ИП|СПК|ОП)"
        };
        String[] datePatterns = new String[]{"dd-mm-yy", "dd.mm.yy", "dd.mm.yyГ", "ddmmyy", "dd/mm-yyyy", "dd.mm.y", "dd.mmyy"};
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String legalEntity1ID = splittedLine[1].replace("\"", "");
                    String legalEntity2ID = "sle";
                    String contractID1 = legalEntity1ID + "/" + legalEntity2ID;
                    String contractID2 = legalEntity2ID + "/" + legalEntity1ID;
                    splittedLine = reader.readLine().split(":");
                    String contractString = splittedLine.length > 6 ? splittedLine[6].trim() : "";
                    if (!contractString.trim().isEmpty()) {
                        String number = "";
                        java.sql.Date dateFrom = null;
                        java.sql.Date dateTo = null;
                        Boolean found = false;
                        for (String p : patterns) {
                            Pattern r = Pattern.compile(p);
                            Matcher m = r.matcher(contractString);
                            if (m.find()) {
                                number = m.group(1).trim();
                                try {
                                    dateFrom = (m.groupCount() >= 2 && !m.group(2).isEmpty()) ? new java.sql.Date(DateUtils.parseDate(m.group(2), datePatterns).getTime()) : null;
                                    dateTo = (m.groupCount() >= 3 && !m.group(3).isEmpty()) ? new java.sql.Date(DateUtils.parseDate(m.group(3), datePatterns).getTime()) : null;
                                } catch (ParseException e) {
                                    if (dateFrom == null && m.groupCount() >= 2)
                                        number = m.group(0).trim();
                                    else if (dateTo == null && m.groupCount() >= 3)
                                        number = m.group(0).trim();
                                }
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            contractsList.add(new Contract(contractID1, legalEntity1ID, legalEntity2ID,
                                    number, dateFrom, dateTo, shortNameCurrency));
                            contractsList.add(new Contract(contractID2, legalEntity2ID, legalEntity1ID,
                                    number, dateFrom, null, shortNameCurrency));
                        }
                    }
                }
            }
        }
        reader.close();

        return contractsList;
    }

    private void importSotUOM(String stmcPath) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException {

        List<List<Object>> data = importSotUOMItemsFromFile(stmcPath);

        if (data != null) {
            ImportField sotUOMIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField weightItemField = new ImportField(LM.findLCPByCompoundName("netWeightItem"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("item"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

            ImportKey<?> sotUOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("sotUOM"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(sotUOMIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(weightItemField, LM.findLCPByCompoundName("netWeightItem").getMapping(itemKey)));
            props.add(new ImportProperty(weightItemField, LM.findLCPByCompoundName("grossWeightItem").getMapping(itemKey)));
            props.add(new ImportProperty(sotUOMIDField, LM.findLCPByCompoundName("sotUOMItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("sotUOM")).getMapping(sotUOMKey)));

            ImportTable table = new ImportTable(Arrays.asList(sotUOMIDField, itemField, weightItemField), data);

            DataSession session = LM.getBL().createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(sotUOMKey, itemKey), props);
            service.synchronize(true, false);
            session.apply(LM.getBL());
            session.close();
        }
    }

    private List<List<Object>> importSotUOMItemsFromFile(String stmcPath) throws IOException {

        String pattern = "(.*)\\s(\\d+)(КГ)";

        List<List<Object>> data = new ArrayList<List<Object>>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(stmcPath), "windows-1251"));
        String line;
        String itemID;
        String uomID = null;
        Double weight = null;
        String itemName = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                itemID = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 11:
                        String uomLine = reader.readLine();
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(uomLine);
                        if (m.matches()) {
                            uomID = m.group(1).trim();
                            weight = Double.parseDouble(m.group(2).trim());
                        } else {
                            String[] splittedLine = uomLine.split(":");
                            uomID = splittedLine.length > 0 ? splittedLine[0] : null;
                        }
                        if (uomID != null)
                            while (uomID.length() < 3)
                                uomID = "0" + uomID;
                        break;
                    case 18:
                        itemName = reader.readLine().trim();
                        break;
                    case 38:
                        if ((itemName != null) && (!"".equals(itemName))) {
                            data.add(Arrays.asList((Object) (uomID == null ? null : "UOMS" + uomID),
                                    "I" + itemID, weight));
                        }
                        uomID = null;
                        weight = null;
                        itemName = null;
                        break;
                }
            }
        }
        reader.close();
        return data;
    }
}