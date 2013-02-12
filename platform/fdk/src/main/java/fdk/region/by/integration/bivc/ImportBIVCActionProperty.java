package fdk.region.by.integration.bivc;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

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

                importData.setItemsList((getLCP("importBIVCItems").read(context) != null) ?
                        importItems(path + "//stmc", (Integer) getLCP("importBIVCNumberItems").read(context)) : null);

                importData.setUserInvoicesList((getLCP("importBIVCUserInvoices").read(context) != null) ?
                        importUserInvoices(path + "//stmc") : null);

                new ImportActionProperty(LM, importData, context).makeImport();
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
                                    nameCountry, null, date, null, null, null, null, retailVAT, null, null, null, null,
                                    baseMarkup, retailMarkup));
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
                                    chargePrice, null, null, null, null, null, null));
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
                    String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : null;
                    String unp = dataLegalEntity.length > 4 ? dataLegalEntity[4].trim() : null;
                    String address = dataLegalEntity.length > 5 ? dataLegalEntity[5].trim() : null;
                    String okpo = dataLegalEntity.length > 10 ? dataLegalEntity[10].trim() : null;

                    if (!"".equals(name))
                        legalEntitiesList.add(new LegalEntity(legalEntityID, name, address, unp, okpo, null,
                                null, null, null, null, null, null, null, nameCountry, true, null, true));
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
}