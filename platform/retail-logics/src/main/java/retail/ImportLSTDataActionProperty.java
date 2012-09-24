package retail;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DateClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
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
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            String path = getLCP("importLSTDirectory").read(context).toString().trim();
            if (!"".equals(path)) {
                Boolean importInactive = getLCP("importInactive").read(context) != null;
                if (getLCP("importGroupItems").read(context) != null) {
                    importItemGroups(path + "//_sprgrt.dbf", context);
                    importParentGroups(path + "//_sprgrt.dbf", context);
                }
    
                if (getLCP("importBanks").read(context) != null)
                    importBanks(path + "//_sprbank.dbf", context);
    
                if (getLCP("importCompanies").read(context) != null)
                    importCompanies(path + "//_sprana.dbf", importInactive, context);
    
                if (getLCP("importSuppliers").read(context) != null)
                    importSuppliers(path + "//_sprana.dbf", importInactive, context);
    
                if (getLCP("importCustomers").read(context) != null)
                    importCustomers(path + "//_sprana.dbf", importInactive, context);
    
                if (getLCP("importStores").read(context) != null)
                    importStores(path + "//_sprana.dbf", importInactive, context);
    
                if (getLCP("importDepartmentStores").read(context) != null)
                    importStocks(path + "//_sprana.dbf", path + "//_storestr.dbf", importInactive, context);
    
                if (getLCP("importRateWastes").read(context) != null)
                    importRateWastes(path + "//_sprvgrt.dbf", context);
    
                if (getLCP("importWares").read(context) != null) {
                    importWares(path + "//_sprgrm.dbf", context);
                }
    
                if (getLCP("importItems").read(context) != null) {
                    Object numberOfItems = getLCP("importNumberItems").read(context);
                    Object numberOfItemsAtATime = getLCP("importNumberItemsAtATime").read(context);
                    importItems(path + "//_sprgrm.dbf", path + "//_postvar.dbf", path + "//_grmcen.dbf", importInactive, context,
                            numberOfItems == null ? 0 : (Integer) numberOfItems, numberOfItemsAtATime == null ? 5000 : (Integer) numberOfItemsAtATime);
                }
    
                if (getLCP("importPrices").read(context) != null) {
                    importPrices(path + "//_grmcen.dbf", context);
                }
    
                if (getLCP("importAssortment").read(context) != null) {
                    importAssortment(path + "//_strvar.dbf", context);
                }
    
                if (getLCP("importShipment").read(context) != null) {
                    importShipment(path + "//_ostn.dbf", context);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importParentGroups(String path, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(getLCP("extSID"));
            ImportField parentGroupID = new ImportField(getLCP("extSID"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                                                      getLCP("extSIDToObject").getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                                                        getLCP("extSIDToObject").getMapping(parentGroupID));

            List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
            propsParent.add(new ImportProperty(parentGroupID, getLCP("parentItemGroup").getMapping(itemGroupKey),
                    LM.object(getClass("itemGroup")).getMapping(parentGroupKey)));
            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField itemGroupID = new ImportField(getLCP("extSID"));
            ImportField itemGroupName = new ImportField(getLCP("name"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLCP("extSIDToObject").getMapping(itemGroupID));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            props.add(new ImportProperty(itemGroupID, getLCP("extSID").getMapping(itemGroupKey)));
            props.add(new ImportProperty(itemGroupName, getLCP("name").getMapping(itemGroupKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField wareIDField = new ImportField(getLCP("extSID"));
            ImportField wareNameField = new ImportField(getLCP("name"));

            ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                    getLCP("extSIDToObject").getMapping(wareIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(wareIDField, getLCP("extSID").getMapping(wareKey)));
            props.add(new ImportProperty(wareNameField, getLCP("name").getMapping(wareKey)));

            ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
            service.synchronize(true, false);
            applySession(session);
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

        ImportField itemIDField = new ImportField(getLCP("extSID"));
        ImportField itemGroupIDField = new ImportField(getLCP("extSID"));
        ImportField itemCaptionField = new ImportField(getLCP("name"));
        ImportField UOMIDField = new ImportField(getLCP("name"));
        ImportField nameUOMField = new ImportField(getLCP("name"));
        ImportField brandIDField = new ImportField(getLCP("name"));
        ImportField nameBrandField = new ImportField(getLCP("name"));
        ImportField nameCountryField = new ImportField(getLCP("name"));
        ImportField barcodeField = new ImportField(getLCP("idBarcode"));
        ImportField dateField = new ImportField(getLCP("date"));
        ImportField importerPriceField = new ImportField(getLCP("dataImporterPriceItemDate"));
        ImportField percentWholesaleMarkItemField = new ImportField(getLCP("percentWholesaleMarkItem"));
        ImportField isFixPriceItemField = new ImportField(getLCP("isFixPriceItem"));
        ImportField isLoafCutItemField = new ImportField(getLCP("isLoafCutItem"));
        ImportField isWeightItemField = new ImportField(getLCP("isWeightItem"));
        ImportField compositionField = new ImportField(getLCP("compositionScalesItem"));
        ImportField dataSuppliersRangeItemField = new ImportField(getLCP("valueRate"));
        ImportField valueRetailVATItemField = new ImportField(getLCP("valueRate"));
        ImportField quantityPackItemField = new ImportField(getLCP("quantityPackItem"));
        ImportField wareIDField = new ImportField(getLCP("extSID"));
        ImportField priceWareField = new ImportField(getLCP("dataWarePriceDate"));
        ImportField ndsWareField = new ImportField(getLCP("valueRate"));
        ImportField rateWasteIDField = new ImportField(getLCP("extSID"));

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                getLCP("extSIDToObject").getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLCP("extSIDToObject").getMapping(itemGroupIDField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) getClass("UOM"),
                getLCP("extSIDToObject").getMapping(UOMIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) getClass("brand"),
                getLCP("extSIDToObject").getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) getClass("country"),
                getLCP("countryName").getMapping(nameCountryField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) getClass("barcode"),
                getLCP("barcodeIdDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(valueRetailVATItemField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                getLCP("extSIDToObject").getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(ndsWareField));

        ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) getClass("rateWaste"),
                getLCP("extSIDToObject").getMapping(rateWasteIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, getLCP("itemGroupItem").getMapping(itemKey),
                LM.object(getClass("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, getLCP("extSID").getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, getLCP("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(UOMIDField, getLCP("extSID").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLCP("name").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLCP("shortName").getMapping(UOMKey)));
        props.add(new ImportProperty(UOMIDField, getLCP("UOMItem").getMapping(itemKey),
                LM.object(getClass("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, getLCP("name").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, getLCP("extSID").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, getLCP("brandItem").getMapping(itemKey),
                LM.object(getClass("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(nameCountryField, getLCP("name").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, getLCP("countryItem").getMapping(itemKey),
                LM.object(getClass("country")).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, getLCP("idBarcode").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, getLCP("dataDateBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, getLCP("skuBarcode").getMapping(barcodeKey),
                LM.object(getClass("item")).getMapping(itemKey)));

        props.add(new ImportProperty(importerPriceField, getLCP("dataImporterPriceItemDate").getMapping(itemKey, dateField)));
        props.add(new ImportProperty(percentWholesaleMarkItemField, getLCP("percentWholesaleMarkItem").getMapping(itemKey)));
        props.add(new ImportProperty(isFixPriceItemField, getLCP("isFixPriceItem").getMapping(itemKey)));
        props.add(new ImportProperty(isLoafCutItemField, getLCP("isLoafCutItem").getMapping(itemKey)));
        props.add(new ImportProperty(isWeightItemField, getLCP("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, getLCP("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(dataSuppliersRangeItemField, getLCP("dataSupplierVATItemDate").getMapping(itemKey, dateField, supplierVATKey),
                LM.object(getClass("range")).getMapping(supplierVATKey)));
        props.add(new ImportProperty(valueRetailVATItemField, getLCP("dataRetailVATItemDate").getMapping(itemKey, dateField, retailVATKey),
                LM.object(getClass("range")).getMapping(retailVATKey)));
        props.add(new ImportProperty(quantityPackItemField, getLCP("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, getLCP("wareItem").getMapping(itemKey),
                LM.object(getClass("ware")).getMapping(wareKey)));

//        props.add(new ImportProperty(wareIDField, getLCP("extSID").getMapping(wareKey))); // нельзя включать, потому то будут проблемы, если ссылается на товар, который не lgrmsec
        props.add(new ImportProperty(priceWareField, getLCP("dataWarePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, getLCP("dataRangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(getClass("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(rateWasteIDField, getLCP("rateWasteItem").getMapping(itemKey),
                LM.object(getClass("rateWaste")).getMapping(rateWasteKey)));

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, UOMIDField,
                nameUOMField, nameBrandField, brandIDField, nameCountryField, barcodeField, dateField,
                importerPriceField, percentWholesaleMarkItemField, isFixPriceItemField, isLoafCutItemField, isWeightItemField,
                compositionField, dataSuppliersRangeItemField, valueRetailVATItemField, quantityPackItemField, wareIDField,
                priceWareField, ndsWareField, rateWasteIDField), data);

        DataSession session = createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, supplierVATKey, retailVATKey, wareKey, rangeKey, rateWasteKey), props);
        service.synchronize(true, false);
        applySession(session);
        session.close();
    }

    private void importPrices(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            int count = 200000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importPricesFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField itemIDField = new ImportField(getLCP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLCP("extSID"));
                ImportField dateField = new ImportField(getLCP("date"));
                ImportField priceField = new ImportField(getLCP("dataRetailPriceItemDepartmentDate"));
                ImportField markupField = new ImportField(getLCP("dataMarkupItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("extSIDToObject").getMapping(departmentStoreIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceField, getLCP("dataRetailPriceItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));
                props.add(new ImportProperty(markupField, getLCP("dataMarkupItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));

                ImportTable table = new ImportTable(Arrays.asList(itemIDField, departmentStoreIDField, dateField, priceField, markupField/*priceWareField, ndsWareField*/), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, departmentStoreKey/*, wareKey, rangeKey*/), props);
                service.synchronize(true, false);
                applySession(session);
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

                ImportField shipmentField = new ImportField(getLCP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLCP("extSID"));
                ImportField supplierIDField = new ImportField(getLCP("extSID"));

                ImportField waybillShipmentField = new ImportField(getLCP("numberObject"));
                ImportField seriesWaybillShipmentField = new ImportField(getLCP("seriesObject"));
                ImportField dateShipmentField = new ImportField(getLCP("dateShipment"));

                ImportField itemIDField = new ImportField(getLCP("extSID"));
                ImportField shipmentDetailIDField = new ImportField(getLCP("extSID"));
                ImportField quantityShipmentDetailField = new ImportField(getLCP("quantityShipmentDetail"));
                ImportField supplierPriceShipmentDetail = new ImportField(getLCP("supplierPriceShipmentDetail"));
                ImportField importerPriceShipmentDetail = new ImportField(getLCP("importerPriceShipmentDetail"));
                ImportField retailPriceShipmentDetailField = new ImportField(getLCP("retailPriceShipmentDetail"));
                ImportField retailMarkupShipmentDetailField = new ImportField(getLCP("retailMarkupShipmentDetail"));
                ImportField dataSuppliersRangeField = new ImportField(getLCP("valueRate"));
                ImportField valueRetailVATField = new ImportField(getLCP("valueRate"));
                ImportField toShowWareField = new ImportField(getLCP("toShowWareShipment"));

                ImportKey<?> shipmentKey = new ImportKey((ConcreteCustomClass) getClass("shipment"),
                        getLCP("numberSeriesToShipment").getMapping(waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                        getLCP("extSIDToObject").getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("extSIDToObject").getMapping(departmentStoreIDField));

                ImportKey<?> shipmentDetailKey = new ImportKey((ConcreteCustomClass) getClass("shipmentDetail"),
                        getLCP("sidNumberSeriesToShipmentDetail").getMapping(shipmentDetailIDField, waybillShipmentField, seriesWaybillShipmentField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> supplierVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                        getLCP("valueCurrentVATDefaultValue").getMapping(dataSuppliersRangeField));

                ImportKey<?> retailVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                        getLCP("valueCurrentVATDefaultValue").getMapping(valueRetailVATField));


                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(waybillShipmentField, getLCP("numberObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(seriesWaybillShipmentField, getLCP("seriesObject").getMapping(shipmentKey)));
                props.add(new ImportProperty(dateShipmentField, getLCP("dateShipment").getMapping(shipmentKey)));
                props.add(new ImportProperty(departmentStoreIDField, getLCP("departmentStoreShipment").getMapping(shipmentKey),
                        LM.object(getClass("departmentStore")).getMapping(departmentStoreKey)));
                props.add(new ImportProperty(supplierIDField, getLCP("supplierShipment").getMapping(shipmentKey),
                        LM.object(getClass("supplier")).getMapping(supplierKey)));
                props.add(new ImportProperty(toShowWareField, getLCP("toShowWareShipment").getMapping(shipmentKey)));

                props.add(new ImportProperty(shipmentDetailIDField, getLCP("sidShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(quantityShipmentDetailField, getLCP("quantityShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(supplierPriceShipmentDetail, getLCP("supplierPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(importerPriceShipmentDetail, getLCP("importerPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailPriceShipmentDetailField, getLCP("retailPriceShipmentDetail").getMapping(shipmentDetailKey)));
                props.add(new ImportProperty(retailMarkupShipmentDetailField, getLCP("retailMarkupShipmentDetail").getMapping(shipmentDetailKey)));

                props.add(new ImportProperty(dataSuppliersRangeField, getLCP("supplierVATShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */supplierVATKey),
                        LM.object(getClass("range")).getMapping(supplierVATKey)));
                props.add(new ImportProperty(valueRetailVATField, getLCP("retailVATShipmentDetail").getMapping(shipmentDetailKey, /*dateShipmentField, */retailVATKey),
                        LM.object(getClass("range")).getMapping(retailVATKey)));

                props.add(new ImportProperty(itemIDField, getLCP("itemShipmentDetail").getMapping(shipmentDetailKey),
                        LM.object(getClass("item")).getMapping(itemKey)));

                props.add(new ImportProperty(shipmentField, getLCP("shipmentShipmentDetail").getMapping(shipmentDetailKey),
                        LM.object(getClass("shipment")).getMapping(shipmentKey)));

                ImportTable table = new ImportTable(Arrays.asList(waybillShipmentField, seriesWaybillShipmentField,
                        departmentStoreIDField, supplierIDField, dateShipmentField, itemIDField, shipmentDetailIDField,
                        quantityShipmentDetailField, supplierPriceShipmentDetail, importerPriceShipmentDetail, retailPriceShipmentDetailField,
                        retailMarkupShipmentDetailField, dataSuppliersRangeField, valueRetailVATField, toShowWareField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(shipmentKey, supplierKey, departmentStoreKey, shipmentDetailKey, itemKey, supplierVATKey, retailVATKey), props);
                service.synchronize(true, false);
                applySession(session);
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

                ImportField itemIDField = new ImportField(getLCP("extSID"));
                ImportField supplierIDField = new ImportField(getLCP("extSID"));
                ImportField departmentStoreIDField = new ImportField(getLCP("extSID"));
                ImportField isSupplierItemDepartmentField = new ImportField(getLCP("name"));
                ImportField priceSupplierItemDepartmentField = new ImportField(getLCP("dataPriceSupplierItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("extSIDToObject").getMapping(itemIDField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                        getLCP("extSIDToObject").getMapping(supplierIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("extSIDToObject").getMapping(departmentStoreIDField));

                ImportKey<?> logicalKey = new ImportKey((ConcreteCustomClass) getClass("yesNo"),
                        getLCP("classSIDToYesNo").getMapping(isSupplierItemDepartmentField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceSupplierItemDepartmentField, getLCP("dataPriceSupplierItemDepartmentDate").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate)));
                props.add(new ImportProperty(isSupplierItemDepartmentField, getLCP("dataIsSupplierItemDepartmentDate").getMapping(supplierKey, itemKey, departmentStoreKey, defaultDate),
                        LM.object(getClass("yesNo")).getMapping(logicalKey)));

                ImportTable table = new ImportTable(Arrays.asList(itemIDField, supplierIDField, departmentStoreIDField, isSupplierItemDepartmentField, priceSupplierItemDepartmentField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, itemKey, departmentStoreKey, logicalKey), props);
                service.synchronize(true, false);
                applySession(session);
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

            ImportField companyIDField = new ImportField(getLCP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLCP("name"));
            ImportField legalAddressField = new ImportField(getLCP("name"));
            ImportField unpField = new ImportField(getLCP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLCP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLCP("dataPhoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLCP("name"));
            ImportField nameOwnershipField = new ImportField(getLCP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLCP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLCP("numberAccount"));

            ImportField chainStoresIDField = new ImportField(getLCP("extSID"));
            ImportField nameChainStoresField = new ImportField(getLCP("name"));
            ImportField nameCountryField = new ImportField(getLCP("name"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) getClass("company"),
                    getLCP("extSIDToObject").getMapping(companyIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLCP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLCP("accountNumber").getMapping(accountField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLCP("extSIDToObject").getMapping(chainStoresIDField));

            ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) getClass("country"),
                    getLCP("countryName").getMapping(nameCountryField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(companyIDField, getLCP("extSID").getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("name").getMapping(companyKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("fullNameLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(legalAddressField, getLCP("dataAddressLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLCP("UNPLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(okpoField, getLCP("OKPOLegalEntity").getMapping(companyKey)));
            props.add(new ImportProperty(phoneField, getLCP("dataPhoneLegalEntityDate").getMapping(companyKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLCP("emailLegalEntity").getMapping(companyKey)));

            props.add(new ImportProperty(nameOwnershipField, getLCP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("ownershipLegalEntity").getMapping(companyKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLCP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(companyIDField, getLCP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("company")).getMapping(companyKey)));

            props.add(new ImportProperty(chainStoresIDField, getLCP("extSID").getMapping(chainStoresKey)));
            props.add(new ImportProperty(nameChainStoresField, getLCP("name").getMapping(chainStoresKey)));

            props.add(new ImportProperty(nameCountryField, getLCP("name").getMapping(countryKey)));
            props.add(new ImportProperty(nameCountryField, getLCP("countryLegalEntity").getMapping(companyKey),
                    LM.object(getClass("country")).getMapping(countryKey)));

            ImportTable table = new ImportTable(Arrays.asList(companyIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, chainStoresIDField, nameChainStoresField, nameCountryField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(companyKey, ownershipKey, accountKey, chainStoresKey, countryKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField supplierIDField = new ImportField(getLCP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLCP("name"));
            ImportField legalAddressField = new ImportField(getLCP("name"));
            ImportField unpField = new ImportField(getLCP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLCP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLCP("dataPhoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLCP("name"));
            ImportField nameOwnershipField = new ImportField(getLCP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLCP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLCP("numberAccount"));
            ImportField bankIDField = new ImportField(getLCP("extSID"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("supplier"),
                    getLCP("extSIDToObject").getMapping(supplierIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLCP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLCP("accountNumber").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLCP("extSIDToObject").getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(supplierIDField, getLCP("extSID").getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("name").getMapping(supplierKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("fullNameLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(legalAddressField, getLCP("dataAddressLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLCP("UNPLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(okpoField, getLCP("OKPOLegalEntity").getMapping(supplierKey)));
            props.add(new ImportProperty(phoneField, getLCP("dataPhoneLegalEntityDate").getMapping(supplierKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLCP("emailLegalEntity").getMapping(supplierKey)));

            props.add(new ImportProperty(nameOwnershipField, getLCP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("ownershipLegalEntity").getMapping(supplierKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLCP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(supplierIDField, getLCP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("supplier")).getMapping(supplierKey)));
            props.add(new ImportProperty(bankIDField, getLCP("bankAccount").getMapping(accountKey),
                    LM.object(getClass("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(supplierIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(supplierKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField customerIDField = new ImportField(getLCP("extSID"));
            ImportField nameLegalEntityField = new ImportField(getLCP("name"));
            ImportField legalAddressField = new ImportField(getLCP("name"));
            ImportField unpField = new ImportField(getLCP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLCP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLCP("dataPhoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLCP("name"));
            ImportField nameOwnershipField = new ImportField(getLCP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLCP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLCP("numberAccount"));
            ImportField bankIDField = new ImportField(getLCP("extSID"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) getClass("customer"),
                    getLCP("extSIDToObject").getMapping(customerIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLCP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLCP("accountNumber").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLCP("extSIDToObject").getMapping(bankIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(customerIDField, getLCP("extSID").getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("name").getMapping(customerKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("fullNameLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(legalAddressField, getLCP("dataAddressLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLCP("UNPLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(okpoField, getLCP("OKPOLegalEntity").getMapping(customerKey)));
            props.add(new ImportProperty(phoneField, getLCP("dataPhoneLegalEntityDate").getMapping(customerKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLCP("emailLegalEntity").getMapping(customerKey)));

            props.add(new ImportProperty(nameOwnershipField, getLCP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("ownershipLegalEntity").getMapping(customerKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLCP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(customerIDField, getLCP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("customer")).getMapping(customerKey)));
            props.add(new ImportProperty(bankIDField, getLCP("bankAccount").getMapping(accountKey),
                    LM.object(getClass("bank")).getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(customerIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, bankIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(customerKey, ownershipKey, accountKey, bankKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField storeIDField = new ImportField(getLCP("extSID"));
            ImportField nameStoreField = new ImportField(getLCP("name"));
            ImportField addressStoreField = new ImportField(getLCP("name"));
            ImportField companyIDField = new ImportField(getLCP("extSID"));
            ImportField chainStoresIDField = new ImportField(getLCP("extSID"));
            ImportField storeTypeField = new ImportField(getLCP("name"));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLCP("extSIDToObject").getMapping(storeIDField));

            ImportKey<?> companyKey = new ImportKey((ConcreteCustomClass) getClass("company"),
                    getLCP("extSIDToObject").getMapping(companyIDField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLCP("extSIDToObject").getMapping(chainStoresIDField));

            ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) getClass("storeType"),
                    getLCP("storeTypeNameChainStores").getMapping(storeTypeField, chainStoresIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, getLCP("extSID").getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, getLCP("name").getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, getLCP("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(companyIDField, getLCP("companyStore").getMapping(storeKey),
                    LM.object(getClass("company")).getMapping(companyKey)));

            props.add(new ImportProperty(storeTypeField, getLCP("name").getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, getLCP("storeTypeStore").getMapping(storeKey),
                    LM.object(getClass("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(chainStoresIDField, getLCP("chainStoresStoreType").getMapping(storeTypeKey),
                    LM.object(getClass("chainStores")).getMapping(chainStoresKey)));

            ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, companyIDField, storeTypeField, chainStoresIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, companyKey, chainStoresKey, storeTypeKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField departmentStoreIDField = new ImportField(getLCP("extSID"));
            ImportField nameDepartmentStoreField = new ImportField(getLCP("name"));
            ImportField storeIDField = new ImportField(getLCP("extSID"));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                    getLCP("extSIDToObject").getMapping(departmentStoreIDField));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLCP("extSIDToObject").getMapping(storeIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(departmentStoreIDField, getLCP("extSID").getMapping(departmentStoreKey)));
            props.add(new ImportProperty(nameDepartmentStoreField, getLCP("name").getMapping(departmentStoreKey)));
            props.add(new ImportProperty(storeIDField, getLCP("storeDepartmentStore").getMapping(departmentStoreKey),
                    LM.object(getClass("store")).getMapping(storeKey)));


            ImportTable table = new ImportTable(Arrays.asList(departmentStoreIDField, nameDepartmentStoreField, storeIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField bankIDField = new ImportField(getLCP("extSID"));
            ImportField nameBankField = new ImportField(getLCP("name"));
            ImportField addressBankField = new ImportField(getLCP("name"));
            ImportField departmentBankField = new ImportField(getLCP("departmentBank"));
            ImportField mfoBankField = new ImportField(getLCP("MFOBank"));
            ImportField cbuBankField = new ImportField(getLCP("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLCP("extSIDToObject").getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, getLCP("extSID").getMapping(bankKey)));
            props.add(new ImportProperty(nameBankField, getLCP("name").getMapping(bankKey)));
            props.add(new ImportProperty(addressBankField, getLCP("dataAddressBankDate").getMapping(bankKey, defaultDate)));
            props.add(new ImportProperty(departmentBankField, getLCP("departmentBank").getMapping(bankKey)));
            props.add(new ImportProperty(mfoBankField, getLCP("MFOBank").getMapping(bankKey)));
            props.add(new ImportProperty(cbuBankField, getLCP("CBUBank").getMapping(bankKey)));

            ImportTable table = new ImportTable(Arrays.asList(bankIDField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
            service.synchronize(true, false);
            applySession(session);
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

            ImportField rateWasteIDField = new ImportField(getLCP("extSID"));
            ImportField nameRateWasteField = new ImportField(getLCP("name"));
            ImportField percentRateWasteField = new ImportField(getLCP("percentRateWaste"));

            ImportKey<?> rateWasteKey = new ImportKey((ConcreteCustomClass) getClass("rateWaste"),
                    getLCP("extSIDToObject").getMapping(rateWasteIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(rateWasteIDField, getLCP("extSID").getMapping(rateWasteKey)));
            props.add(new ImportProperty(nameRateWasteField, getLCP("name").getMapping(rateWasteKey)));
            props.add(new ImportProperty(percentRateWasteField, getLCP("percentRateWaste").getMapping(rateWasteKey)));

            ImportTable table = new ImportTable(Arrays.asList(rateWasteIDField, nameRateWasteField, percentRateWasteField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(rateWasteKey), props);
            service.synchronize(true, false);
            applySession(session);
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
            String group4 = "ВСЕ";

            if ((!"".equals(group1)) && (!"".equals(group2)) && (!"".equals(group3))) {
                if (!parents) {
                    //sid - name
                    addIfNotContains(Arrays.asList((Object) group4, group4));
                    addIfNotContains(Arrays.asList((Object) (group3.substring(0, 3) + "/" + group4), group3));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3) + "/" + group4), group2));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4), group1));
                    addIfNotContains(Arrays.asList((Object) k_grtov, pol_naim));
                } else {
                    //sid - parentSID
                    addIfNotContains(Arrays.asList((Object) group4, null));
                    addIfNotContains(Arrays.asList((Object) (group3.substring(0, 3) + "/" + group4), group4));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3) + "/" + group4), group3.substring(0, 3) + "/" + group4));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4), group2 + "/" + group3.substring(0, 3) + "/" + group4));
                    addIfNotContains(Arrays.asList((Object) k_grtov, group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3) + "/" + group4));
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

            if (quantityPackItem == 0)
                quantityPackItem = 1.0;
            if (!quantities.containsKey(itemID)) {
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
                Double retailVAT = new Double(new String(itemsImportFile.getField("NDSR").getBytes(), "Cp1251").trim());
                Double quantityPackItem = quantities.containsKey(itemID) ? quantities.get(itemID) : null;
                Boolean isWare = "T".equals(new String(itemsImportFile.getField("LGRMSEC").getBytes(), "Cp1251").substring(0, 1));
                String wareID = new String(itemsImportFile.getField("K_GRMSEC").getBytes(), "Cp1251").trim();
                if (wareID.isEmpty())
                    wareID = null;
                String rateWasteID = "RW_" + new String(itemsImportFile.getField("K_VGRTOV").getBytes(), "Cp1251").trim();

                if (!"".equals(k_grtov) && (!inactiveItem || importInactive) && !isWare)
                    data.add(Arrays.asList((Object) itemID, k_grtov, pol_naim, "U_" + UOM, UOM, brand, "B_" + brand, country, barcode,
                            date, importerPrice, percentWholesaleMarkItem, isFixPriceItem ? isFixPriceItem : null, isLoafCutItem ? isLoafCutItem : null, isWeightItem ? isWeightItem : null,
                            "".equals(composition) ? null : composition, suppliersRange, retailVAT, quantityPackItem, wareID,
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
            Double retailVAT = new Double(new String(importFile.getField("NDSR").getBytes(), "Cp1251").trim());

            if (post_dok.length != 1)
                data.add(Arrays.asList((Object) number, series, departmentStoreID, supplierID, dateShipment, itemID,
                        shipmentDetailID, quantityShipmentDetail, supplierPriceShipmentDetail, importerPriceShipmentDetail, retailPriceShipmentDetail,
                        retailMarkupShipmentDetail, suppliersRange, retailVAT, true));
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
                String nameCountry = "РБ";

                if ("МГ".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                else if ("ПС".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, "BANK_" + k_bank));
                else if ("ЮР".equals(type))
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1], ownership[0], account, k_ana + "ТС", ownership[2], nameCountry));
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