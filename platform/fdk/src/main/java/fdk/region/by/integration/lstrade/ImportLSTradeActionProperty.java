package fdk.region.by.integration.lstrade;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.*;
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

import java.io.File;
import java.io.IOException;
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
            String path = getLCP("importLSTDirectory").read(context).toString().trim();
            if (!"".equals(path)) {

                ImportData importData = new ImportData();

                Boolean importInactive = getLCP("importInactive").read(context) != null;
                importData.setImportInactive(importInactive);

                importData.setNumberOfItemsAtATime((Integer) getLCP("importNumberItemsAtATime").read(context));

                importData.setItemGroupsList((getLCP("importGroupItems").read(context) != null) ?
                        importItemGroupsFromDBF(path + "//_sprgrt.dbf", false) : null);

                importData.setParentGroupsList((getLCP("importGroupItems").read(context) != null) ?
                        importItemGroupsFromDBF(path + "//_sprgrt.dbf", true) : null);

                importData.setBanksList((getLCP("importBanks").read(context) != null) ?
                        importBanksFromDBF(path + "//_sprbank.dbf") : null);

                importData.setLegalEntitiesList((getLCP("importLegalEntities").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//_sprana.dbf", importInactive, false) : null);

                importData.setWarehousesList((getLCP("importWarehouses").read(context) != null) ?
                        importWarehousesFromDBF(path + "//_sprana.dbf", importInactive) : null);

                importData.setStoresList((getLCP("importStores").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//_sprana.dbf", importInactive, true) : null);

                importData.setDepartmentStoresList((getLCP("importDepartmentStores").read(context) != null) ?
                        importDepartmentStoresFromDBF(path + "//_sprana.dbf", importInactive, path + "//_storestr.dbf") : null);

                importData.setRateWastesList((getLCP("importRateWastes").read(context) != null) ?
                        importRateWastesFromDBF(path + "//_sprvgrt.dbf") : null);

                importData.setWaresList((getLCP("importWares").read(context) != null) ?
                        importWaresFromDBF(path + "//_sprgrm.dbf") : null);

                importData.setItemsList((getLCP("importItems").read(context) != null) ?
                        importItemsFromDBF(path + "//_sprgrm.dbf", path + "//_postvar.dbf", path + "//_grmcen.dbf", numberOfItems, importInactive) : null);

                //importData.setPricesList((getLCP("importPrices").read(context) != null) ?
                //        importPricesFromDBF(path + "//_grmcen.dbf") : null);

                importData.setAssortmentsList((getLCP("importAssortment").read(context) != null) ?
                        importAssortmentFromDBF(path + "//_strvar.dbf") : null);

                importData.setStockSuppliersList((getLCP("importAssortment").read(context) != null) ?
                        importStockSuppliersFromDBF(context, path + "//_strvar.dbf") : null);

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

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<ItemGroup>();
        itemGroups = new ArrayList<String>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String group1 = new String(importFile.getField("GROUP1").getBytes(), "Cp1251").trim();
            String group2 = new String(importFile.getField("GROUP2").getBytes(), "Cp1251").trim();
            String group3 = new String(importFile.getField("GROUP3").getBytes(), "Cp1251").trim();
            String group4 = "ВСЕ";

            if ((!"".equals(group1)) && (!"".equals(group2)) && (!"".equals(group3))) {
                if (!parents) {
                    //sid - name - parentSID(null)
                    addIfNotContains(new ItemGroup(group4, group4, null));
                    addIfNotContains(new ItemGroup((group3.substring(0, 3) + "/" + group4), group3, null));
                    addIfNotContains(new ItemGroup((group2 + "/" + group3.substring(0, 3) + "/" + group4), group2, null));
                    addIfNotContains(new ItemGroup((group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4), group1, null));
                    addIfNotContains(new ItemGroup(k_grtov, pol_naim, null));
                } else {
                    //sid - name(null) - parentSID
                    addIfNotContains(new ItemGroup(group4, null, null));
                    addIfNotContains(new ItemGroup((group3.substring(0, 3) + "/" + group4), null, group4));
                    addIfNotContains(new ItemGroup((group2 + "/" + group3.substring(0, 3) + "/" + group4), null, group3.substring(0, 3) + "/" + group4));
                    addIfNotContains(new ItemGroup((group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4), null, group2 + "/" + group3.substring(0, 3) + "/" + group4));
                    addIfNotContains(new ItemGroup(k_grtov, null, group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4));
                }
            }
        }
        return data;
    }

    private List<Ware> importWaresFromDBF(String path) throws IOException, xBaseJException {

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();
        List<Ware> data = new ArrayList<Ware>();

        for (int i = 0; i < recordCount; i++) {
            importFile.read();

            Boolean isWare = "T".equals(new String(importFile.getField("LGRMSEC").getBytes(), "Cp1251").trim());
            String wareID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            Double price = new Double(new String(importFile.getField("N_PRCEN").getBytes(), "Cp1251").trim());

            if (!"".equals(wareID) && isWare)
                data.add(new Ware(wareID, pol_naim, price));
        }
        return data;
    }

    private List<Item> importItemsFromDBF(String itemsPath, String quantityPath, String warePath, Integer numberOfItems, Boolean importInactive) throws IOException, xBaseJException, ParseException {

        if(!(new File(itemsPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + itemsPath + " не найден");

        if(!(new File(quantityPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + quantityPath + " не найден");

        if(!(new File(warePath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + warePath + " не найден");

        Set<String> barcodes = new HashSet<String>();
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
            String UOM = new String(itemsImportFile.getField("K_IZM").getBytes(), "Cp1251").trim();
            String brand = new String(itemsImportFile.getField("BRAND").getBytes(), "Cp1251").trim();
            String country = new String(itemsImportFile.getField("MANFR").getBytes(), "Cp1251").trim();
            if ("РБ".equals(country) || "Беларусь".equals(country))
                country = "БЕЛАРУСЬ";
            String dateString = new String(itemsImportFile.getField("P_TIME").getBytes(), "Cp1251").trim();
            Date date = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
            Boolean isWeightItem = "T".equals(new String(itemsImportFile.getField("LWEIGHT").getBytes(), "Cp1251").substring(0, 1));
            String composition = null;
            if (itemsImportFile.getField("FORMULA").getBytes() != null) {
                composition = new String(itemsImportFile.getField("FORMULA").getBytes(), "Cp1251").replace("\n", "").replace("\r", "");
            }
            Double retailVAT = new Double(new String(itemsImportFile.getField("NDSR").getBytes(), "Cp1251").trim());
            Double quantityPackItem = quantities.containsKey(itemID) ? quantities.get(itemID) : null;
            Boolean isWare = "T".equals(new String(itemsImportFile.getField("LGRMSEC").getBytes(), "Cp1251").substring(0, 1));
            String wareID = new String(itemsImportFile.getField("K_GRMSEC").getBytes(), "Cp1251").trim();
            if (wareID.isEmpty())
                wareID = null;
            String rateWasteID = "RW_" + new String(itemsImportFile.getField("K_VGRTOV").getBytes(), "Cp1251").trim();

            if (!"".equals(k_grtov) && (!inactiveItem || importInactive) && !isWare)
                data.add(new Item(itemID, k_grtov, pol_naim, UOM, UOM, "U_" + UOM, brand, "B_" + brand, country, barcode, barcode,
                        date, isWeightItem ? isWeightItem : null, null, null,
                        "".equals(composition) ? null : composition, allowedVAT.contains(retailVAT) ? retailVAT : null, wareID,
                        wares.containsKey(itemID) ? wares.get(itemID)[0] : null, wares.containsKey(itemID) ? wares.get(itemID)[1] : null,
                        "RW_".equals(rateWasteID) ? null : rateWasteID, null, null, null, itemID, quantityPackItem));
        }
        return data;
    }

    private List<Price> importPricesFromDBF(String path) throws IOException, xBaseJException, ParseException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        List<Price> data = new ArrayList<Price>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();
            String item = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String departmentStore = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            Date date = new java.sql.Date(DateUtils.parseDate(new String(importFile.getField("D_CEN").getBytes(), "Cp1251").trim(), new String[]{"yyyyMMdd"}).getTime());
            Double price = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            Double markup = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());
            data.add(new Price(item, departmentStore, date, price, markup));
        }
        return data;
    }

    private List<UserInvoiceDetail> importUserInvoicesFromDBF(String path) throws
            IOException, xBaseJException, ParseException, ScriptingErrorLog.SemanticErrorException {

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String post_dok[] = new String(importFile.getField("POST_DOK").getBytes(), "Cp1251").trim().split("-");
            String number = post_dok[0];
            String series = post_dok.length == 1 ? null : post_dok[1];
            String itemID = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String userInvoiceDetailSID = "SD_" + number + series + itemID;
            String dateString = new String(importFile.getField("D_PRIH").getBytes(), "Cp1251").trim();
            Date dateShipment = "".equals(dateString) ? null : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());
            Double quantityShipmentDetail = new Double(new String(importFile.getField("N_MAT").getBytes(), "Cp1251").trim());
            String supplierID = new String(importFile.getField("K_POST").getBytes(), "Cp1251").trim();
            String warehouseID = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            String supplierWarehouse = supplierID + "WH";
            Double priceShipmentDetail = new Double(new String(importFile.getField("N_IZG").getBytes(), "Cp1251").trim());
            Double retailPriceShipmentDetail = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());
            Double retailMarkupShipmentDetail = new Double(new String(importFile.getField("N_TN").getBytes(), "Cp1251").trim());

            if ((post_dok.length != 1) && (supplierID.startsWith("ПС")) && (quantityShipmentDetail != 0))
                data.add(new UserInvoiceDetail(number, series, true, true, userInvoiceDetailSID, dateShipment, itemID,
                        quantityShipmentDetail, supplierID, warehouseID, supplierWarehouse, priceShipmentDetail, null,
                        retailPriceShipmentDetail, retailMarkupShipmentDetail, null));
        }
        return data;
    }


    private List<Assortment> importAssortmentFromDBF(String path) throws
            IOException, xBaseJException {

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        List<Assortment> data = new ArrayList<Assortment>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String item = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String supplier = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String departmentStore = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();
            String currency = "BLR";
            Double price = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());

            if (departmentStore.length() >= 2 && supplier.startsWith("ПС")) {
                data.add(new Assortment(item, supplier, supplier + "ПР", departmentStore, currency, price, true));
            }
        }
        return data;
    }

    private List<StockSupplier> importStockSuppliersFromDBF(ExecutionContext context, String path) throws
            IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        List<StockSupplier> data = new ArrayList<StockSupplier>();
        List<String> stockSuppliers = new ArrayList<String>();
        Set<String> stores = new HashSet<String>();
        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String supplier = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String store = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();

            if (supplier.startsWith("ПС") && (!stores.contains(store))) {

                Object storeObject = getLCP("externalizableSID").readClasses(context.getSession(), new DataObject(store, StringClass.get(110)));
                if (!(storeObject instanceof NullValue)) {
                    LCP isDepartmentStore = LM.is(getClass("departmentStore"));
                    ImRevMap<Object, KeyExpr> keys = isDepartmentStore.getMapKeys();
                    KeyExpr key = keys.singleValue();
                    QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
                    query.addProperty("sidExternalizable", getLCP("sidExternalizable").getExpr(context.getModifier(), key));
                    query.and(isDepartmentStore.getExpr(key).getWhere());
                    query.and(getLCP("storeDepartmentStore").getExpr(context.getModifier(), key).compare(((DataObject) storeObject).getExpr(), Compare.EQUALS));
                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context.getSession().sql);

                    for (ImMap<Object, Object> entry : result.valueIt()) {
                        StockSupplier stockSupplier = new StockSupplier(supplier + "ПР", (String) entry.get("sidExternalizable"), null, true);
                        String sid = stockSupplier.departmentStore.trim() + stockSupplier.userPriceListID == null ? "" : stockSupplier.userPriceListID;
                        if (!stockSuppliers.contains(sid)) {
                            data.add(stockSupplier);
                            stockSuppliers.add(sid);
                        }
                    }
                    stores.add(store);
                }
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


    private List<LegalEntity> importLegalEntitiesFromDBF(String path, Boolean importInactive, Boolean isStore) throws
            IOException, xBaseJException {

        if(!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            Boolean inactiveItem = "T".equals(new String(importFile.getField("LINACTIVE").getBytes(), "Cp1251"));
            if (!inactiveItem || importInactive) {
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
                String nameCountry = "БЕЛАРУСЬ";
                String type = k_ana.substring(0, 2);
                Boolean isCompany = "ЮР".equals(type);
                Boolean isSupplier = "ПС".equals(type);
                Boolean isCustomer = "ПК".equals(type);
                if (isStore) {
                    if ("МГ".equals(type))
                        data.add(new Store(k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                } else if (isCompany || isSupplier || isCustomer)
                    data.add(new LegalEntity(k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1],
                            ownership[0], account, isCompany ? (k_ana + "ТС") : null, isCompany ? ownership[2] : null,
                            "BANK_" + k_bank, nameCountry, isSupplier ? true : null, isCompany ? true : null,
                            isCustomer ? true : null));
            }
        }
        return data;
    }

    private List<Warehouse> importWarehousesFromDBF(String path, Boolean importInactive) throws
            IOException, xBaseJException {

        if(!(new File(path).exists()))
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

        if(!(new File(path).exists()))
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
                data.add(new DepartmentStore(k_ana, ownership[2], store));
            }
        }
        return data;
    }

    private List<Bank> importBanksFromDBF(String path) throws IOException, xBaseJException {

        if(!(new File(path).exists()))
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
            data.add(new Bank(("BANK_" + k_bank), name, address, department, mfo, cbu));
        }
        return data;
    }

    private List<RateWaste> importRateWastesFromDBF(String path) throws IOException, xBaseJException {

        if(!(new File(path).exists()))
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

    private void addIfNotContains(ItemGroup element) {
        String itemGroup = element.sid.trim() + (element.name == null ? "" : element.name.trim()) + (element.parent == null ? "" : element.parent.trim());
        if (!itemGroups.contains(itemGroup)) {
            data.add(element);
            itemGroups.add(itemGroup);
        }
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