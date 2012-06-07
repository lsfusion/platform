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
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ImportLSTDataActionProperty extends ScriptingActionProperty {

    public ImportLSTDataActionProperty(ScriptingLogicsModule LM) {
        super(LM);

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        try {
            String path = getLP("importLSTDirectory").read(context).toString().trim();
            if (!"".equals(path)) {
                Boolean importInactive = getLP("importInactive").read(context) != null;
                if (getLP("importGroupItems").read(context) != null) {
                    importItemGroups(path + "/_sprgrt.dbf", context);
                    importParentGroups(path + "/_sprgrt.dbf", context);
                }

                if (getLP("importBanks").read(context) != null)
                    importBanks(path + "/_sprbank.dbf", context);

                if (getLP("importCompanies").read(context) != null)
                    importCompanies(path + "/_sprana.dbf", importInactive, context);

                if (getLP("importSuppliers").read(context) != null)
                    importSuppliers(path + "/_sprana.dbf", importInactive, context);

                if (getLP("importCustomers").read(context) != null)
                    importCustomers(path + "/_sprana.dbf", importInactive, context);

                if (getLP("importStores").read(context) != null)
                    importStores(path + "/_sprana.dbf", importInactive, context);

                if (getLP("importDepartmentStores").read(context) != null)
                    importStocks(path + "/_sprana.dbf", path + "/_storestr.dbf", importInactive, context);

                if (getLP("importRateWastes").read(context) != null)
                    importRateWastes(path + "/_sprvgrt.dbf", context);

                if (getLP("importWares").read(context) != null) {
                    importWares(path + "/_sprgrm.dbf", context);
                }

                if (getLP("importItems").read(context) != null) {
                    Object numberOfItems = getLP("importNumberItems").read(context);
                    Object numberOfItemsAtATime = getLP("importNumberItemsAtATime").read(context);
                    importItems(path + "/_sprgrm.dbf", path + "/_postvar.dbf", path + "/_grmcen.dbf", importInactive, context,
                            numberOfItems == null ? 0 : (Integer) numberOfItems, numberOfItemsAtATime == null ? 5000 : (Integer) numberOfItemsAtATime);
                }

                if (getLP("importPrices").read(context) != null) {
                    importPrices(path + "/_grmcen.dbf", context);
                }

                if (getLP("importAssortment").read(context) != null) {
                    importAssortment(path + "/_strvar.dbf", context);
                }

                if (getLP("importShipment").read(context) != null) {
                    importShipment(path + "/_ostn.dbf", context);
                }
            }
        }
        catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importParentGroups(String path, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(getLP("extSID"));
            ImportField parentGroupID = new ImportField(getLP("extSID"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLP("extSIDToObject").getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLP("extSIDToObject").getMapping(parentGroupID));

            List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
            propsParent.add(new ImportProperty(parentGroupID, getLP("parentItemGroup").getMapping(itemGroupKey),
                    LM.object(getClass("itemGroup")).getMapping(parentGroupKey)));
            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importItemGroups(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, false);

            ImportField itemGroupID = new ImportField(getLP("extSID"));
            ImportField itemGroupName = new ImportField(getLP("name"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLP("extSIDToObject").getMapping(itemGroupID));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            props.add(new ImportProperty(itemGroupID, getLP("extSID").getMapping(itemGroupKey)));
            props.add(new ImportProperty(itemGroupName, getLP("name").getMapping(itemGroupKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importWares(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importWaresFromDBF(path);

            ImportField wareIDField = new ImportField(getLP("extSID"));
            ImportField wareNameField = new ImportField(getLP("name"));

            ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                    getLP("extSIDToObject").getMapping(wareIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(wareIDField, getLP("extSID").getMapping(wareKey)));
            props.add(new ImportProperty(wareNameField, getLP("name").getMapping(wareKey)));

            ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importItems(String itemsPath, String quantityPath, String warePath, Boolean importInactive, ExecutionContext context, Integer numberOfItems, Integer numberOfItemsAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

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

    private void importPackOfItems(String itemsPath, String quantityPath, String warePath, Boolean importInactive, ExecutionContext context, Integer numberOfItemsAtATime, int i, Set<String> barcodes) throws SQLException, IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException {
        List<List<Object>> data = importItemsFromDBF(itemsPath, quantityPath, warePath, importInactive, numberOfItemsAtATime, numberOfItemsAtATime * i, barcodes);
        if (data == null) return;

        ImportField itemIDField = new ImportField(getLP("extSID"));
        ImportField itemGroupIDField = new ImportField(getLP("extSID"));
        ImportField itemCaptionField = new ImportField(getLP("name"));
        ImportField UOMIDField = new ImportField(getLP("name"));
        ImportField nameUOMField = new ImportField(getLP("name"));
        ImportField brandIDField = new ImportField(getLP("name"));
        ImportField nameBrandField = new ImportField(getLP("name"));
        ImportField countryIDField = new ImportField(getLP("extSIDCountry"));
        ImportField nameCountryField = new ImportField(getLP("name"));
        ImportField barcodeField = new ImportField(getLP("idBarcode"));
        ImportField dateField = new ImportField(getLP("date"));
        ImportField importerPriceField = new ImportField(getLP("importerPriceItemDate"));
        ImportField percentWholesaleMarkItemField = new ImportField(getLP("percentWholesaleMarkItem"));
        ImportField isFixPriceItemField = new ImportField(getLP("isFixPriceItem"));
        ImportField isLoafCutItemField = new ImportField(getLP("isLoafCutItem"));
        ImportField isWeightItemField = new ImportField(getLP("isWeightItem"));
        ImportField compositionField = new ImportField(getLP("compositionScalesItem"));
        ImportField dataSuppliersRangeItemField = new ImportField(getLP("valueRate"));
        ImportField valueRetailRangeItemField = new ImportField(getLP("valueRate"));
        ImportField quantityPackItemField = new ImportField(getLP("quantityPackItem"));
        ImportField wareIDField = new ImportField(getLP("extSID"));
        ImportField priceWareField = new ImportField(getLP("warePriceDate"));
        ImportField ndsWareField = new ImportField(getLP("valueRate"));
        ImportField rateWasteIDField = new ImportField(getLP("extSID"));

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                getLP("extSIDToObject").getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLP("extSIDToObject").getMapping(itemGroupIDField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) getClass("UOM"),
                getLP("extSIDToObject").getMapping(UOMIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) getClass("brand"),
                getLP("extSIDToObject").getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass)getClass("country"),
                getLP("extSIDToCountry").getMapping(countryIDField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) getClass("barcode"),
                getLP("barcodeIdDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLP("valueCurrentRangeValue").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLP("valueCurrentRangeValue").getMapping(valueRetailRangeItemField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                getLP("extSIDToObject").getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLP("valueCurrentRangeValue").getMapping(ndsWareField));

        ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) getClass("rateWaste"),
                getLP("extSIDToObject").getMapping(rateWasteIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, getLP("itemGroupItem").getMapping(itemKey),
                LM.object(getClass("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, getLP("extSID").getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, getLP("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(UOMIDField, getLP("extSID").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLP("name").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLP("shortName").getMapping(UOMKey)));
        props.add(new ImportProperty(UOMIDField, getLP("UOMItem").getMapping(itemKey),
                LM.object(getClass("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, getLP("name").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, getLP("extSID").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, getLP("brandItem").getMapping(itemKey),
                LM.object(getClass("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(countryIDField, getLP("extSIDCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, getLP("name").getMapping(countryKey)));
        props.add(new ImportProperty(countryIDField, getLP("countryItem").getMapping(itemKey),
                LM.object(getClass("country")).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, getLP("idBarcode").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, getLP("dataDateBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, getLP("skuBarcode").getMapping(barcodeKey),
                LM.object(getClass("item")).getMapping(itemKey)));

        props.add(new ImportProperty(importerPriceField, getLP("importerPriceItemDate").getMapping(itemKey, dateField)));
        props.add(new ImportProperty(percentWholesaleMarkItemField, getLP("percentWholesaleMarkItem").getMapping(itemKey)));
        props.add(new ImportProperty(isFixPriceItemField, getLP("isFixPriceItem").getMapping(itemKey)));
        props.add(new ImportProperty(isLoafCutItemField, getLP("isLoafCutItem").getMapping(itemKey)));
        props.add(new ImportProperty(isWeightItemField, getLP("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, getLP("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(dataSuppliersRangeItemField, getLP("supplierRangeItemDate").getMapping(itemKey, dateField, supplierRangeKey),
                LM.object(getClass("range")).getMapping(supplierRangeKey)));
        props.add(new ImportProperty(valueRetailRangeItemField, getLP("retailRangeItemDate").getMapping(itemKey, dateField, retailRangeKey),
                LM.object(getClass("range")).getMapping(retailRangeKey)));
        props.add(new ImportProperty(quantityPackItemField, getLP("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, getLP("wareItem").getMapping(itemKey),
                LM.object(getClass("ware")).getMapping(wareKey)));

//        props.add(new ImportProperty(wareIDField, getLP("extSID").getMapping(wareKey))); // нельзя включать, потому то будут проблемы, если ссылается на товар, который не lgrmsec
        props.add(new ImportProperty(priceWareField, getLP("warePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, getLP("rangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(getClass("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(rateWasteIDField, getLP("rateWasteItem").getMapping(itemKey),
                LM.object(getClass("rateWaste")).getMapping(rateWasteKey)));

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, UOMIDField,
                nameUOMField, nameBrandField, brandIDField, countryIDField, nameCountryField, barcodeField, dateField,
                importerPriceField, percentWholesaleMarkItemField, isFixPriceItemField, isLoafCutItemField, isWeightItemField,
                compositionField, dataSuppliersRangeItemField, valueRetailRangeItemField, quantityPackItemField, wareIDField,
                priceWareField, ndsWareField, rateWasteIDField), data);

        DataSession session = createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, supplierRangeKey, retailRangeKey, wareKey, rangeKey, rateWasteKey), props);
        service.synchronize(true, false);
        if (session.hasChanges()) {
            String result = applySession(session);
            if (result != null)
                context.addAction(new MessageClientAction(result, "Ошибка"));
        }
        session.close();
    }

    private void importPrices(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            int count = 200000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importPricesFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField itemIDField = new ImportField(getLP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLP("extSID"));
                ImportField dateField = new ImportField(getLP("date"));
                ImportField priceField = new ImportField(getLP("dataRetailPriceItemDepartmentDate"));
                ImportField markupField = new ImportField(getLP("dataMarkupItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLP("extSIDToObject").getMapping(departmentStoreIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceField, getLP("dataRetailPriceItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));
                props.add(new ImportProperty(markupField, getLP("dataMarkupItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));

                ImportTable table = new ImportTable(Arrays.asList(itemIDField, departmentStoreIDField, dateField, priceField, markupField/*priceWareField, ndsWareField*/), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, departmentStoreKey/*, wareKey, rangeKey*/), props);
                service.synchronize(true, false);
                if (session.hasChanges()) {
                    String result = applySession(session);
                    if (result != null)
                        context.addAction(new MessageClientAction(result, "Ошибка"));
                }
                session.close();
                System.out.println("done prices " + start);
            }
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void importShipment(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            int count = 20000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importShipmentFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField shipmentField = new ImportField(getLP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLP("extSID"));
                ImportField supplierIDField = new ImportField(getLP("extSID"));

                ImportField waybillShipmentField = new ImportField(getLP("numberObject"));
                ImportField seriesWaybillShipmentField = new ImportField(getLP("seriesObject"));
                ImportField dateShipmentField = new ImportField(getLP("dateShipment"));

                ImportField itemIDField = new ImportField(getLP("extSID"));
                ImportField shipmentDetailIDField = new ImportField(getLP("extSID"));
                ImportField quantityShipmentDetailField = new ImportField(getLP("quantityShipmentDetail"));
                ImportField supplierPriceShipmentDetail = new ImportField(getLP("supplierPriceShipmentDetail"));
                ImportField importerPriceShipmentDetail = new ImportField(getLP("importerPriceShipmentDetail"));
                ImportField retailPriceShipmentDetailField = new ImportField(getLP("retailPriceShipmentDetail"));
                ImportField retailMarkupShipmentDetailField = new ImportField(getLP("retailMarkupShipmentDetail"));
                ImportField dataSuppliersRangeField = new ImportField(getLP("valueRate"));
                ImportField valueRetailRangeField = new ImportField(getLP("valueRate"));
                ImportField toShowWareField = new ImportField(getLP("toShowWareShipment"));

                ImportKey<?> shipmentKey = new ImportKey((ConcreteCustomClass) getClass("shipment"),
                        getLP("numberSeriesToShipment").getMapping(waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                        getLP("extSIDToObject").getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLP("extSIDToObject").getMapping(departmentStoreIDField));

                ImportKey<?> shipmentDetailKey = new ImportKey((ConcreteCustomClass) getClass("shipmentDetail"),
                        getLP("sidNumberSeriesToShipmentDetail").getMapping(shipmentDetailIDField, waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> supplierRangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                        getLP("valueCurrentRangeValue").getMapping(dataSuppliersRangeField));

                ImportKey<?> retailRangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                        getLP("valueCurrentRangeValue").getMapping(valueRetailRangeField));


                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(waybillShipmentField, getLP("numberObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(seriesWaybillShipmentField, getLP("seriesObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(dateShipmentField, getLP("dateShipment").getMapping(shipmentKey)));
                props.add(new ImportProperty(departmentStoreIDField, getLP("departmentStoreShipment").getMapping(shipmentKey),
                        LM.object(getClass("departmentStore")).getMapping(departmentStoreKey)));
                props.add(new ImportProperty(supplierIDField, getLP("supplierShipment").getMapping(shipmentKey),
                        LM.object(getClass("supplier")).getMapping(supplierKey)));
                props.add(new ImportProperty(toShowWareField, getLP("toShowWareShipment").getMapping(shipmentKey)));

                props.add(new ImportProperty(shipmentDetailIDField, getLP("sidShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(quantityShipmentDetailField, getLP("quantityShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(supplierPriceShipmentDetail, getLP("supplierPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(importerPriceShipmentDetail, getLP("importerPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailPriceShipmentDetailField, getLP("retailPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailMarkupShipmentDetailField, getLP("retailMarkupShipmentDetail").getMapping(shipmentDetailKey)));

                props.add(new ImportProperty(dataSuppliersRangeField, getLP("supplierRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */supplierRangeKey),
                        LM.object(getClass("range")).getMapping(supplierRangeKey)));
                props.add(new ImportProperty(valueRetailRangeField, getLP("retailRangeShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */retailRangeKey),
                        LM.object(getClass("range")).getMapping(retailRangeKey)));

                props.add(new ImportProperty(itemIDField, getLP("itemShipmentDetail").getMapping(shipmentDetailKey),
                        LM.object(getClass("item")).getMapping(itemKey)));

                props.add(new ImportProperty(shipmentField, getLP("shipmentShipmentDetail").getMapping(shipmentDetailKey),
                        LM.object(getClass("shipment")).getMapping(shipmentKey)));

                ImportTable table = new ImportTable(Arrays.asList(waybillShipmentField, seriesWaybillShipmentField,
                        departmentStoreIDField, supplierIDField, dateShipmentField, itemIDField, shipmentDetailIDField,
                        quantityShipmentDetailField, supplierPriceShipmentDetail, importerPriceShipmentDetail, retailPriceShipmentDetailField,
                        retailMarkupShipmentDetailField, dataSuppliersRangeField, valueRetailRangeField, toShowWareField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(shipmentKey, supplierKey, departmentStoreKey, shipmentDetailKey, itemKey, supplierRangeKey, retailRangeKey), props);
                service.synchronize(true, false);
                if (session.hasChanges()) {
                    String result = applySession(session);
                    if (result != null)
                        context.addAction(new MessageClientAction(result, "Ошибка"));
                }
                session.close();

                System.out.println("done shipment " + start);
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

    private void importAssortment(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {

            int count = 50000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importAssortmentFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportField itemIDField = new ImportField(getLP("extSID"));
                ImportField supplierIDField = new ImportField(getLP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLP("extSID"));
                ImportField isSupplierItemDepartmentField = new ImportField(getLP("name"));
                ImportField priceSupplierItemDepartmentField = new ImportField(getLP("dataPriceSupplierItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                        getLP("extSIDToObject").getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLP("extSIDToObject").getMapping(departmentStoreIDField));

                ImportKey<?> logicalKey = new ImportKey((ConcreteCustomClass) getClass("yesNo"),
                        getLP("classSIDToYesNo").getMapping(isSupplierItemDepartmentField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceSupplierItemDepartmentField, getLP("dataPriceSupplierItemDepartmentDate").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate)));
                props.add(new ImportProperty(isSupplierItemDepartmentField, getLP("dataIsSupplierItemDepartmentDate").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate),
                        LM.object(getClass("yesNo")).getMapping(logicalKey)));

                ImportTable table = new ImportTable(Arrays.asList(itemIDField, supplierIDField, departmentStoreIDField, isSupplierItemDepartmentField, priceSupplierItemDepartmentField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, itemKey, departmentStoreKey, logicalKey), props);
                service.synchronize(true, false);
                if (session.hasChanges()) {
                    String result = applySession(session);
                    if (result != null)
                        context.addAction(new MessageClientAction(result, "Ошибка"));
                }
                session.close();

                System.out.println("done assortment " + start);
            }

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importCompanies(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ЮР");

            ImportField companyIDField = new ImportField(getLP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLP("name"));
            ImportField legalAddressField = new ImportField(getLP("name"));
            ImportField unpField = new ImportField(getLP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLP("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLP("name"));
            ImportField nameOwnershipField = new ImportField(getLP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLP("numberAccount"));

            ImportField chainStoresIDField = new ImportField(getLP("extSID"));
            ImportField nameChainStoresField = new ImportField(getLP("name"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) getClass("company"),
                    getLP("extSIDToObject").getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLP("accountNumber").getMapping(accountField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLP("extSIDToObject").getMapping(chainStoresIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(companyIDField, getLP("extSID").getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("name").getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("fullNameLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(legalAddressField, getLP("addressLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLP("UNPLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(okpoField, getLP("OKPOLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(phoneField, getLP("phoneLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLP("emailLegalEntity").getMapping(companyKey)));

            props.add(new ImportProperty(nameOwnershipField, getLP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("ownershipLegalEntity").getMapping(companyKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(companyIDField, getLP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("company")).getMapping(companyKey)));

            props.add(new ImportProperty(chainStoresIDField, getLP("extSID").getMapping(chainStoresKey)));
            props.add(new ImportProperty(nameChainStoresField, getLP("name").getMapping(chainStoresKey)));

            ImportTable table = new ImportTable(Arrays.asList(companyIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, chainStoresIDField, nameChainStoresField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(companyKey, ownershipKey, accountKey, chainStoresKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importSuppliers(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ПС");

            ImportField supplierIDField = new ImportField(getLP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLP("name"));
            ImportField legalAddressField = new ImportField(getLP("name"));
            ImportField unpField = new ImportField(getLP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLP("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLP("name"));
            ImportField nameOwnershipField = new ImportField(getLP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLP("numberAccount"));
            ImportField bankIDField = new ImportField(getLP("extSID"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                    getLP("extSIDToObject").getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLP("accountNumber").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLP("extSIDToObject").getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(supplierIDField, getLP("extSID").getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("name").getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("fullNameLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(legalAddressField, getLP("addressLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLP("UNPLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(okpoField, getLP("OKPOLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(phoneField, getLP("phoneLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLP("emailLegalEntity").getMapping(supplierKey)));

            props.add(new ImportProperty(nameOwnershipField, getLP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("ownershipLegalEntity").getMapping(supplierKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(supplierIDField, getLP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("supplier")).getMapping(supplierKey)));
            props.add(new ImportProperty(bankIDField, getLP("bankAccount").getMapping(accountKey),
                    LM.object(getClass("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(supplierIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importCustomers(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "ПК");

            ImportField customerIDField = new ImportField(getLP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLP("name"));
            ImportField legalAddressField = new ImportField(getLP("name"));
            ImportField unpField = new ImportField(getLP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLP("phoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLP("name"));
            ImportField nameOwnershipField = new ImportField(getLP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLP("numberAccount"));
            ImportField bankIDField = new ImportField(getLP("extSID"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) getClass("customer"),
                    getLP("extSIDToObject").getMapping(customerIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLP("accountNumber").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLP("extSIDToObject").getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(customerIDField, getLP("extSID").getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("name").getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLP("fullNameLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(legalAddressField, getLP("addressLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLP("UNPLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(okpoField, getLP("OKPOLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(phoneField, getLP("phoneLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLP("emailLegalEntity").getMapping(customerKey)));

            props.add(new ImportProperty(nameOwnershipField, getLP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLP("ownershipLegalEntity").getMapping(customerKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(customerIDField, getLP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("customer")).getMapping(customerKey)));
            props.add(new ImportProperty(bankIDField, getLP("bankAccount").getMapping(accountKey),
                    LM.object(getClass("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(customerIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(customerKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importStores(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, "МГ");

            ImportField storeIDField = new ImportField(getLP("extSID"));
            ImportField nameStoreField = new ImportField(getLP("name"));
            ImportField addressStoreField = new ImportField(getLP("name"));
            ImportField companyIDField = new ImportField(getLP("extSID"));
            ImportField chainStoresIDField = new ImportField(getLP("extSID"));
            ImportField storeTypeField = new ImportField(getLP("name"));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLP("extSIDToObject").getMapping(storeIDField));

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) getClass("company"),
                    getLP("extSIDToObject").getMapping(companyIDField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLP("extSIDToObject").getMapping(chainStoresIDField));

            ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) getClass("storeType"),
                    getLP("storeTypeNameChainStores").getMapping(storeTypeField, chainStoresIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, getLP("extSID").getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, getLP("name").getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, getLP("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, getLP("companyStore").getMapping(storeKey),
                    LM.object(getClass("company")).getMapping(companyKey)));

            props.add(new ImportProperty(storeTypeField, getLP("name").getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, getLP("storeTypeStore").getMapping(storeKey),
                    LM.object(getClass("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(chainStoresIDField, getLP("chainStoresStoreType").getMapping(storeTypeKey),
                    LM.object(getClass("chainStores")).getMapping(chainStoresKey)));

            ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, companyIDField, storeTypeField, chainStoresIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, companyKey, chainStoresKey, storeTypeKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importStocks(String path, String storesPath, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importDepartmentStoresFromDBF(path, importInactive, storesPath);

            ImportField departmentStoreIDField = new ImportField(getLP("extSID"));
            ImportField nameDepartmentStoreField = new ImportField(getLP("name"));
            ImportField storeIDField = new ImportField(getLP("extSID"));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                    getLP("extSIDToObject").getMapping(departmentStoreIDField));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLP("extSIDToObject").getMapping(storeIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(departmentStoreIDField, getLP("extSID").getMapping(departmentStoreKey)));
            props.add(new ImportProperty(nameDepartmentStoreField, getLP("name").getMapping(departmentStoreKey)));
            props.add(new ImportProperty(storeIDField, getLP("storeDepartmentStore").getMapping(departmentStoreKey),
                    LM.object(getClass("store")).getMapping(storeKey)));


            ImportTable table = new ImportTable(Arrays.asList(departmentStoreIDField, nameDepartmentStoreField, storeIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importBanks(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importBanksFromDBF(path);

            ImportField bankIDField = new ImportField(getLP("extSID"));
            ImportField nameBankField = new ImportField(getLP("name"));
            ImportField addressBankField = new ImportField(getLP("name"));
            ImportField departmentBankField = new ImportField(getLP("departmentBank"));
            ImportField mfoBankField = new ImportField(getLP("MFOBank"));
            ImportField cbuBankField = new ImportField(getLP("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLP("extSIDToObject").getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, getLP("extSID").getMapping(bankKey)));
            props.add(new ImportProperty(nameBankField, getLP("name").getMapping(bankKey)));
            props.add(new ImportProperty(addressBankField, getLP("addressBankDate").getMapping(bankKey, defaultDate)));
            props.add(new ImportProperty(departmentBankField, getLP("departmentBank").getMapping(bankKey)));
            props.add(new ImportProperty(mfoBankField, getLP("MFOBank").getMapping(bankKey)));
            props.add(new ImportProperty(cbuBankField, getLP("CBUBank").getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(bankIDField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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

    private void importRateWastes(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importRateWastesFromDBF(path);

            ImportField rateWasteIDField = new ImportField(getLP("extSID"));
            ImportField nameRateWasteField = new ImportField(getLP("name"));
            ImportField percentRateWasteField = new ImportField(getLP("percentRateWaste"));

            ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) getClass("rateWaste"),
                    getLP("extSIDToObject").getMapping(rateWasteIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(rateWasteIDField, getLP("extSID").getMapping(rateWasteKey)));
            props.add(new ImportProperty(nameRateWasteField, getLP("name").getMapping(rateWasteKey)));
            props.add(new ImportProperty(percentRateWasteField, getLP("percentRateWaste").getMapping(rateWasteKey)));

            ImportTable table = new ImportTable(Arrays.asList(rateWasteIDField, nameRateWasteField, percentRateWasteField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(rateWasteKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = applySession(session);
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
                String UOM = new String(itemsImportFile.getField("K_IZM").getBytes(), "Cp1251").trim();
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
                    data.add(Arrays.asList((Object) itemID, k_grtov, pol_naim, "U_" + UOM, UOM, brand, "B_" + brand, "C_" + country, country, barcode,
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
                        retailMarkupShipmentDetail, suppliersRange, retailRange, true));
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