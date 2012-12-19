package retail;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
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

import java.io.IOException;
import java.security.Timestamp;
import java.security.cert.CertPath;
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

                Object countryBelarus = getLCP("countrySID").read(context.getSession(), new DataObject("112", StringClass.get(3)));
                getLCP("defaultCountry").change(countryBelarus, context.getSession());
                applySession(context.getSession());

                Boolean importInactive = getLCP("importInactive").read(context) != null;
                if (getLCP("importGroupItems").read(context) != null) {
                    importItemGroups(path + "//_sprgrt.dbf", context);
                    importParentGroups(path + "//_sprgrt.dbf", context);
                }

                if (getLCP("importBanks").read(context) != null)
                    importBanks(path + "//_sprbank.dbf", context);

                if (getLCP("importLegalEntities").read(context) != null)
                    importLegalEntities(path + "//_sprana.dbf", importInactive, context);

                if (getLCP("importWarehouses").read(context) != null)
                    importWarehouses(path + "//_sprana.dbf", importInactive, context);

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

                if (getLCP("importUserInvoices").read(context) != null) {
                    importUserInvoices(path + "//_ostn.dbf", context);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importParentGroups(String path, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(getLCP("sidExternalizable"));
            ImportField parentGroupID = new ImportField(getLCP("sidExternalizable"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLCP("externalizableSID").getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLCP("externalizableSID").getMapping(parentGroupID));

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

            ImportField itemGroupID = new ImportField(getLCP("sidExternalizable"));
            ImportField itemGroupName = new ImportField(getLCP("name"));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                    getLCP("externalizableSID").getMapping(itemGroupID));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            props.add(new ImportProperty(itemGroupID, getLCP("sidExternalizable").getMapping(itemGroupKey)));
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

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportField wareIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField wareNameField = new ImportField(getLCP("name"));
            ImportField warePriceField = new ImportField(getLCP("warePrice"));

            ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                    getLCP("externalizableSID").getMapping(wareIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(wareIDField, getLCP("sidExternalizable").getMapping(wareKey)));
            props.add(new ImportProperty(wareNameField, getLCP("name").getMapping(wareKey)));
            props.add(new ImportProperty(warePriceField, getLCP("dataWarePriceDate").getMapping(wareKey, defaultDate)));

            ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField, warePriceField), data);

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

        ImportField itemIDField = new ImportField(getLCP("sidExternalizable"));
        ImportField itemGroupIDField = new ImportField(getLCP("sidExternalizable"));
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
        ImportField valueVATItemCountryDateField = new ImportField(getLCP("valueVATItemCountryDate"));
//        ImportField quantityPackItemField = new ImportField(getLCP("quantityPackItem"));
        ImportField wareIDField = new ImportField(getLCP("sidExternalizable"));
        ImportField priceWareField = new ImportField(getLCP("dataWarePriceDate"));
        ImportField ndsWareField = new ImportField(getLCP("valueRate"));
        ImportField writeOffRateIDField = new ImportField(getLCP("sidExternalizable"));

        DataObject defaultCountryObject = (DataObject) getLCP("defaultCountry").readClasses(context.getSession());

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                getLCP("externalizableSID").getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLCP("externalizableSID").getMapping(itemGroupIDField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) getClass("UOM"),
                getLCP("externalizableSID").getMapping(UOMIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) getClass("brand"),
                getLCP("externalizableSID").getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) getClass("country"),
                getLCP("countryName").getMapping(nameCountryField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) getClass("barcode"),
                getLCP("barcodeIdDate").getMapping(barcodeField, dateField));

        ImportKey<?> supplierVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(dataSuppliersRangeItemField));

        ImportKey<?> retailVATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(valueRetailVATItemField));

        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(valueVATItemCountryDateField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) getClass("ware"),
                getLCP("externalizableSID").getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) getClass("range"),
                getLCP("valueCurrentVATDefaultValue").getMapping(ndsWareField));

        ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) getClass("writeOffRate"),
                getLCP("externalizableSID").getMapping(writeOffRateIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, getLCP("itemGroupItem").getMapping(itemKey),
                LM.object(getClass("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, getLCP("sidExternalizable").getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, getLCP("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(UOMIDField, getLCP("sidExternalizable").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLCP("name").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, getLCP("shortName").getMapping(UOMKey)));
        props.add(new ImportProperty(UOMIDField, getLCP("UOMItem").getMapping(itemKey),
                LM.object(getClass("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, getLCP("name").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, getLCP("sidExternalizable").getMapping(brandKey)));
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
        props.add(new ImportProperty(valueVATItemCountryDateField, getLCP("dataVATItemCountryDate").getMapping(itemKey, defaultCountryObject, dateField),
                LM.object(getClass("range")).getMapping(VATKey)));
//        props.add(new ImportProperty(quantityPackItemField, getLCP("quantityPackItem").getMapping(itemKey)));

        props.add(new ImportProperty(wareIDField, getLCP("wareItem").getMapping(itemKey),
                LM.object(getClass("ware")).getMapping(wareKey)));

//        props.add(new ImportProperty(wareIDField, getLCP("sidExternalizable").getMapping(wareKey))); // нельзя включать, потому то будут проблемы, если ссылается на товар, который не lgrmsec
        props.add(new ImportProperty(priceWareField, getLCP("dataWarePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, getLCP("dataRangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(getClass("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(writeOffRateIDField, getLCP("writeOffRateCountryItem").getMapping(defaultCountryObject, itemKey),
                LM.object(getClass("writeOffRate")).getMapping(writeOffRateKey)));

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, UOMIDField,
                nameUOMField, nameBrandField, brandIDField, nameCountryField, barcodeField, dateField,
                importerPriceField, percentWholesaleMarkItemField, isFixPriceItemField, isLoafCutItemField, isWeightItemField,
                compositionField, dataSuppliersRangeItemField, valueRetailVATItemField, valueVATItemCountryDateField, wareIDField, //quantityPackItemField, wareIDField,
                priceWareField, ndsWareField, writeOffRateIDField), data);

        DataSession session = createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, supplierVATKey, retailVATKey, VATKey, wareKey, rangeKey, writeOffRateKey), props);
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

                ImportField itemIDField = new ImportField(getLCP("sidExternalizable"));
                ImportField departmentStoreIDField = new ImportField(getLCP("sidExternalizable"));
                ImportField dateField = new ImportField(getLCP("date"));
                ImportField priceField = new ImportField(getLCP("dataRetailPriceItemDepartmentDate"));
                ImportField markupField = new ImportField(getLCP("dataMarkupItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("externalizableSID").getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("externalizableSID").getMapping(departmentStoreIDField));

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

    private void importUserInvoices(String path, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            int count = 20000;
            for (int start = 0; true; start += count) {

                List<List<Object>> data = importUserInvoicesFromDBF(path, start, count);
                if (data.isEmpty())
                    return;

                ImportField userInvoiceField = new ImportField(getLCP("sidExternalizable"));
                ImportField customerDepartmentStoreIDField = new ImportField(getLCP("sidExternalizable"));
                ImportField supplierIDField = new ImportField(getLCP("sidExternalizable"));
                ImportField supplierWarehouseIDField = new ImportField(getLCP("sidExternalizable"));

                ImportField numberUserInvoiceField = new ImportField(getLCP("numberObject"));
                ImportField seriesUserInvoiceField = new ImportField(getLCP("seriesObject"));
                ImportField createPricingUserInvoiceField = new ImportField(getLCP("Purchase.createPricingUserInvoice"));
                ImportField createShipmentUserInvoiceField = new ImportField(getLCP("Purchase.createShipmentUserInvoice"));
                ImportField dateUserInvoiceField = new ImportField(getLCP("Purchase.dateUserInvoice"));

                ImportField itemField = new ImportField(getLCP("sidExternalizable"));
                ImportField sidUserInvoiceDetailField = new ImportField(getLCP("Purchase.sidUserInvoiceDetail"));
                ImportField quantityUserInvoiceDetailField = new ImportField(getLCP("Purchase.quantityUserInvoiceDetail"));
                ImportField priceUserInvoiceDetail = new ImportField(getLCP("Purchase.priceUserInvoiceDetail"));
                ImportField retailPriceUserInvoiceDetailField = new ImportField(getLCP("Purchase.retailPriceUserInvoiceDetail"));
                ImportField retailMarkupUserInvoiceDetailField = new ImportField(getLCP("Purchase.retailMarkupUserInvoiceDetail"));

                ImportKey<?> userInvoiceKey = new ImportKey((ConcreteCustomClass) getClass("Purchase.userInvoice"),
                        getLCP("Purchase.numberSeriesToUserInvoice").getMapping(numberUserInvoiceField, seriesUserInvoiceField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) getClass("legalEntity"),
                        getLCP("externalizableSID").getMapping(supplierIDField));

                ImportKey<?> customerDepartmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("externalizableSID").getMapping(customerDepartmentStoreIDField));

                ImportKey<?> supplierWarehouseKey = new ImportKey((ConcreteCustomClass) getClass("warehouse"),
                        getLCP("externalizableSID").getMapping(supplierWarehouseIDField));

                ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) getClass("Purchase.userInvoiceDetail"),
                        getLCP("Purchase.userInvoiceDetailSID").getMapping(sidUserInvoiceDetailField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("externalizableSID").getMapping(itemField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(numberUserInvoiceField, getLCP("numberObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(seriesUserInvoiceField, getLCP("seriesObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(createPricingUserInvoiceField, getLCP("Purchase.createPricingUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(createShipmentUserInvoiceField, getLCP("Purchase.createShipmentUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(dateUserInvoiceField, getLCP("Purchase.dateUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(customerDepartmentStoreIDField, getLCP("Purchase.customerStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(getClass("departmentStore")).getMapping(customerDepartmentStoreKey)));
                props.add(new ImportProperty(supplierWarehouseIDField, getLCP("Purchase.supplierStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(getClass("warehouse")).getMapping(supplierWarehouseKey)));
                props.add(new ImportProperty(supplierIDField, getLCP("Purchase.supplierUserInvoice").getMapping(userInvoiceKey),
                        LM.object(getClass("legalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(customerDepartmentStoreIDField, getLCP("Purchase.customerUserInvoice").getMapping(userInvoiceKey),
                        getLCP("legalEntityStock").getMapping(customerDepartmentStoreKey)));

                props.add(new ImportProperty(quantityUserInvoiceDetailField, getLCP("Purchase.quantityUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceUserInvoiceDetail, getLCP("Purchase.priceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(retailPriceUserInvoiceDetailField, getLCP("Purchase.retailPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(retailMarkupUserInvoiceDetailField, getLCP("Purchase.retailMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(itemField, getLCP("Purchase.skuInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(getClass("item")).getMapping(itemKey)));

                props.add(new ImportProperty(userInvoiceField, getLCP("Purchase.userInvoiceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(getClass("Purchase.userInvoice")).getMapping(userInvoiceKey)));

                ImportTable table = new ImportTable(Arrays.asList(numberUserInvoiceField, seriesUserInvoiceField,
                        createPricingUserInvoiceField, createShipmentUserInvoiceField, sidUserInvoiceDetailField, dateUserInvoiceField, itemField,
                        quantityUserInvoiceDetailField, supplierIDField, customerDepartmentStoreIDField,
                        supplierWarehouseIDField, priceUserInvoiceDetail, retailPriceUserInvoiceDetailField,
                        retailMarkupUserInvoiceDetailField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userInvoiceKey, userInvoiceDetailKey, itemKey, supplierKey, customerDepartmentStoreKey), props);
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
                    break;

                DataSession currencySession = LM.getBL().createSession();

                DataObject dataPriceListTypeObject = currencySession.addObject((ConcreteCustomClass) getClass("dataPriceListType"));
                Object defaultCurrency = getLCP("currencyShortName").read(currencySession, new DataObject("BLR", StringClass.get(3)));
                getLCP("name").change("Поставщика", currencySession, dataPriceListTypeObject);
                getLCP("currencyDataPriceListType").change(defaultCurrency, currencySession, dataPriceListTypeObject);
                currencySession.apply(LM.getBL());

                ImportField itemField = new ImportField(getLCP("sidExternalizable"));
                ImportField legalEntityField = new ImportField(getLCP("sidExternalizable"));
                ImportField userPriceListField = new ImportField(getLCP("sidExternalizable"));
                ImportField departmentStoreField = new ImportField(getLCP("sidExternalizable"));
                ImportField currencyField = new ImportField(getLCP("shortNameCurrency"));
                ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(getLCP("pricePriceListDetailDataPriceListType"));
                ImportField inPriceListPriceListTypeField = new ImportField(getLCP("inPriceListPriceListType"));

                ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) getClass("userPriceListDetail"),
                        getLCP("userPriceListDetailSkuUserPriceList").getMapping(itemField, userPriceListField));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) getClass("legalEntity"),
                        getLCP("externalizableSID").getMapping(legalEntityField));

                ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) getClass("userPriceList"),
                        getLCP("externalizableSID").getMapping(userPriceListField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) getClass("item"),
                        getLCP("externalizableSID").getMapping(itemField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) getClass("currency"),
                        getLCP("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(userPriceListField, getLCP("sidExternalizable").getMapping(userPriceListKey)));
                props.add(new ImportProperty(legalEntityField, getLCP("companyUserPriceList").getMapping(userPriceListKey),
                        LM.object(getClass("legalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(itemField, getLCP("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(getClass("item")).getMapping(itemKey)));
                props.add(new ImportProperty(legalEntityField, getLCP("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(getClass("userPriceList")).getMapping(userPriceListKey)));
                props.add(new ImportProperty(currencyField, getLCP("currencyUserPriceList").getMapping(userPriceListKey),
                        LM.object(getClass("currency")).getMapping(currencyKey)));
                props.add(new ImportProperty(inPriceListPriceListTypeField, getLCP("inUserPriceListPriceListType").getMapping(userPriceListKey, new DataObject(dataPriceListTypeObject.object, (ConcreteClass) getClass("dataPriceListType")))));
                props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, getLCP("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, new DataObject(dataPriceListTypeObject.object, (ConcreteClass) getClass("dataPriceListType"))/*, itemKey, departmentStoreKey, defaultDate*/)));
                ImportTable table = new ImportTable(Arrays.asList(itemField, legalEntityField, userPriceListField, departmentStoreField, currencyField, pricePriceListDetailDataPriceListTypeField, inPriceListPriceListTypeField), data);

                DataSession session = createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey, userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                service.synchronize(true, false);
                applySession(session);
                session.close();

                System.out.println("done assortment " + data.size());

                data = importStockSuppliersFromDBF(context, path, start, count);

                ImportField inPriceList2Field = new ImportField(getLCP("inPriceList"));
                ImportField inPriceListStock2Field = new ImportField(getLCP("inPriceListStock"));
                ImportField userPriceList2Field = new ImportField(getLCP("sidExternalizable"));
                ImportField departmentStore2Field = new ImportField(getLCP("sidExternalizable"));

                ImportKey<?> userPriceList2Key = new ImportKey((ConcreteCustomClass) getClass("userPriceList"),
                        getLCP("externalizableSID").getMapping(userPriceList2Field));
                ImportKey<?> departmentStore2Key = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                        getLCP("externalizableSID").getMapping(departmentStore2Field));

                props = new ArrayList<ImportProperty<?>>();
                props.add(new ImportProperty(userPriceList2Field, getLCP("sidExternalizable").getMapping(userPriceList2Key)));
                props.add(new ImportProperty(inPriceList2Field, getLCP("inPriceList").getMapping(userPriceList2Key)));
                props.add(new ImportProperty(inPriceListStock2Field, getLCP("inPriceListStock").getMapping(userPriceList2Key, departmentStore2Key)));

                table = new ImportTable(Arrays.asList(userPriceList2Field, departmentStore2Field, inPriceList2Field, inPriceListStock2Field), data);

                session = createSession();
                service = new IntegrationService(session, table, Arrays.asList(userPriceList2Key, departmentStore2Key), props);
                service.synchronize(true, false);
                applySession(session);
                session.close();
            }

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importLegalEntities(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, false);

            ImportField legalEntityIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameLegalEntityField = new ImportField(getLCP("name"));
            ImportField legalAddressField = new ImportField(getLCP("name"));
            ImportField unpField = new ImportField(getLCP("UNPLegalEntity"));
            ImportField okpoField = new ImportField(getLCP("OKPOLegalEntity"));
            ImportField phoneField = new ImportField(getLCP("dataPhoneLegalEntityDate"));
            ImportField emailField = new ImportField(getLCP("name"));
            ImportField nameOwnershipField = new ImportField(getLCP("name"));
            ImportField shortNameOwnershipField = new ImportField(getLCP("shortNameOwnership"));
            ImportField accountField = new ImportField(getLCP("numberAccount"));

            ImportField chainStoresIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameChainStoresField = new ImportField(getLCP("name"));
            ImportField bankIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameCountryField = new ImportField(getLCP("name"));

            ImportField isSupplierLegalEntityField = new ImportField(getLCP("isSupplierLegalEntity"));
            ImportField isCompanyLegalEntityField = new ImportField(getLCP("isCompanyLegalEntity"));
            ImportField isCustomerLegalEntityField = new ImportField(getLCP("isCustomerLegalEntity"));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) getClass("legalEntity"),
                    getLCP("externalizableSID").getMapping(legalEntityIDField));

            ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) getClass("ownership"),
                    getLCP("shortNameToOwnership").getMapping(shortNameOwnershipField));

            ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) getClass("account"),
                    getLCP("accountNumber").getMapping(accountField));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLCP("externalizableSID").getMapping(bankIDField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLCP("externalizableSID").getMapping(chainStoresIDField));

            ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) getClass("country"),
                    getLCP("countryName").getMapping(nameCountryField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(legalEntityIDField, getLCP("sidExternalizable").getMapping(legalEntityKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("name").getMapping(legalEntityKey)));
            props.add(new ImportProperty(nameLegalEntityField, getLCP("fullNameLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(legalAddressField, getLCP("dataAddressLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
            props.add(new ImportProperty(unpField, getLCP("UNPLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(okpoField, getLCP("OKPOLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(phoneField, getLCP("dataPhoneLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
            props.add(new ImportProperty(emailField, getLCP("emailLegalEntity").getMapping(legalEntityKey)));

            props.add(new ImportProperty(isSupplierLegalEntityField, getLCP("isSupplierLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(isCompanyLegalEntityField, getLCP("isCompanyLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(isCustomerLegalEntityField, getLCP("isCustomerLegalEntity").getMapping(legalEntityKey)));

            props.add(new ImportProperty(nameOwnershipField, getLCP("name").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("shortNameOwnership").getMapping(ownershipKey)));
            props.add(new ImportProperty(shortNameOwnershipField, getLCP("ownershipLegalEntity").getMapping(legalEntityKey),
                    LM.object(getClass("ownership")).getMapping(ownershipKey)));

            props.add(new ImportProperty(accountField, getLCP("numberAccount").getMapping(accountKey)));
            props.add(new ImportProperty(legalEntityIDField, getLCP("legalEntityAccount").getMapping(accountKey),
                    LM.object(getClass("legalEntity")).getMapping(legalEntityKey)));

            props.add(new ImportProperty(chainStoresIDField, getLCP("sidExternalizable").getMapping(chainStoresKey)));
            props.add(new ImportProperty(nameChainStoresField, getLCP("name").getMapping(chainStoresKey)));

            props.add(new ImportProperty(bankIDField, getLCP("bankAccount").getMapping(accountKey),
                    LM.object(getClass("bank")).getMapping(bankKey)));

            props.add(new ImportProperty(nameCountryField, getLCP("name").getMapping(countryKey)));
            props.add(new ImportProperty(nameCountryField, getLCP("countryLegalEntity").getMapping(legalEntityKey),
                    LM.object(getClass("country")).getMapping(countryKey)));

            ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, nameLegalEntityField, legalAddressField,
                    unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                    accountField, chainStoresIDField, nameChainStoresField, bankIDField, nameCountryField,
                    isSupplierLegalEntityField, isCompanyLegalEntityField, isCustomerLegalEntityField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, ownershipKey, accountKey, bankKey, chainStoresKey, countryKey), props);
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

    private void importWarehouses(String path, Boolean importInactive, ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            List<List<Object>> data = importWarehousesFromDBF(path, importInactive);

            ImportField legalEntityIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField warehouseIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameWarehouseField = new ImportField(getLCP("name"));
            ImportField addressWarehouseField = new ImportField(getLCP("name"));

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) getClass("legalEntity"),
                    getLCP("externalizableSID").getMapping(legalEntityIDField));

            ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) getClass("warehouse"),
                    getLCP("externalizableSID").getMapping(warehouseIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(warehouseIDField, getLCP("sidExternalizable").getMapping(warehouseKey)));
            props.add(new ImportProperty(nameWarehouseField, getLCP("name").getMapping(warehouseKey)));
            props.add(new ImportProperty(addressWarehouseField, getLCP("addressWarehouse").getMapping(warehouseKey)));
            props.add(new ImportProperty(legalEntityIDField, getLCP("legalEntityWarehouse").getMapping(warehouseKey),
                    LM.object(getClass("legalEntity")).getMapping(legalEntityKey)));

            ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, warehouseIDField,
                    nameWarehouseField, addressWarehouseField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, warehouseKey), props);
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
            List<List<Object>> data = importLegalEntitiesFromDBF(path, importInactive, true);

            ImportField storeIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameStoreField = new ImportField(getLCP("name"));
            ImportField addressStoreField = new ImportField(getLCP("name"));
            ImportField legalEntityIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField chainStoresIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField storeTypeField = new ImportField(getLCP("name"));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLCP("externalizableSID").getMapping(storeIDField));

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) getClass("legalEntity"),
                    getLCP("externalizableSID").getMapping(legalEntityIDField));

            ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) getClass("chainStores"),
                    getLCP("externalizableSID").getMapping(chainStoresIDField));

            ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) getClass("storeType"),
                    getLCP("storeTypeNameChainStores").getMapping(storeTypeField, chainStoresIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(storeIDField, getLCP("sidExternalizable").getMapping(storeKey)));
            props.add(new ImportProperty(nameStoreField, getLCP("name").getMapping(storeKey)));
            props.add(new ImportProperty(addressStoreField, getLCP("addressStore").getMapping(storeKey)));
            props.add(new ImportProperty(legalEntityIDField, getLCP("legalEntityStore").getMapping(storeKey),
                    LM.object(getClass("legalEntity")).getMapping(legalEntityKey)));

            props.add(new ImportProperty(storeTypeField, getLCP("name").getMapping(storeTypeKey)));
            props.add(new ImportProperty(storeTypeField, getLCP("storeTypeStore").getMapping(storeKey),
                    LM.object(getClass("storeType")).getMapping(storeTypeKey)));
            props.add(new ImportProperty(chainStoresIDField, getLCP("chainStoresStoreType").getMapping(storeTypeKey),
                    LM.object(getClass("chainStores")).getMapping(chainStoresKey)));

            ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, legalEntityIDField, storeTypeField, chainStoresIDField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, legalEntityKey, chainStoresKey, storeTypeKey), props);
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

            ImportField departmentStoreIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameDepartmentStoreField = new ImportField(getLCP("name"));
            ImportField storeIDField = new ImportField(getLCP("sidExternalizable"));

            ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) getClass("departmentStore"),
                    getLCP("externalizableSID").getMapping(departmentStoreIDField));

            ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) getClass("store"),
                    getLCP("externalizableSID").getMapping(storeIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(departmentStoreIDField, getLCP("sidExternalizable").getMapping(departmentStoreKey)));
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

            ImportField bankIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameBankField = new ImportField(getLCP("name"));
            ImportField addressBankField = new ImportField(getLCP("name"));
            ImportField departmentBankField = new ImportField(getLCP("departmentBank"));
            ImportField mfoBankField = new ImportField(getLCP("MFOBank"));
            ImportField cbuBankField = new ImportField(getLCP("CBUBank"));

            ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) getClass("bank"),
                    getLCP("externalizableSID").getMapping(bankIDField));

            DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(bankIDField, getLCP("sidExternalizable").getMapping(bankKey)));
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

            ImportField writeOffRateIDField = new ImportField(getLCP("sidExternalizable"));
            ImportField nameWriteOffRateField = new ImportField(getLCP("name"));
            ImportField percentWriteOffRateField = new ImportField(getLCP("percentWriteOffRate"));
            ImportField countryWriteOffRateField = new ImportField(getLCP("name"));

            ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) getClass("writeOffRate"),
                    getLCP("externalizableSID").getMapping(writeOffRateIDField));

            ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) getClass("country"),
                    getLCP("countryName").getMapping(countryWriteOffRateField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(writeOffRateIDField, getLCP("sidExternalizable").getMapping(writeOffRateKey)));
            props.add(new ImportProperty(nameWriteOffRateField, getLCP("name").getMapping(writeOffRateKey)));
            props.add(new ImportProperty(percentWriteOffRateField, getLCP("percentWriteOffRate").getMapping(writeOffRateKey)));
            props.add(new ImportProperty(countryWriteOffRateField, getLCP("countryWriteOffRate").getMapping(writeOffRateKey),
                    LM.object(getClass("country")).getMapping(countryKey)));
            ImportTable table = new ImportTable(Arrays.asList(writeOffRateIDField, nameWriteOffRateField, percentWriteOffRateField, countryWriteOffRateField), data);

            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(writeOffRateKey, countryKey), props);
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
            Double price = new Double(new String(importFile.getField("N_PRCEN").getBytes(), "Cp1251").trim());

            if (!"".equals(wareID) && isWare)
                data.add(Arrays.asList((Object) wareID, pol_naim, price));
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
//            Double quantityPackItem = new Double(new String(quantityImportFile.getField("PACKSIZE").getBytes(), "Cp1251").trim());

//            if (quantityPackItem == 0)
//                quantityPackItem = 1.0;
//            if (!quantities.containsKey(itemID)) {
//                quantities.put(itemID, quantityPackItem);
//            }
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
                if ("РБ".equals(country) || "Беларусь".equals(country))
                    country = "БЕЛАРУСЬ";
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
                            "".equals(composition) ? null : composition, suppliersRange, retailVAT, retailVAT, quantityPackItem, wareID,
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

    private List<List<Object>> importUserInvoicesFromDBF(String path, int start, int count) throws
            IOException, xBaseJException, ParseException, ScriptingErrorLog.SemanticErrorException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < Math.min(totalRecordCount, start + count); i++) {
            importFile.read();
            if (i < start) continue;

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

            if ((post_dok.length != 1) && (supplierID.startsWith("ПС")))
                data.add(Arrays.asList((Object) number, series, true, true, userInvoiceDetailSID, dateShipment, itemID,
                        quantityShipmentDetail, supplierID, warehouseID, supplierWarehouse, priceShipmentDetail,
                        retailPriceShipmentDetail, retailMarkupShipmentDetail));
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
            String currency = "BLR";
            Double price = new Double(new String(importFile.getField("N_CENU").getBytes(), "Cp1251").trim());

            if (departmentStore.length() >= 2 && supplier.startsWith("ПС")) {
                data.add(Arrays.asList((Object) item, supplier, supplier + "ПР", departmentStore, currency, price, true));
            }
        }
        return data;
    }

    private List<List<Object>> importStockSuppliersFromDBF(ExecutionContext context, String path, int start, int count) throws
            IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        DBF importFile = new DBF(path);
        int totalRecordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();
        Set<String> stores = new HashSet<String>();
        for (int i = 0; i < Math.min(totalRecordCount, start + count); i++) {
            importFile.read();
            if (i < start) continue;

            String supplier = new String(importFile.getField("K_ANA").getBytes(), "Cp1251").trim();
            String store = new String(importFile.getField("K_SKL").getBytes(), "Cp1251").trim();

            if (supplier.startsWith("ПС") && (!stores.contains(store))) {

                Object storeObject = getLCP("externalizableSID").readClasses(context.getSession(), new DataObject(store, StringClass.get(110)));
                if (!(storeObject instanceof NullValue)) {
                    LCP isDepartmentStore = LM.is(getClass("departmentStore"));
                    Map<Object, KeyExpr> keys = isDepartmentStore.getMapKeys();
                    KeyExpr key = BaseUtils.singleValue(keys);
                    Query<Object, Object> query = new Query<Object, Object>(keys);
                    query.properties.put("sidExternalizable", getLCP("sidExternalizable").getExpr(context.getModifier(), key));
                    query.and(isDepartmentStore.getExpr(key).getWhere());
                    query.and(getLCP("storeDepartmentStore").getExpr(context.getModifier(), key).compare(((DataObject)storeObject).getExpr(), Compare.EQUALS));
                    OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(context.getSession().sql);

                    for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : result.entrySet()) {
                        List<Object> row = new ArrayList<Object>();
                        row.addAll(Arrays.asList(supplier + "ПР", entry.getValue().get("sidExternalizable"), null, true));
                        if (!data.contains(row))
                            data.add(row);
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


    private List<List<Object>> importLegalEntitiesFromDBF(String path, Boolean importInactive, Boolean isStore) throws
            IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

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
                        data.add(Arrays.asList((Object) k_ana, ownership[2], address, companyStore, "Магазин", companyStore + "ТС"));
                } else if (isCompany || isSupplier || isCustomer)
                    data.add(Arrays.asList((Object) k_ana, ownership[2], address, unp, okpo, phone, email, ownership[1],
                            ownership[0], account, isCompany ? (k_ana + "ТС") : null, isCompany ? ownership[2] : null,
                            "BANK_" + k_bank, nameCountry, isSupplier ? true : null, isCompany ? true : null,
                            isCustomer ? true : null));
            }
        }
        return data;
    }

    private List<List<Object>> importWarehousesFromDBF(String path, Boolean importInactive) throws
            IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

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
                    data.add(Arrays.asList((Object) k_ana, k_ana + "WH", "Склад " + name, address));
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
            String country = "БЕЛАРУСЬ";
            data.add(Arrays.asList((Object) ("RW_" + rateWasteID), name, coef, country));
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