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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Date;
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
                        importDepartmentStoresFromDBF(path + "//_sprana.dbf", importInactive, path + "//_storestr.dbf",
                                prefixStore) : null);

                importData.setRateWastesList((getLCP("importRateWastes").read(context) != null) ?
                        importRateWastesFromDBF(path + "//_sprvgrt.dbf") : null);

                importData.setWaresList((getLCP("importWares").read(context) != null) ?
                        importWaresFromDBF(path + "//_sprgrm.dbf") : null);

                importData.setItemsList((getLCP("importItems").read(context) != null) ?
                        importItemsFromDBF(path + "//_sprgrm.dbf", path + "//_postvar.dbf", numberOfItems, importInactive) : null);

                importData.setPriceListStoresList((getLCP("importPriceListStores").read(context) != null) ?
                        importPriceListStoreFromDBF(path + "//_postvar.dbf", path + "//_strvar.dbf", prefixStore, numberOfPriceLists) : null);

                importData.setPriceListSuppliersList((getLCP("importPriceListSuppliers").read(context) != null) ?
                        importPriceListSuppliersFromDBF(path + "//_postvar.dbf", numberOfPriceLists) : null);

                importData.setUserInvoicesList((getLCP("importUserInvoices").read(context) != null) ?
                        importUserInvoicesFromDBF(path + "//_sprcont.dbf", path + "//_ostn.dbf") : null);

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

            if (!pol_naim.isEmpty()) {

                if (!group2.isEmpty() && (!group3.isEmpty())) {

                    if (!parents) {
                        //id - name - idParent(null)
                        addIfNotContains(new ItemGroup((group3.substring(0, 3) + "/" + groupTop), group3, null));
                        addIfNotContains(new ItemGroup((group2 + "/" + group3.substring(0, 3) + "/" + groupTop), group2, null));
                        addIfNotContains(new ItemGroup((group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + groupTop), group1, null));
                        addIfNotContains(new ItemGroup(k_grtov, pol_naim, null));
                    } else {
                        //id - name(null) - idParent
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

            Boolean isWare = getBooleanFieldValue(importFile, "LGRMSEC", "Cp1251", false);
            String wareID = getFieldValue(importFile, "K_GRMAT", "Cp1251", null);
            String pol_naim = getFieldValue(importFile, "POL_NAIM", "Cp1251", null);
            BigDecimal price = getBigDecimalFieldValue(importFile, "CENUOSEC", "Cp1251", null);

            if (!wareID.isEmpty() && isWare)
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

        Map<String, BigDecimal> quantities = new HashMap<String, BigDecimal>();

        for (int i = 0; i < totalRecordCount; i++) {
            quantityImportFile.read();

            String itemID = getFieldValue(quantityImportFile, "K_GRMAT", "Cp1251", null);
            BigDecimal quantityPackItem = getBigDecimalFieldValue(quantityImportFile, "PACKSIZE", "Cp1251", null);

            if (quantityPackItem.equals(new BigDecimal(0)))
                quantityPackItem = new BigDecimal(1);
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
            String barcode = getFieldValue(itemsImportFile, "K_GRUP", "Cp1251", null);
            int counter = 1;
            if (barcodes.contains(barcode)) {
                while (barcodes.contains(barcode + "_" + counter)) {
                    counter++;
                }
                barcode += "_" + counter;
            }
            barcodes.add(barcode);
            Boolean inactiveItem = getBooleanFieldValue(itemsImportFile, "LINACTIVE", "Cp1251", false);
            String itemID = getFieldValue(itemsImportFile, "K_GRMAT", "Cp1251", null);
            String name = getFieldValue(itemsImportFile, "POL_NAIM", "Cp1251", null);
            String k_grtov = getFieldValue(itemsImportFile, "K_GRTOV", "Cp1251", null);
            if (k_grtov.endsWith("."))
                k_grtov = k_grtov.substring(0, k_grtov.length() - 1);
            String UOM = getFieldValue(itemsImportFile, "K_IZM", "Cp1251", null);
            String brand = getFieldValue(itemsImportFile, "BRAND", "Cp1251", null);
            String country = getFieldValue(itemsImportFile, "MANFR", "Cp1251", null);
            if ("РБ".equals(country) || "Беларусь".equals(country))
                country = "БЕЛАРУСЬ";
            Date date = getDateFieldValue(itemsImportFile, "P_TIME", "Cp1251", null);
            Boolean isWeightItem = getBooleanFieldValue(itemsImportFile, "LWEIGHT", "Cp1251", false);
            String composition = "";
            if (itemsImportFile.getField("ENERGVALUE").getBytes() != null) {
                composition = getFieldValue(itemsImportFile, "ENERGVALUE", "Cp1251", "").replace("\n", "").replace("\r", "");
            }
            BigDecimal retailVAT = getBigDecimalFieldValue(itemsImportFile, "NDSR", "Cp1251", null);
            BigDecimal quantityPackItem = quantities.containsKey(itemID) ? quantities.get(itemID) : null;
            Boolean isWare = getBooleanFieldValue(itemsImportFile, "LGRMSEC", "Cp1251", false);
            String wareID = getFieldValue(itemsImportFile, "K_GRMSEC", "Cp1251", null);
            if (wareID.isEmpty())
                wareID = null;
            String rateWasteID = "RW_" + getFieldValue(itemsImportFile, "K_VGRTOV", "Cp1251", "");

            BigDecimal priceWare = getBigDecimalFieldValue(itemsImportFile, "CENUOSEC", "Cp1251", null);
            BigDecimal ndsWare = getBigDecimalFieldValue(itemsImportFile, "NDSSEC", "Cp1251", "20");

            if (!k_grtov.isEmpty() && (!inactiveItem || importInactive) && !isWare)
                data.add(new Item(itemID, k_grtov, name, UOM, UOM, "U_" + UOM, brand, "B_" + brand, country, barcode, barcode,
                        date, isWeightItem ? isWeightItem : null, null, null, composition.isEmpty() ? null : composition,
                        allowedVAT.contains(retailVAT) ? retailVAT : null, wareID, priceWare, ndsWare,
                        "RW_".equals(rateWasteID) ? null : rateWasteID, null, null, itemID, quantityPackItem, null, null,
                        null, null));
        }
        return data;
    }

    private List<UserInvoiceDetail> importUserInvoicesFromDBF(String sprcontPath, String ostnPath) throws
            IOException, xBaseJException, ParseException, ScriptingErrorLog.SemanticErrorException {

        Map<String, String> contractSupplierMap = new HashMap<String, String>();

        if (new File(sprcontPath).exists()) {

            DBF importFile = new DBF(sprcontPath);
            int totalRecordCount = importFile.getRecordCount();

            for (int i = 0; i < totalRecordCount; i++) {
                importFile.read();
                String idLegalEntity1 = getFieldValue(importFile, "K_ANA", "Cp1251", "");
                String idLegalEntity2 = getFieldValue(importFile, "DPRK", "Cp1251", "");
                String idContract = getFieldValue(importFile, "K_CONT", "Cp1251", null);
                contractSupplierMap.put(idContract, idLegalEntity1.startsWith("ПС") ? idLegalEntity1 : idLegalEntity2);
            }
        }

        if (!(new File(ostnPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + ostnPath + " не найден");


        DBF importFile = new DBF(ostnPath);
        int totalRecordCount = importFile.getRecordCount();

        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();
        Map<String, String> userInvoiceSupplierMap = new HashMap<String, String>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String post_dok = getFieldValue(importFile, "POST_DOK", "Cp1251", null);
            String[] seriesNumber = post_dok.split("-");
            String number = seriesNumber[0];
            String series = seriesNumber.length == 1 ? null : seriesNumber[1];
            String idItem = getFieldValue(importFile, "K_GRMAT", "Cp1251", null);
            String idUserInvoiceDetail = number + series + idItem;
            Date dateShipment = getDateFieldValue(importFile, "D_PRIH", "Cp1251", null);
            BigDecimal quantityShipmentDetail = getBigDecimalFieldValue(importFile, "N_MAT", "Cp1251", null);
            String idSupplier = getFieldValue(importFile, "K_POST", "Cp1251", null);
            if (userInvoiceSupplierMap.containsKey(post_dok))
                idSupplier = userInvoiceSupplierMap.get(post_dok);
            else
                userInvoiceSupplierMap.put(post_dok, idSupplier);

            String idWarehouse = getFieldValue(importFile, "K_SKL", "Cp1251", null);
            String supplierWarehouse = idSupplier + "WH";
            BigDecimal priceShipmentDetail = getBigDecimalFieldValue(importFile, "N_IZG", "Cp1251", null);
            BigDecimal retailPriceShipmentDetail = getBigDecimalFieldValue(importFile, "N_CENU", "Cp1251", null);
            BigDecimal retailMarkupShipmentDetail = getBigDecimalFieldValue(importFile, "N_TN", "Cp1251", null);
            String idContract;
            try {
                idContract = new String(importFile.getField("K_CONT").getBytes(), "Cp1251").trim();
                idContract = idSupplier.equals(contractSupplierMap.get(idContract)) ? idContract : null;
            } catch (xBaseJException e) {
                idContract = null;
            }
            if ((seriesNumber.length != 1) && (idSupplier.startsWith("ПС")) && (!quantityShipmentDetail.equals(new BigDecimal(0))))
                data.add(new UserInvoiceDetail(number, series, true, true, idUserInvoiceDetail, dateShipment, idItem, false,
                        quantityShipmentDetail, idSupplier, idWarehouse, supplierWarehouse, priceShipmentDetail, null, null, null, null,
                        retailPriceShipmentDetail, retailMarkupShipmentDetail, null, idContract));
        }
        return data;
    }


    private List<PriceListStore> importPriceListStoreFromDBF(String postvarPath, String strvarPath,
                                                             String prefixStore, Integer numberOfItems) throws
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

            String supplier = getFieldValue(importPostvarFile, "K_ANA", "Cp1251", null);
            String item = getFieldValue(importPostvarFile, "K_GRMAT", "Cp1251", null);
            BigDecimal price = getBigDecimalFieldValue(importPostvarFile, "N_CENU", "Cp1251", null);
            Date date = getDateFieldValue(importPostvarFile, "DBANNED", "Cp1251", null);

            postvarMap.put(supplier + item, new Object[]{price, date});
        }

        List<PriceListStore> data = new ArrayList<PriceListStore>();

        DBF importStrvarFile = new DBF(strvarPath);
        totalRecordCount = importStrvarFile.getRecordCount();

        for (int i = 0; i < totalRecordCount; i++) {

            if (numberOfItems != null && data.size() >= numberOfItems)
                break;

            importStrvarFile.read();

            String supplier = getFieldValue(importStrvarFile, "K_ANA", "Cp1251", null);
            String departmentStore = getFieldValue(importStrvarFile, "K_SKL", "Cp1251", null);
            departmentStore = departmentStore.replace("МГ", prefixStore);
            String item = getFieldValue(importStrvarFile, "K_GRMAT", "Cp1251", null);
            String currency = "BLR";
            BigDecimal price = getBigDecimalFieldValue(importStrvarFile, "N_CENU", "Cp1251", null);

            Object[] priceDate = postvarMap.get(supplier + item);
            if (departmentStore.length() >= 2 && supplier.startsWith("ПС")) {
                Date date = priceDate == null ? null : (Date) priceDate[1];
                price = price.equals(new BigDecimal(0)) ? (priceDate == null ? null : (BigDecimal) priceDate[0]) : price;
                if (price != null && (date == null || date.before(new Date(System.currentTimeMillis()))))
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

            String supplier = getFieldValue(importPostvarFile, "K_ANA", "Cp1251", null);
            String item = getFieldValue(importPostvarFile, "K_GRMAT", "Cp1251", null);
            String currency = "BLR";
            BigDecimal price = getBigDecimalFieldValue(importPostvarFile, "N_CENU", "Cp1251", null);

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
            Boolean inactiveItem = getBooleanFieldValue(importFile, "LINACTIVE", "Cp1251", false);
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
            Boolean inactiveItem = getBooleanFieldValue(importFile, "LINACTIVE", "Cp1251", false);
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
            pathStores, String prefixStore) throws IOException, xBaseJException {

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
            Boolean inactiveItem = getBooleanFieldValue(importFile, "LINACTIVE", "Cp1251", false);
            if ("СК".equals(k_ana.substring(0, 2)) && (!inactiveItem || importInactive)) {
                String name = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
                String store = storeDepartmentStoreMap.get(k_ana);
                store = store == null ? null : store.replace("МГ", prefixStore);
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
            String rateWasteID = getFieldValue(importFile, "K_GRTOV", "Cp1251", "");
            String name = getFieldValue(importFile, "POL_NAIM", "Cp1251", null);
            BigDecimal coef = getBigDecimalFieldValue(importFile, "KOEFF", "Cp1251", null);
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
        List<String> contractIDs = new ArrayList<String>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();

            String legalEntity1ID = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String legalEntity2ID = new String(importFile.getField("DPRK").getBytes(), "Cp1251").trim();
            String contractID = new String(importFile.getField("K_CONT").getBytes(), "Cp1251").trim();
            String number = new String(importFile.getField("CFULLNAME").getBytes(), "Cp1251").trim();

            java.sql.Date dateFrom = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_VV").getBytes(), "Cp1251").trim(), new String[]{"yyyymmdd"}).getTime());
            java.sql.Date dateTo = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_END").getBytes(), "Cp1251").trim(), new String[]{"yyyymmdd"}).getTime());

            if (!contractIDs.contains(contractID)) {
                if (legalEntity1ID.startsWith("ПС"))
                    contractsList.add(new Contract(contractID, legalEntity1ID, legalEntity2ID, number, dateFrom, dateTo, shortNameCurrency));
                else
                    contractsList.add(new Contract(contractID, legalEntity2ID, legalEntity1ID, number, dateFrom, dateTo, shortNameCurrency));
                contractIDs.add(contractID);
            }
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

    private BigDecimal getBigDecimalFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        return BigDecimal.valueOf(Double.valueOf(getFieldValue(importFile, fieldName, charset, defaultValue)));
    }

    private java.sql.Date getDateFieldValue(DBF importFile, String fieldName, String charset, java.sql.Date defaultValue) throws UnsupportedEncodingException, ParseException {
        String dateString = getFieldValue(importFile, fieldName, charset, "");
        return dateString.isEmpty() ? defaultValue : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());

    }

    private  Boolean getBooleanFieldValue(DBF importFile, String fieldName, String charset, Boolean defaultValue) throws UnsupportedEncodingException {
        return "T".equals(getFieldValue(importFile, fieldName, charset, String.valueOf(defaultValue)));
    }

    private String getFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        try {
            return new String(importFile.getField(fieldName).getBytes(), charset).trim();
        } catch (xBaseJException e) {
            return defaultValue;
        }
    }

}