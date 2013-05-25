package integration;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.xBaseJException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

                ImportActionProperty imp = new ImportActionProperty(LM, importData, context);
                imp.showManufacturingPrice = true;
                imp.showWholesalePrice = true;
                imp.makeImport();

                if ((getLCP("importBIVCItems").read(context) != null))
                    importSotUOM(context, path + "//stmc");
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<String> itemGroups;
    List<ItemGroup> itemGroupsList;

    private List<ItemGroup> importItemGroups(String path, Boolean parents) throws IOException, xBaseJException {

        itemGroups = new ArrayList<String>();
        itemGroupsList = new ArrayList<ItemGroup>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
        String line;
        String idItem;
        String idGroup1 = null;
        String group1Name = null;
        String idGroup2 = null;
        String group2Name = null;
        String idGroup3 = null;
        String group3Name = null;
        String group4 = "ВСЕ";
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                idItem = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 12:
                        String[] splittedLine = reader.readLine().split(":");
                        idGroup3 = splittedLine.length > 0 ? splittedLine[0] : null;
                        group3Name = splittedLine.length > 1 ? splittedLine[1] : idGroup3;
                        break;
                    case 13:
                        splittedLine = reader.readLine().split(":");
                        idGroup2 = splittedLine.length > 0 ? splittedLine[0] : null;
                        group2Name = splittedLine.length > 1 ? splittedLine[1] : idGroup2;
                        break;
                    case 14:
                        splittedLine = reader.readLine().split(":");
                        idGroup1 = splittedLine.length > 0 ? splittedLine[0] : null;
                        group1Name = splittedLine.length > 1 ? splittedLine[1] : idGroup1;
                        break;
                    case 18:
                        String itemName = reader.readLine().trim();

                        if ((group2Name != null) && (group3Name != null)) {
                            if (!parents) {
                                //id - name - idParent(null)
                                addIfNotContains(new ItemGroup(group4, group4, null));
                                addIfNotContains(new ItemGroup((idGroup3 + "/" + group4), group3Name, null));
                                addIfNotContains(new ItemGroup((idGroup2 + "/" + idGroup3 + "/" + group4), group2Name, null));
                                if (group1Name != null) {
                                    addIfNotContains(new ItemGroup((idGroup1 + "/" + idGroup2 + "/" + idGroup3 + "/" + group4), group1Name, null));
                                    addIfNotContains(new ItemGroup(idItem, itemName, null));
                                }
                            } else {
                                //id - name(null) - idParent
                                addIfNotContains(new ItemGroup(group4, null, null));
                                addIfNotContains(new ItemGroup((idGroup3 + "/" + group4), null, group4));
                                addIfNotContains(new ItemGroup((idGroup2 + "/" + idGroup3 + "/" + group4), null, idGroup3 + "/" + group4));
                                if (idGroup1 != null) {
                                    addIfNotContains(new ItemGroup((idGroup1 + "/" + idGroup2 + "/" + idGroup3 + "/" + group4), null, idGroup2 + "/" + idGroup3 + "/" + group4));
                                    addIfNotContains(new ItemGroup(idItem, null, idGroup1 + "/" + idGroup2 + "/" + idGroup3 + "/" + group4));
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
        String idItem;
        BigDecimal baseMarkup = null;
        BigDecimal retailMarkup = null;
        String idUOM = null;
        String uomName = null;
        String uomShortName = null;
        String idGroup1 = null;
        String idGroup2 = null;
        String idGroup3 = null;
        BigDecimal retailVAT = null;
        String itemName = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && itemsList.size() > numberOfItems)
                break;
            if (line.startsWith("^stmc")) {
                idItem = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 2:
                        String baseMarkupValue = reader.readLine().trim();
                        baseMarkup = baseMarkupValue.isEmpty() ? null : parseBigDecimal(baseMarkupValue);
                        break;
                    case 3:
                        String retailMarkupValue = reader.readLine().trim();
                        retailMarkup = retailMarkupValue.isEmpty() ? null : parseBigDecimal(retailMarkupValue);
                        break;
                    case 11:
                        String[] splittedLine = reader.readLine().split(":");
                        idUOM = splittedLine.length > 0 ? splittedLine[0] : null;
                        uomName = splittedLine.length > 1 ? splittedLine[1] : null;
                        uomShortName = uomName == null ? null : uomName.length() <= 5 ? uomName : uomName.substring(0, 5);
                        break;
                    case 12:
                        splittedLine = reader.readLine().split(":");
                        idGroup3 = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 13:
                        splittedLine = reader.readLine().split(":");
                        idGroup2 = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 14:
                        splittedLine = reader.readLine().split(":");
                        idGroup1 = splittedLine.length > 0 ? splittedLine[0] : null;
                        break;
                    case 15:
                        retailVAT = parseBigDecimal(reader.readLine().trim());
                        break;
                    case 18:
                        itemName = reader.readLine().trim();
                        break;
                    case 38:
                        if ((itemName != null) && (!"".equals(itemName))) {
                            String idGroup = (idGroup1 == null ? "" : (idGroup1 + "/")) + idGroup2 + "/" + idGroup3 + "/" + "ВСЕ";
                            String d = reader.readLine().trim();
                            Date date;
                            try {
                                date = "".equals(d) ? null : new Date(DateUtils.parseDate(d, new String[]{"MM/dd/yyyy"}).getTime());
                            } catch (ParseException e) {
                                date = null;
                            }
                            itemsList.add(new Item(idItem, idGroup, itemName, uomName, uomShortName, idUOM, null, null,
                                    nameCountry, null, null, date, null, null, null, null, retailVAT, null, null, null, null,
                                    baseMarkup, retailMarkup, null, null, null, null, null, null));
                            idGroup1 = null;
                            idGroup2 = null;
                            idGroup3 = null;
                            itemName = null;
                            uomName = null;
                            uomShortName = null;
                            idUOM = null;
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
        String idItem;
        BigDecimal baseMarkup = null;
        BigDecimal supplierMarkup = null;
        BigDecimal quantity = null;
        BigDecimal price;
        BigDecimal manufacturingPrice = null;
        BigDecimal chargePrice = null;
        BigDecimal wholesalePrice;
        String idCustomerWarehouse = null;
        String idSupplierWarehouse = null;
        String idSupplier = null;
        String series = "AA";
        String number = null;
        String textCompliance = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                sid = line.split("\\(|\\)|,")[1];
                idItem = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 0:
                        quantity = parseBigDecimal(reader.readLine().trim());
                        break;
                    case 2:
                        String baseMarkupString = reader.readLine().trim();
                        baseMarkup = baseMarkupString.isEmpty() ? new BigDecimal(0) : parseBigDecimal(baseMarkupString);
                        break;
                    case 4:
                        String supplierMarkupString = reader.readLine().trim();
                        supplierMarkup = supplierMarkupString.isEmpty() ? new BigDecimal(0) : parseBigDecimal(supplierMarkupString);
                        break;
                    case 8:
                        textCompliance = reader.readLine().trim();
                        break;
                    case 23:
                        manufacturingPrice = parseBigDecimal(reader.readLine().trim());
                        break;
                    case 27:
                        idCustomerWarehouse = trimIdWarehouses(reader.readLine().trim());
                        break;
                    case 30:
                        idSupplier = reader.readLine().trim();
                        idSupplierWarehouse = "S" + trimIdWarehouses(idSupplier);
                        break;
                    case 32:
                        String cp = reader.readLine().trim();
                        if (!"".equals(cp))
                            chargePrice = parseBigDecimal((cp.startsWith(".") ? "0" : "") + cp);
                        break;
                    case 37:
                        number = reader.readLine().trim();
                        break;
                    case 38:
                        if ((number != null) && (!"".equals(number))) {
                            price = manufacturingPrice.multiply(new BigDecimal(100).subtract(supplierMarkup)).divide(new BigDecimal(100));
                            wholesalePrice = manufacturingPrice.add(chargePrice).multiply(baseMarkup.add(new BigDecimal(100))).divide(new BigDecimal(100));
                            String d = reader.readLine().trim();
                            Date date;
                            try {
                                date = "".equals(d) ? null : new Date(DateUtils.parseDate(d, new String[]{"MM/dd/yyyy"}).getTime());
                            } catch (ParseException e) {
                                date = null;
                            }
                            userInvoiceDetailsList.add(new UserInvoiceDetail(series + number, series, number, null, true, sid,
                                    date, idItem, false, quantity, idSupplier, idCustomerWarehouse,idSupplierWarehouse,
                                    price, chargePrice, manufacturingPrice, wholesalePrice, baseMarkup, null, null,
                                    textCompliance, null, null, null, null, null, null, null, null));
                            quantity = null;
                            idSupplier = null;
                            idCustomerWarehouse = null;
                            idSupplierWarehouse = null;
                            baseMarkup = null;
                            supplierMarkup = null;
                            textCompliance = null;
                        }
                        break;
                }
            }
        }
        reader.close();
        return userInvoiceDetailsList;
    }

    private String trimIdWarehouses(String idWarehouse) {
        while (idWarehouse.startsWith("0"))
            idWarehouse = idWarehouse.substring(1);
        return idWarehouse;
    }

    private List<Warehouse> importWarehouses(String path, String path2) {

        List<Warehouse> warehousesList = new ArrayList<Warehouse>();

        try {
            String idDefaultLegalEntity = "sle";
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "windows-1251"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("^SMOL")) {
                    String idWarehouse = line.split("\\(|\\)")[1];
                    String[] dataWarehouse = reader.readLine().split(":");
                    String name = dataWarehouse.length > 0 ? dataWarehouse[0].trim() : null;
                    warehousesList.add(new Warehouse(idDefaultLegalEntity, "own", trimIdWarehouses(idWarehouse), name, null));
                }
            }
            reader.close();

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path2), "windows-1251"));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("^SWTP")) {
                    String idLegalEntity = line.split("\\(|\\)")[1];
                    String[] dataLegalEntity = reader.readLine().split(":");
                    String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : null;
                    String warehouseAddress = dataLegalEntity.length > 13 ? dataLegalEntity[13].trim() : null;
                    if (name != null && !"".equals(name))
                        warehousesList.add(new Warehouse(idLegalEntity, "contractor", "S" + trimIdWarehouses(idLegalEntity),
                                name, warehouseAddress));
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
                    String idLegalEntity = line.split("\\(|\\)")[1];
                    String[] dataLegalEntity = reader.readLine().split(":");
                    String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : "";
                    String unp = dataLegalEntity.length > 4 ? dataLegalEntity[4].trim() : "";
                    String address = dataLegalEntity.length > 5 ? dataLegalEntity[5].trim() : "";
                    String okpo = dataLegalEntity.length > 10 ? dataLegalEntity[10].trim() : "";

                    if (!name.isEmpty())
                        legalEntitiesList.add(new LegalEntity(idLegalEntity, name,
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
                    String idLegalEntity1 = splittedLine[1].replace("\"", "");
                    String idLegalEntity2 = "sle";
                    String idContract1 = idLegalEntity1 + "/" + idLegalEntity2;
                    String idContract2 = idLegalEntity2 + "/" + idLegalEntity1;
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
                            contractsList.add(new Contract(idContract1, idLegalEntity1, idLegalEntity2,
                                    number, dateFrom, dateTo, shortNameCurrency));
                            contractsList.add(new Contract(idContract2, idLegalEntity2, idLegalEntity1,
                                    number, dateFrom, null, shortNameCurrency));
                        }
                    }
                }
            }
        }
        reader.close();

        return contractsList;
    }

    private void importSotUOM(ExecutionContext context, String stmcPath) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException {

        List<List<Object>> data = importSotUOMItemsFromFile(stmcPath);

        if (data != null) {
            ImportField idSotUOMField = new ImportField(LM.findLCPByCompoundName("idSotUOM"));
            ImportField itemField = new ImportField(LM.findLCPByCompoundName("idItem"));
            ImportField weightItemField = new ImportField(LM.findLCPByCompoundName("netWeightItem"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                    LM.findLCPByCompoundName("itemId").getMapping(itemField));

            ImportKey<?> sotUOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("SotUOM"),
                    LM.findLCPByCompoundName("sotUOMId").getMapping(idSotUOMField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(weightItemField, LM.findLCPByCompoundName("netWeightItem").getMapping(itemKey)));
            props.add(new ImportProperty(weightItemField, LM.findLCPByCompoundName("grossWeightItem").getMapping(itemKey)));
            props.add(new ImportProperty(idSotUOMField, LM.findLCPByCompoundName("sotUOMItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("SotUOM")).getMapping(sotUOMKey)));

            ImportTable table = new ImportTable(Arrays.asList(idSotUOMField, itemField, weightItemField), data);

            DataSession session = context.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(sotUOMKey, itemKey), props);
            service.synchronize(true, false);
            session.apply(context.getBL());
            session.close();
        }
    }

    private List<List<Object>> importSotUOMItemsFromFile(String stmcPath) throws IOException {

        String pattern = "(.*)\\s(\\d+)(КГ)";

        List<List<Object>> data = new ArrayList<List<Object>>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(stmcPath), "windows-1251"));
        String line;
        String idItem;
        String idUOM = null;
        BigDecimal weight = null;
        String itemName = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^stmc")) {
                idItem = line.split("\\(|\\)|,")[1];
                Integer field = Integer.parseInt(line.split("\\(|\\)|,")[2]);
                switch (field) {
                    case 11:
                        String uomLine = reader.readLine();
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(uomLine);
                        if (m.matches()) {
                            idUOM = m.group(1).trim();
                            weight = parseBigDecimal(m.group(2).trim());
                        } else {
                            String[] splittedLine = uomLine.split(":");
                            idUOM = splittedLine.length > 0 ? splittedLine[0] : null;
                        }
                        if (idUOM != null)
                            while (idUOM.length() < 3)
                                idUOM = "0" + idUOM;
                        break;
                    case 18:
                        itemName = reader.readLine().trim();
                        break;
                    case 38:
                        if ((itemName != null) && (!"".equals(itemName))) {
                            data.add(Arrays.asList((Object) (idUOM == null ? null : "S" + idUOM),
                                    idItem, weight));
                        }
                        idUOM = null;
                        weight = null;
                        itemName = null;
                        break;
                }
            }
        }
        reader.close();
        return data;
    }

    private BigDecimal parseBigDecimal(String value) {
        return BigDecimal.valueOf(Double.parseDouble(value));
    }
}