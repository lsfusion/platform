package fdk.region.by.integration.lstrade;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ImportLSTradeActionProperty extends ScriptingActionProperty {

    public ImportLSTradeActionProperty(ScriptingLogicsModule LM) {
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
            Integer numberOfItems = (Integer) getLCP("importNumberItems").read(context);
            Integer numberOfPriceLists = (Integer) getLCP("importNumberPriceLists").read(context);
            String prefixStore = (String) getLCP("prefixStore").read(context);
            prefixStore = prefixStore == null ? "МГ" : prefixStore.trim();

            Object pathObject = getLCP("importLSTDirectory").read(context);
            String path = pathObject == null ? "" : ((String) pathObject).trim();
            if (!path.isEmpty()) {

                ImportData importData = new ImportData();

                Boolean importInactive = getLCP("importInactive").read(context) != null;
                importData.setImportInactive(importInactive);

                importData.setNumberOfItemsAtATime((Integer) getLCP("importNumberItemsAtATime").read(context));
                importData.setNumberOfPriceListsAtATime((Integer) getLCP("importNumberPriceListsAtATime").read(context));

                importData.setItemGroupsList((getLCP("importGroupItems").read(context) != null) ?
                        importItemGroupsFromDBF(path + "//_sprgrt.dbf", false) : null);

                importData.setParentGroupsList((getLCP("importGroupItems").read(context) != null) ?
                        importItemGroupsFromDBF(path + "//_sprgrt.dbf", true) : null);

                importData.setBanksList((getLCP("importBanks").read(context) != null) ?
                        importBanksFromDBF(path + "//_sprbank.dbf") : null);

                importData.setLegalEntitiesList((getLCP("importLegalEntities").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//_sprana.dbf", prefixStore, importInactive, false) : null);

                importData.setWarehousesList((getLCP("importWarehouses").read(context) != null) ?
                        importWarehousesFromDBF(path + "//_sprana.dbf", importInactive) : null);

                importData.setContractsList((getLCP("importContracts").read(context) != null) ?
                        importContractsFromDBF(path + "//_sprcont.dbf") : null);

                importData.setStoresList((getLCP("importStores").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//_sprana.dbf", prefixStore, importInactive, true) : null);

                importData.setDepartmentStoresList((getLCP("importDepartmentStores").read(context) != null) ?
                        importDepartmentStoresFromDBF(path + "//_sprana.dbf", importInactive, path + "//_storestr.dbf") : null);

                importData.setRateWastesList((getLCP("importRateWastes").read(context) != null) ?
                        importRateWastesFromDBF(path + "//_sprvgrt.dbf") : null);

                importData.setWaresList((getLCP("importWares").read(context) != null) ?
                        importWaresFromDBF(path + "//_sprgrm.dbf") : null);

                importData.setItemsList((getLCP("importItems").read(context) != null) ?
                        importItemsFromDBF(path + "//_sprgrm.dbf", path + "//_postvar.dbf", numberOfItems, importInactive) : null);

                importData.setPriceListStoresList((getLCP("importPriceListStores").read(context) != null) ?
                        importPriceListStoreFromDBF(path + "//_postvar.dbf", path + "//_strvar.dbf", numberOfPriceLists) : null);

                importData.setPriceListSuppliersList((getLCP("importPriceListSuppliers").read(context) != null) ?
                        importPriceListSuppliersFromDBF(path + "//_postvar.dbf", numberOfPriceLists) : null);

                importData.setUserInvoicesList((getLCP("importUserInvoices").read(context) != null) ?
                        importUserInvoicesFromDBF(path + "//_ostn.dbf") : null);

                new ImportActionProperty(LM, importData, context).makeImport();
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ItemGroup> data;
    private List<String> itemGroups;

    private List<ItemGroup> importItemGroupsFromDBF(String path, Boolean parents) throws IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<ItemGroup>();
        itemGroups = new ArrayList<String>();

        String groupTop = "ВСЕ";
        if (!parents)
            addIfNotContains(new ItemGroup(groupTop, groupTop, null));
        else
            addIfNotContains(new ItemGroup(groupTop, null, null));

        for (int i = 0; i < recordCount; i++) {

            importFile.read();

            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String group1 = new String(importFile.getField("GROUP1").getBytes(), "Cp1251").trim();
            String group2 = new String(importFile.getField("GROUP2").getBytes(), "Cp1251").trim();
            String group3 = new String(importFile.getField("GROUP3").getBytes(), "Cp1251").trim();

            if (!group2.isEmpty() && (!group3.isEmpty())) {

                if (!parents) {
                    //sid - name - parentSID(null)
                    addIfNotContains(new ItemGroup((group3.substring(0, 3) + "/" + groupTop), group3, null));
                    addIfNotContains(new ItemGroup((group2 + "/" + group3.substring(0, 3) + "/" + groupTop), group2, null));
                    addIfNotContains(new ItemGroup((group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + groupTop), group1, null));
                    addIfNotContains(new ItemGroup(k_grtov, pol_naim, null));
                } else {
                    //sid - name(null) - parentSID
                    addIfNotContains(new ItemGroup((group3.substring(0, 3) + "/" + groupTop), null, groupTop));
                    addIfNotContains(new ItemGroup((group2 + "/" + group3.substring(0, 3) + "/" + groupTop), null, group3.substring(0, 3) + "/" + groupTop));
                    addIfNotContains(new ItemGroup((group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + groupTop), null, group2 + "/" + group3.substring(0, 3) + "/" + groupTop));
                    addIfNotContains(new ItemGroup(k_grtov, null, group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + groupTop));
                }

            } else {
                if (k_grtov.endsWith("."))
                    k_grtov = k_grtov.substring(0, k_grtov.length() - 1);

                int dotCount = 0;
                for (char c : k_grtov.toCharArray())
                    if (c == '.')
                        dotCount++;

                if (!parents) {
                    //sid - name - parentSID(null)
                    addIfNotContains(new ItemGroup(k_grtov, pol_naim, null));
                    if (dotCount == 1)
                        addIfNotContains(new ItemGroup(group1, group1, null));

                } else {
                    //sid - name(null) - parentSID
                    addIfNotContains(new ItemGroup(k_grtov, null, group1));
                    if (dotCount == 1)
                        addIfNotContains(new ItemGroup(group1, null, groupTop));
                }
            }
        }
        return data;
    }

    private List<Ware> importWaresFromDBF(String path) throws IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();
        List<Ware> data = new ArrayList<Ware>();

        for (int i = 0; i < recordCount; i++) {
            importFile.read();

            Boolean isWare = "T".equals(new String(importFile.getField("LGRMSEC").getBytes(), "Cp1251").trim());
            String wareID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            Double price = new Double(new String(importFile.getField("CENUOSEC").getBytes(), "Cp1251").trim());

            if (!"".equals(wareID) && isWare)
                data.add(new Ware(wareID, pol_naim, price));
        }
        return data;
    }

    private List<Item> importItemsFromDBF(String itemsPath, String quantityPath, Integer numberOfItems, Boolean importInactive) throws IOException, xBaseJException, ParseException {

        if (!(new File(itemsPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + itemsPath + " не найден");

        if (!(new File(quantityPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + quantityPath + " не найден");

        Set<String> barcodes = new HashSet<String>();

        DBF quantityImportFile = new DBF(quantityPath);
        int totalRecordCount = quantityImportFile.getRecordCount();

        Map<String, Double> quantities = new HashMap<String, Double>();

        for (int i = 0; i < totalRecordCount; i++) {
            quantityImportFile.read();

            String itemID = new String(quantityImportFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            Double quantityPackItem = new Double(new String(quantityImportFile.getField("PACKSIZE").getBytes(), "Cp1251").trim());

            if (quantityPackItem == 0)
                quantityPackItem = 1.0;
            if (!quantities.containsKey(itemID)) {
                quantities.put(itemID, quantityPackItem);
            }
        }

        DBF itemsImportFile = new DBF(itemsPath);
        totalRecordCount = itemsImportFile.getRecordCount();
        if (totalRecordCount <= 0) {
            return null;
        }

        List<Item> data = new ArrayList<Item>();
        List<Double> allowedVAT = Arrays.asList(0.0, 9.09, 16.67, 10.0, 20.0, 24.0);

        int recordCount = (numberOfItems != null && numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;
        for (int i = 0; i < recordCount; i++) {
            itemsImportFile.read();
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
            if (k_grtov.endsWith("."))
                k_grtov = k_grtov.substring(0, k_grtov.length() - 1);
            String UOM = new String(itemsImportFile.getField("K_IZM").getBytes(), "Cp1251").trim();
            String brand = new String(itemsImportFile.getField("BRAND").getBytes(), "Cp1251").trim();
            String country = new String(itemsImportFile.getField("MANFR").getBytes(), "Cp1251").trim();
            if ("РБ".equals(country) || "Беларусь".equals(country))
                country = "БЕЛАРУСЬ";
            String dateString = new String(itemsImportFile.getField("P_TIME").getBytes(), "Cp1251").trim();
            Date date = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
            Boolean isWeightItem = "T".equals(new String(itemsImportFile.getField("LWEIGHT").getBytes(), "Cp1251").substring(0, 1));
            String composition = "";
            if (itemsImportFile.getField(/*"FORMULA"*/"ENERGVALUE").getBytes() != null) {
                composition = new String(itemsImportFile.getField(/*"FORMULA"*/"ENERGVALUE").getBytes(), "Cp1251").replace("\n", "").replace("\r", "");
            }
            Double retailVAT = new Double(new String(itemsImportFile.getField("NDSR").getBytes(), "Cp1251").trim());
            Double quantityPackItem = quantities.containsKey(itemID) ? quantities.get(itemID) : null;
            Boolean isWare = "T".equals(new String(itemsImportFile.getField("LGRMSEC").getBytes(), "Cp1251").substring(0, 1));
            String wareID = new String(itemsImportFile.getField("K_GRMSEC").getBytes(), "Cp1251").trim();
            if (wareID.isEmpty())
                wareID = null;
            String rateWasteID = "RW_" + new String(itemsImportFile.getField("K_VGRTOV").getBytes(), "Cp1251").trim();

            Double priceWare = new Double(new String(itemsImportFile.getField("CENUOSEC").getBytes(), "Cp1251").trim());
            Double ndsWare = new Double(getFieldValue(itemsImportFile, "NDSSEC", "Cp1251", "20"));

            if (!"".equals(k_grtov) && (!inactiveItem || importInactive) && !isWare)
                data.add(new Item(itemID, k_grtov, pol_naim, UOM, UOM, "U_" + UOM, brand, "B_" + brand, country, barcode, barcode,
                        date, isWeightItem ? isWeightItem : null, null, null, composition.isEmpty() ? null : composition,
                        allowedVAT.contains(retailVAT) ? retailVAT : null, wareID, priceWare, ndsWare,
                        "RW_".equals(rateWasteID) ? null : rateWasteID, null, null, itemID, quantityPackItem));
        }
        return data;
    }

    private List<UserInvoiceDetail> importUserInvoicesFromDBF(String path) throws
            IOException, xBaseJException, ParseException, ScriptingErrorLog.SemanticErrorException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();
        Map<String, String> userInvoiceSupplierMap = new HashMap<String, String>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String post_dok = new String(importFile.getField("POST_DOK").getBytes(), "Cp1251").trim();
            String[] seriesNumber = post_dok.split("-");
            String number = seriesNumber[0];
            String series = seriesNumber.length == 1 ? null : seriesNumber[1];
            String itemID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String userInvoiceDetailSID = "SD_" + number + series + itemID;
            String dateString = new String(importFile.getField("D_PRIH").getBytes(), "Cp1251").trim();
            Date dateShipment = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
            Double quantityShipmentDetail = new Double(new String(importFile.getField("N_MAT").getBytes(), "Cp1251").trim());
            String supplierID = new String(importFile.getField("K_POST").getBytes(), "Cp1251").trim();
            if (userInvoiceSupplierMap.containsKey(post_dok))
                supplierID = userInvoiceSupplierMap.get(post_dok);
            else
                userInvoiceSupplierMap.put(post_dok, supplierID);

            String warehouseID = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            String supplierWarehouse = supplierID + "WH";
            Double priceShipmentDetail = new Double(new String(importFile.getField("N_IZG").getBytes(), "Cp1251").trim());
            Double retailPriceShipmentDetail = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            Double retailMarkupShipmentDetail = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());
            String contractID = new String(importFile.getField("K_CONT").getBytes(), "Cp1251").trim();

            if ((seriesNumber.length != 1) && (supplierID.startsWith("ПС")) && (quantityShipmentDetail != 0))
                data.add(new UserInvoiceDetail(number, series, true, true, userInvoiceDetailSID, dateShipment, itemID, false,
                        quantityShipmentDetail, supplierID, warehouseID, supplierWarehouse, priceShipmentDetail, null, null, null, null,
                        retailPriceShipmentDetail, retailMarkupShipmentDetail, null, contractID.isEmpty() ? null : contractID));
        }
        return data;
    }


    private List<PriceListStore> importPriceListStoreFromDBF(String postvarPath, String strvarPath, Integer numberOfItems) throws
            IOException, xBaseJException, ParseException {

        if (!(new File(postvarPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + postvarPath + " не найден");

        if (!(new File(strvarPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + strvarPath + " не найден");

        Map<String, Object[]> postvarMap = new HashMap<String, Object[]>();

        DBF importPostvarFile = new DBF(postvarPath);
        int totalRecordCount = importPostvarFile.getRecordCount();

        for (int i = 0; i < totalRecordCount; i++) {
            importPostvarFile.read();

            String supplier = new String(importPostvarFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String item = new String(importPostvarFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            Double price = new Double(new String(importPostvarFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            String dateString = new String(importPostvarFile.getField("DBANNED").getBytes(), "Cp1251").trim();
            Date date = dateString.isEmpty() ? null : DateUtils.parseDate(dateString, new String[]{"yyyymmdd"});

            postvarMap.put(supplier+item, new Object[]{price, date});
        }

        List<PriceListStore> data = new ArrayList<PriceListStore>();

        DBF importStrvarFile = new DBF(strvarPath);
        totalRecordCount = importStrvarFile.getRecordCount();

        for (int i = 0; i < totalRecordCount; i++) {

            if (numberOfItems != null && data.size() >= numberOfItems)
                break;

            importStrvarFile.read();

            String supplier = new String(importStrvarFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String departmentStore = new String(importStrvarFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            String item = new String(importStrvarFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String currency = "BLR";
            Double price = new Double(new String(importStrvarFile.getField("N_CENU").getBytes(), "Cp1251").trim());

            Object[] priceDate = postvarMap.get(supplier+item);
            if (departmentStore.length() >= 2 && supplier.startsWith("ПС")) {
                Date date = priceDate == null ? null : (Date) priceDate[1];
                price = (price == 0) ? (priceDate == null ? null : (Double) priceDate[0]) : price;
                if (price!=null && (date == null || date.before(new Date(System.currentTimeMillis()))))
                    data.add(new PriceListStore(supplier + departmentStore, item, supplier, departmentStore, currency, price, true, true));
            }
        }
        return data;
    }

    private List<PriceListSupplier> importPriceListSuppliersFromDBF(String postvarPath, Integer numberOfItems) throws
            IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        if (!(new File(postvarPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + postvarPath + " не найден");

        List<PriceListSupplier> data = new ArrayList<PriceListSupplier>();

        DBF importPostvarFile = new DBF(postvarPath);
        int totalRecordCount = importPostvarFile.getRecordCount();

        for (int i = 0; i < totalRecordCount; i++) {

            if (numberOfItems != null && data.size() >= numberOfItems)
                break;

            importPostvarFile.read();

            String supplier = new String(importPostvarFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String item = new String(importPostvarFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String currency = "BLR";
            Double price = new Double(new String(importPostvarFile.getField("N_CENU").getBytes(), "Cp1251").trim());

            data.add(new PriceListSupplier(supplier, item, supplier, currency, price, true));
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


    private List<LegalEntity> importLegalEntitiesFromDBF(String path, String prefixStore, Boolean importInactive, Boolean isStore) throws
            IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = getFieldValue(importFile, "K_ANA", "Cp1251", null);
            Boolean inactiveItem = "T".equals(getFieldValue(importFile, "LINACTIVE", "Cp1251", "T"));
            if (!inactiveItem || importInactive) {
                String name = getFieldValue(importFile, "POL_NAIM", "Cp1251", null);
                String address = getFieldValue(importFile, "ADDRESS", "Cp1251", null);
                String unp = getFieldValue(importFile, "UNN", "Cp1251", null);
                String okpo = getFieldValue(importFile, "OKPO", "Cp1251", null);
                String phone = getFieldValue(importFile, "TEL", "Cp1251", null);
                String email = getFieldValue(importFile, "EMAIL", "Cp1251", null);
                String account = getFieldValue(importFile, "ACCOUNT", "Cp1251", null);
                String companyStore = getFieldValue(importFile, "K_JUR", "Cp1251", null);
                String k_bank = getFieldValue(importFile, "K_BANK", "Cp1251", null);
                String[] ownership = getAndTrimOwnershipFromName(name);
                String nameCountry = "БЕЛАРУСЬ";
                String type = k_ana.substring(0, 2);
                Boolean isCompany = "ЮР".equals(type);
                Boolean isSupplier = "ПС".equals(type);
                Boolean isCustomer = "ПК".equals(type);
                if (isStore) {
                    if (prefixStore.equals(type))
                        data.add(new Store(k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                } else if (isCompany || isSupplier || isCustomer)
                    data.add(new LegalEntity(k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1],
                            ownership[0], account, isCompany ? (k_ana + "ТС") : null, isCompany ? ownership[2] : null,
                            k_bank, nameCountry, isSupplier ? true : null, isCompany ? true : null,
                            isCustomer ? true : null));
            }
        }
        return data;
    }

    private List<Warehouse> importWarehousesFromDBF(String path, Boolean importInactive) throws
            IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<Warehouse> data = new ArrayList<Warehouse>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            if (!inactiveItem || importInactive) {
                String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String address = new String(importFile.getField("ADDRESS").getBytes(), "Cp1251").trim();
                String type = k_ana.substring(0, 2);
                Boolean isSupplier = "ПС".equals(type);
                Boolean isCustomer = "ПК".equals(type);
                if (isSupplier || isCustomer)
                    data.add(new Warehouse(k_ana, null, k_ana + "WH", "Склад " + name, address));
            }
        }
        return data;
    }


    private List<DepartmentStore> importDepartmentStoresFromDBF(String path, Boolean importInactive, String
            pathStores) throws IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importStores = new DBF(pathStores);
        Map<String, String> storeDepartmentStoreMap = new HashMap<String, String>();
        for (int i = 0; i < importStores.getRecordCount(); i++) {

            importStores.read();
            storeDepartmentStoreMap.put(new String(importStores.getField("K_SKL").getBytes(), "Cp1251").trim(),
                    new String(importStores.getField("K_SKLP").getBytes(), "Cp1251").trim());
        }

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<DepartmentStore> data = new ArrayList<DepartmentStore>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            if ("СК".equals(k_ana.substring(0, 2)) && (!inactiveItem || importInactive)) {
                String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String store = storeDepartmentStoreMap.get(k_ana);
                String[] ownership = getAndTrimOwnershipFromName(name);
                if (store != null)
                    data.add(new DepartmentStore(k_ana, ownership[2], store));
            }
        }
        return data;
    }

    private List<Bank> importBanksFromDBF(String path) throws IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<Bank> data = new ArrayList<Bank>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_bank = new String(importFile.getField("K_BANK").getBytes(), "Cp1251").trim();
            String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String address = new String(importFile.getField("ADDRESS").getBytes(), "Cp1251").trim();
            String department = new String(importFile.getField("DEPART").getBytes(), "Cp1251").trim();
            String mfo = new String(importFile.getField("K_MFO").getBytes(), "Cp1251").trim();
            String cbu = new String(importFile.getField("CBU").getBytes(), "Cp1251").trim();
            data.add(new Bank(k_bank, name, address, department, mfo, cbu));
        }
        return data;
    }

    private List<RateWaste> importRateWastesFromDBF(String path) throws IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<RateWaste> data = new ArrayList<RateWaste>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String rateWasteID = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            Double coef = new Double(new String(importFile.getField("KOEFF").getBytes(), "Cp1251").trim());
            String country = "БЕЛАРУСЬ";
            data.add(new RateWaste(("RW_" + rateWasteID), name, coef, country));
        }
        return data;
    }

    private List<Contract> importContractsFromDBF(String path) throws IOException, xBaseJException, ParseException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();
        String shortNameCurrency = "BLR";

        List<Contract> contractsList = new ArrayList<Contract>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();

            String legalEntity1ID = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String legalEntity2ID = new String(importFile.getField("DPRK").getBytes(), "Cp1251").trim();
            String contractID = new String(importFile.getField("K_CONT").getBytes(), "Cp1251").trim();
            String number = new String(importFile.getField("CFULLNAME").getBytes(), "Cp1251").trim();

            java.sql.Date dateFrom = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_VV").getBytes(), "Cp1251").trim(), new String[]{"yyyymmdd"}).getTime());
            java.sql.Date dateTo = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_END").getBytes(), "Cp1251").trim(), new String[]{"yyyymmdd"}).getTime());

            if (legalEntity1ID.startsWith("ПС"))
                contractsList.add(new Contract(contractID, legalEntity1ID, legalEntity2ID, number, dateFrom, dateTo, shortNameCurrency));
            else
                contractsList.add(new Contract(contractID, legalEntity2ID, legalEntity1ID, number, dateFrom, dateTo, shortNameCurrency));
        }
        return contractsList;
    }

    private void addIfNotContains(ItemGroup element) {
        String itemGroup = element.sid.trim() + (element.name == null ? "" : element.name.trim()) + (element.parent == null ? "" : element.parent.trim());
        if (!itemGroups.contains(itemGroup)) {
            data.add(element);
            itemGroups.add(itemGroup);
        }
    }

    private String[] getAndTrimOwnershipFromName(String name) {
        name = name == null ? "" : name;
        String ownershipName = "";
        String ownershipShortName = "";
        for (String[] ownership : ownershipsList) {
            if (name.contains(ownership[0] + " ") || name.contains(" " + ownership[0])) {
                ownershipName = ownership[1];
                ownershipShortName = ownership[0];
                name = name.replace(ownership[0], "");
            }
        }
        return new String[]{ownershipShortName, ownershipName, name};
    }

    private String getFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        try {
            return new String(importFile.getField(fieldName).getBytes(), charset).trim();
        } catch (xBaseJException e) {
            return defaultValue;
        }
    }

}