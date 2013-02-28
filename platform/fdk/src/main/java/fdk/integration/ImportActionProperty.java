package fdk.integration;

import org.xBaseJ.xBaseJException;
import platform.server.classes.*;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;

public class ImportActionProperty {
    private ScriptingLogicsModule LM;
    private ImportData importData;
    private ExecutionContext<ClassPropertyInterface> context;

    public ImportActionProperty(ScriptingLogicsModule LM, ImportData importData, ExecutionContext<ClassPropertyInterface> context) {
        this.LM = LM;
        this.importData = importData;
        this.context = context;
    }

    public void makeImport() throws SQLException {
        try {

            Object countryBelarus = LM.findLCPByCompoundName("countrySID").read(context.getSession(), new DataObject("112", StringClass.get(3)));
            LM.findLCPByCompoundName("defaultCountry").change(countryBelarus, context.getSession());
            context.getSession().apply(LM.getBL());

            importItemGroups(importData.getItemGroupsList());

            importParentGroups(importData.getParentGroupsList());

            importBanks(importData.getBanksList());

            importLegalEntities(importData.getLegalEntitiesList());

            importEmployees(importData.getEmployeesList());

            importWarehouseGroups(importData.getWarehouseGroupsList());

            importWarehouses(importData.getWarehousesList());

            importStores(importData.getStoresList());

            importDepartmentStores(importData.getDepartmentStoresList());

            importContracts(importData.getContractsList());

            importRateWastes(importData.getRateWastesList());

            importWares(importData.getWaresList());

            importItems(importData.getItemsList(), importData.getNumberOfItemsAtATime());

            //importPrices(importData.pricesList);

            importAssortment(importData.getAssortmentsList(), importData.getStockSuppliersList());

            importUserInvoices(importData.getUserInvoicesList(), importData.getImportUserInvoicesPosted(), importData.getNumberOfUserInvoicesAtATime());

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importParentGroups(List<ItemGroup> parentGroupsList) throws ScriptingErrorLog.SemanticErrorException {
        try {
            if (parentGroupsList != null) {
                ImportField itemGroupID = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField parentGroupID = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("itemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupID));
                ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("itemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(parentGroupID));

                List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
                propsParent.add(new ImportProperty(parentGroupID, LM.findLCPByCompoundName("parentItemGroup").getMapping(itemGroupKey),
                        LM.object(LM.findClassByCompoundName("itemGroup")).getMapping(parentGroupKey)));
                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup p : parentGroupsList) {
                    data.add(Arrays.asList((Object) (p.sid == null ? null : ("IG" + p.sid)), p.parent == null ? null : ("IG" + p.parent)));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importItemGroups(List<ItemGroup> itemGroupsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (itemGroupsList != null) {
                ImportField itemGroupID = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField itemGroupName = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("itemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupID));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
                props.add(new ImportProperty(itemGroupID, LM.findLCPByCompoundName("sidExternalizable").getMapping(itemGroupKey)));
                props.add(new ImportProperty(itemGroupName, LM.findLCPByCompoundName("name").getMapping(itemGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup i : itemGroupsList) {
                    data.add(Arrays.asList((Object) (i.sid == null ? null : ("IG" + i.sid)), i.name));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWares(List<Ware> waresList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (waresList != null) {
                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportField wareIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField wareNameField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField warePriceField = new ImportField(LM.findLCPByCompoundName("warePrice"));

                ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ware"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(wareIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(wareKey)));
                props.add(new ImportProperty(wareNameField, LM.findLCPByCompoundName("name").getMapping(wareKey)));
                props.add(new ImportProperty(warePriceField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, defaultDate)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Ware w : waresList) {
                    data.add(Arrays.asList((Object) (w.wareID == null ? null : ("W" + w.wareID)), w.name, w.price));
                }
                ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField, warePriceField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importItems(List<Item> itemsList, Integer numberOfItemsAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            Integer numAtATime = (numberOfItemsAtATime == null || numberOfItemsAtATime <= 0) ? 5000 : numberOfItemsAtATime;
            if (itemsList != null) {
                int amountOfImportIterations = (int) Math.ceil((double) itemsList.size() / numAtATime);
                Integer rest = itemsList.size();
                for (int i = 0; i < amountOfImportIterations; i++) {
                    importPackOfItems(itemsList, i * numAtATime, rest > numAtATime ? numAtATime : rest);
                    rest -= numAtATime;
                    System.gc();
                }
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importPackOfItems(List<Item> itemsList, Integer start, Integer numberOfItems) throws SQLException, IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException {
        List<Item> dataItems = itemsList.subList(start, start + numberOfItems);
        if (dataItems == null) return;

        ImportField itemIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField itemGroupIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField itemCaptionField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField UOMIDField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField nameUOMField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField shortNameUOMField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField brandIDField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField nameBrandField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField barcodeField = new ImportField(LM.findLCPByCompoundName("idBarcode"));
        ImportField dateField = new ImportField(DateClass.instance);
        ImportField isWeightItemField = new ImportField(LM.findLCPByCompoundName("isWeightItem"));
        ImportField netWeightItemField = new ImportField(LM.findLCPByCompoundName("netWeightItem"));
        ImportField grossWeightItemField = new ImportField(LM.findLCPByCompoundName("grossWeightItem"));
        ImportField compositionField = new ImportField(LM.findLCPByCompoundName("compositionScalesItem"));
        ImportField valueVATItemCountryDateField = new ImportField(LM.findLCPByCompoundName("valueVATItemCountryDate"));
        ImportField wareIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField priceWareField = new ImportField(LM.findLCPByCompoundName("dataWarePriceDate"));
        ImportField ndsWareField = new ImportField(LM.findLCPByCompoundName("valueRate"));
        ImportField writeOffRateIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField retailCalcPriceListTypeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField retailCalcPriceListTypeNameField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField retailMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));
        ImportField baseCalcPriceListTypeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField baseCalcPriceListTypeNameField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField baseMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));

        DataObject defaultCountryObject = (DataObject) LM.findLCPByCompoundName("defaultCountry").readClasses(context.getSession());

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("item"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("itemGroup"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupIDField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(UOMIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("brand"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("country"),
                LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("barcode"),
                LM.findLCPByCompoundName("barcodeIdDate").getMapping(barcodeField, dateField));

        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(valueVATItemCountryDateField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ware"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(ndsWareField));

        ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("writeOffRate"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(writeOffRateIDField));

        ImportKey<?> baseCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("calcPriceListType"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(baseCalcPriceListTypeIDField));

        ImportKey<?> retailCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("calcPriceListType"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(retailCalcPriceListTypeIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, LM.findLCPByCompoundName("itemGroupItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("itemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, LM.findLCPByCompoundName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(UOMIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, LM.findLCPByCompoundName("name").getMapping(UOMKey)));
        props.add(new ImportProperty(shortNameUOMField, LM.findLCPByCompoundName("shortName").getMapping(UOMKey)));
        props.add(new ImportProperty(UOMIDField, LM.findLCPByCompoundName("UOMItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, LM.findLCPByCompoundName("name").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, LM.findLCPByCompoundName("brandItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("brand")).getMapping(brandKey)));

        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("name").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("country")).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeField, LM.findLCPByCompoundName("idBarcode").getMapping(barcodeKey)/*, BL.LM.toEAN13.getMapping(barcodeField)*/));
        props.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dataDateBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, LM.findLCPByCompoundName("skuBarcode").getMapping(barcodeKey),
                LM.object(LM.findClassByCompoundName("item")).getMapping(itemKey)));

        props.add(new ImportProperty(isWeightItemField, LM.findLCPByCompoundName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(netWeightItemField, LM.findLCPByCompoundName("netWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(grossWeightItemField, LM.findLCPByCompoundName("grossWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, LM.findLCPByCompoundName("compositionScalesItem").getMapping(itemKey)));
        props.add(new ImportProperty(valueVATItemCountryDateField, LM.findLCPByCompoundName("dataVATItemCountryDate").getMapping(itemKey, defaultCountryObject, dateField),
                LM.object(LM.findClassByCompoundName("range")).getMapping(VATKey)));

        props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(wareKey)));
        props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("wareItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("ware")).getMapping(wareKey)));

        props.add(new ImportProperty(priceWareField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, LM.findLCPByCompoundName("dataRangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(LM.findClassByCompoundName("range")).getMapping(rangeKey)));

        props.add(new ImportProperty(writeOffRateIDField, LM.findLCPByCompoundName("writeOffRateCountryItem").getMapping(defaultCountryObject, itemKey),
                LM.object(LM.findClassByCompoundName("writeOffRate")).getMapping(writeOffRateKey)));

        props.add(new ImportProperty(retailCalcPriceListTypeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(retailCalcPriceListTypeNameField, LM.findLCPByCompoundName("name").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(retailMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(retailCalcPriceListTypeKey, itemKey)));
        props.add(new ImportProperty(baseCalcPriceListTypeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(baseCalcPriceListTypeNameField, LM.findLCPByCompoundName("name").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(baseMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(baseCalcPriceListTypeKey, itemKey)));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (Item i : dataItems) {
            data.add(Arrays.asList((Object) (i.itemID == null ? null : ("I" + i.itemID)),
                    i.k_grtov == null ? null : ("IG" + i.k_grtov), i.name, i.uomName, i.uomShortName,
                    i.uomID == null ? null : ("UOM" + i.uomID), i.brandName, i.brandID, i.country, i.barcode, i.date,
                    i.isWeightItem, i.netWeightItem, i.grossWeightItem, i.composition, i.retailVAT, i.wareID == null ? null : ("W" + i.wareID),
                    i.priceWare, i.ndsWareField, i.writeOffRateID == null ? null : ("RW" + i.writeOffRateID),
                    "cplt_retail", "Розничная надбавка", i.retailMarkup, "cplt_base", "Надбавка базы", i.baseMarkup));
        }

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, nameUOMField,
                shortNameUOMField, UOMIDField, nameBrandField, brandIDField, nameCountryField, barcodeField, dateField,
                isWeightItemField, netWeightItemField, grossWeightItemField, compositionField, valueVATItemCountryDateField,
                wareIDField, priceWareField, ndsWareField, writeOffRateIDField, retailCalcPriceListTypeIDField,
                retailCalcPriceListTypeNameField, retailMarkupCalcPriceListTypeField, baseCalcPriceListTypeIDField,
                baseCalcPriceListTypeNameField, baseMarkupCalcPriceListTypeField), data);

        DataSession session = LM.getBL().createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, VATKey, wareKey, rangeKey, writeOffRateKey, retailCalcPriceListTypeKey,
                baseCalcPriceListTypeKey), props);
        service.synchronize(true, false);
        session.apply(LM.getBL());
        session.close();
    }

    private void importPrices(List<Price> pricesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {
        if (pricesList != null) {
            int count = 200000;
            for (int start = 0; true; start += count) {

                int finish = count < pricesList.size() ? count : pricesList.size();
                List<Price> dataPrice = start < finish ? pricesList.subList(start, finish) : new ArrayList<Price>();
                if (dataPrice.isEmpty())
                    return;

                ImportField itemIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField departmentStoreIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField dateField = new ImportField(DateClass.instance);
                ImportField priceField = new ImportField(LM.findLCPByCompoundName("dataRetailPriceItemDepartmentDate"));
                ImportField markupField = new ImportField(LM.findLCPByCompoundName("dataMarkupItemDepartmentDate"));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("item"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("departmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStoreIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceField, LM.findLCPByCompoundName("dataRetailPriceItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));
                props.add(new ImportProperty(markupField, LM.findLCPByCompoundName("dataMarkupItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Price p : dataPrice) {
                    data.add(Arrays.asList((Object) (p.item == null ? null : ("I" + p.item)),
                            p.departmentStore == null ? null : ("WH" + p.departmentStore), p.date, p.price, p.markup));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemIDField, departmentStoreIDField, dateField, priceField, markupField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, departmentStoreKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
                System.out.println("done prices " + start);
            }
        }
    }

    private void importUserInvoices(List<UserInvoiceDetail> userInvoiceDetailsList, Boolean posted, Integer numberOfUserInvoicesAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        if(numberOfUserInvoicesAtATime==null)
            numberOfUserInvoicesAtATime = 20000;

        try {
            if (userInvoiceDetailsList != null) {
                for (int start = 0; true; start += numberOfUserInvoicesAtATime) {

                    int finish = (start + numberOfUserInvoicesAtATime) < userInvoiceDetailsList.size() ? (start + numberOfUserInvoicesAtATime) : userInvoiceDetailsList.size();
                    List<UserInvoiceDetail> dataUserInvoiceDetail = start < finish ? userInvoiceDetailsList.subList(start, finish) : new ArrayList<UserInvoiceDetail>();
                    if (dataUserInvoiceDetail.isEmpty())
                        return;

                    ImportField userInvoiceField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField customerDepartmentStoreIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField supplierIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField supplierWarehouseIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                    ImportField numberUserInvoiceField = new ImportField(LM.findLCPByCompoundName("numberObject"));
                    ImportField seriesUserInvoiceField = new ImportField(LM.findLCPByCompoundName("seriesObject"));
                    ImportField createPricingUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.createPricingUserInvoice"));
                    ImportField createShipmentUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.createShipmentUserInvoice"));
                    ImportField dateUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.dateUserInvoice"));
                    ImportField timeUserInvoiceField = new ImportField(TimeClass.instance);

                    ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField userInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField quantityUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail"));
                    ImportField priceUserInvoiceDetail = new ImportField(LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail"));
                    ImportField manufacturingPriceInvoiceDetail = new ImportField(LM.findLCPByCompoundName("Purchase.manufacturingPriceInvoiceDetail"));
                    ImportField chargePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail"));
                    ImportField retailPriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail"));
                    ImportField retailMarkupUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail"));
                    //ImportField numberComplianceField = new ImportField(LM.findLCPByCompoundName("numberCertificate"));
                    //ImportField dateTimeComplianceField = new ImportField(LM.findLCPByCompoundName("dateTimeCompliance"));
                    //ImportField toDateTimeComplianceField = new ImportField(LM.findLCPByCompoundName("toDateTimeCompliance"));
                    ImportField certificateTextUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("certificateTextUserInvoiceDetail"));

                    ImportKey<?> userInvoiceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName(posted ? "Purchase.userInvoicePosted" : "Purchase.userInvoice"),
                            LM.findLCPByCompoundName("Purchase.numberSeriesToUserInvoice").getMapping(numberUserInvoiceField, seriesUserInvoiceField));

                    ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(supplierIDField));

                    ImportKey<?> customerDepartmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("departmentStore"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(customerDepartmentStoreIDField));

                    ImportKey<?> supplierWarehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("warehouse"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(supplierWarehouseIDField));

                    ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.userInvoiceDetail"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(userInvoiceDetailField));

                    ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("item"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                    //ImportKey<?> complianceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("compliance"),
                    //        LM.findLCPByCompoundName("numberToCompliance").getMapping(numberComplianceField));

                    List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                    props.add(new ImportProperty(numberUserInvoiceField, LM.findLCPByCompoundName("numberObject").getMapping(userInvoiceKey)));
                    props.add(new ImportProperty(seriesUserInvoiceField, LM.findLCPByCompoundName("seriesObject").getMapping(userInvoiceKey)));
                    props.add(new ImportProperty(createShipmentUserInvoiceField, LM.findLCPByCompoundName("Purchase.createShipmentUserInvoice").getMapping(userInvoiceKey)));
                    props.add(new ImportProperty(dateUserInvoiceField, LM.findLCPByCompoundName("Purchase.dateUserInvoice").getMapping(userInvoiceKey)));
                    props.add(new ImportProperty(timeUserInvoiceField, LM.findLCPByCompoundName("Purchase.timeUserInvoice").getMapping(userInvoiceKey)));

                    props.add(new ImportProperty(supplierIDField, LM.findLCPByCompoundName("Purchase.supplierUserInvoice").getMapping(userInvoiceKey),
                            LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(supplierKey)));
                    props.add(new ImportProperty(supplierWarehouseIDField, LM.findLCPByCompoundName("Purchase.supplierStockUserInvoice").getMapping(userInvoiceKey),
                            LM.object(LM.findClassByCompoundName("stock")).getMapping(supplierWarehouseKey)));

                    props.add(new ImportProperty(customerDepartmentStoreIDField, LM.findLCPByCompoundName("Purchase.customerUserInvoice").getMapping(userInvoiceKey),
                            LM.findLCPByCompoundName("legalEntityStock").getMapping(customerDepartmentStoreKey)));
                    props.add(new ImportProperty(customerDepartmentStoreIDField, LM.findLCPByCompoundName("Purchase.customerStockUserInvoice").getMapping(userInvoiceKey),
                            LM.object(LM.findClassByCompoundName("stock")).getMapping(customerDepartmentStoreKey)));

                    props.add(new ImportProperty(userInvoiceDetailField, LM.findLCPByCompoundName("sidExternalizable").getMapping(userInvoiceDetailKey)));
                    props.add(new ImportProperty(quantityUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                    props.add(new ImportProperty(priceUserInvoiceDetail, LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                    props.add(new ImportProperty(manufacturingPriceInvoiceDetail, LM.findLCPByCompoundName("Purchase.manufacturingPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                    props.add(new ImportProperty(chargePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                    // розничная расценка
                    props.add(new ImportProperty(createPricingUserInvoiceField, LM.findLCPByCompoundName("Purchase.createPricingUserInvoice").getMapping(userInvoiceKey)));
                    props.add(new ImportProperty(retailPriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                    props.add(new ImportProperty(retailMarkupUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                    props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("Purchase.skuInvoiceDetail").getMapping(userInvoiceDetailKey),
                            LM.object(LM.findClassByCompoundName("item")).getMapping(itemKey)));

                    props.add(new ImportProperty(userInvoiceField, LM.findLCPByCompoundName("Purchase.userInvoiceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                            LM.object(LM.findClassByCompoundName(posted ? "Purchase.userInvoicePosted" : "Purchase.userInvoice")).getMapping(userInvoiceKey)));

                    //props.add(new ImportProperty(numberComplianceField, LM.findLCPByCompoundName("numberCertificate").getMapping(complianceKey)));
                    //props.add(new ImportProperty(dateTimeComplianceField, LM.findLCPByCompoundName("dateTimeCompliance").getMapping(complianceKey)));
                    //props.add(new ImportProperty(toDateTimeComplianceField, LM.findLCPByCompoundName("toDateTimeCompliance").getMapping(complianceKey)));
                    props.add(new ImportProperty(certificateTextUserInvoiceDetailField, LM.findLCPByCompoundName("certificateTextUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                    //props.add(new ImportProperty(numberComplianceField, LM.findLCPByCompoundName("complianceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                    //        LM.object(LM.findClassByCompoundName("compliance")).getMapping(complianceKey)));

                    List<List<Object>> data = new ArrayList<List<Object>>();
                    for (UserInvoiceDetail u : dataUserInvoiceDetail) {
                        data.add(Arrays.asList((Object) u.number, u.series, u.createPricing, u.createShipment,
                                u.sid == null ? null : ("UID" + u.sid), u.date, new Time(12, 0, 0), u.item == null ? null : ("I" + u.item),
                                u.quantity, u.supplier == null ? null : ("L" + u.supplier),
                                u.warehouse == null ? null : ("WH" + u.warehouse),
                                u.supplierWarehouse == null ? null : ("WH" + u.supplierWarehouse), u.price, u.price, u.chargePrice,
                                u.retailPrice, u.retailMarkup, /*u.numberCompliance == null ? null : ("CM" + u.numberCompliance), u.dateTimeCompliance, u.toDateTimeCompliance,*/
                                u.textCompliance));
                    }
                    ImportTable table = new ImportTable(Arrays.asList(numberUserInvoiceField, seriesUserInvoiceField,
                            createPricingUserInvoiceField, createShipmentUserInvoiceField, userInvoiceDetailField,
                            dateUserInvoiceField, timeUserInvoiceField, itemField, quantityUserInvoiceDetailField, supplierIDField,
                            customerDepartmentStoreIDField, supplierWarehouseIDField, priceUserInvoiceDetail,
                            manufacturingPriceInvoiceDetail,
                            chargePriceUserInvoiceDetailField, retailPriceUserInvoiceDetailField,
                            retailMarkupUserInvoiceDetailField, /*numberComplianceField, dateTimeComplianceField,
                            toDateTimeComplianceField,*/ certificateTextUserInvoiceDetailField), data);

                    DataSession session = LM.getBL().createSession();
                    IntegrationService service = new IntegrationService(session, table, Arrays.asList(userInvoiceKey, userInvoiceDetailKey,
                            itemKey, supplierKey, customerDepartmentStoreKey/*, complianceKey*/), props);
                    service.synchronize(true, false);
                    session.apply(LM.getBL());
                    session.close();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importAssortment(List<Assortment> assortmentsList, List<StockSupplier> stockSuppliersList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if ((assortmentsList != null) && (stockSuppliersList != null)) {
                int count = 50000;
                for (int start = 0; true; start += count) {

                    int finish = count < assortmentsList.size() ? count : assortmentsList.size();
                    List<Assortment> dataAssortment = start < finish ? assortmentsList.subList(start, finish) : new ArrayList<Assortment>();
                    if (dataAssortment.isEmpty())
                        break;

                    DataSession currencySession = LM.getBL().createSession();

                    DataObject dataPriceListTypeObject = currencySession.addObject((ConcreteCustomClass) LM.findClassByCompoundName("dataPriceListType"));
                    Object defaultCurrency = LM.findLCPByCompoundName("currencyShortName").read(currencySession, new DataObject("BLR", StringClass.get(3)));
                    LM.findLCPByCompoundName("name").change("Поставщика", currencySession, dataPriceListTypeObject);
                    LM.findLCPByCompoundName("currencyDataPriceListType").change(defaultCurrency, currencySession, dataPriceListTypeObject);
                    currencySession.apply(LM.getBL());

                    ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField legalEntityField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField userPriceListField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField departmentStoreField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                    ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(LM.findLCPByCompoundName("pricePriceListDetailDataPriceListType"));
                    ImportField inPriceListPriceListTypeField = new ImportField(LM.findLCPByCompoundName("inPriceListDataPriceListType"));


                    ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityField));

                    ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("userPriceList"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(userPriceListField));

                    ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("item"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                    ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("currency"),
                            LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                    ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("userPriceListDetail"),
                            LM.findLCPByCompoundName("userPriceListDetailSkuSIDUserPriceListSID").getMapping(itemField, userPriceListField));

                    List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                    props.add(new ImportProperty(userPriceListField, LM.findLCPByCompoundName("sidExternalizable").getMapping(userPriceListKey)));
                    props.add(new ImportProperty(legalEntityField, LM.findLCPByCompoundName("companyUserPriceList").getMapping(userPriceListKey),
                            LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(legalEntityKey)));
                    props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                            LM.object(LM.findClassByCompoundName("item")).getMapping(itemKey)));
                    props.add(new ImportProperty(legalEntityField, LM.findLCPByCompoundName("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                            LM.object(LM.findClassByCompoundName("userPriceList")).getMapping(userPriceListKey)));
                    props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyUserPriceList").getMapping(userPriceListKey),
                            LM.object(LM.findClassByCompoundName("currency")).getMapping(currencyKey)));
                    props.add(new ImportProperty(inPriceListPriceListTypeField, LM.findLCPByCompoundName("inUserPriceListDataPriceListType").getMapping(userPriceListKey, new DataObject(dataPriceListTypeObject.object, (ConcreteClass) LM.findClassByCompoundName("dataPriceListType")))));
                    props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, LM.findLCPByCompoundName("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, new DataObject(dataPriceListTypeObject.object, (ConcreteClass) LM.findClassByCompoundName("dataPriceListType"))/*, itemKey, departmentStoreKey, defaultDate*/)));

                    List<List<Object>> data = new ArrayList<List<Object>>();
                    for (Assortment a : dataAssortment) {
                        data.add(Arrays.asList((Object) (a.item == null ? null : ("I" + a.item)),
                                a.supplier == null ? null : ("L" + a.supplier),
                                a.userPriceList == null ? null : ("PL" + a.userPriceList),
                                a.departmentStore == null ? null : ("WH" + a.departmentStore), a.currency, a.price,
                                a.inPriceList));
                    }
                    ImportTable table = new ImportTable(Arrays.asList(itemField, legalEntityField, userPriceListField, departmentStoreField, currencyField, pricePriceListDetailDataPriceListTypeField, inPriceListPriceListTypeField), data);

                    DataSession session = LM.getBL().createSession();
                    IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey, userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                    service.synchronize(true, false);
                    session.apply(LM.getBL());
                    session.close();

                    System.out.println("done assortment " + dataAssortment.size());

                    ImportField inPriceList2Field = new ImportField(LM.findLCPByCompoundName("inPriceList"));
                    ImportField inPriceListStock2Field = new ImportField(LM.findLCPByCompoundName("inPriceListStock"));
                    ImportField userPriceList2Field = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                    ImportField departmentStore2Field = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                    ImportKey<?> userPriceList2Key = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("userPriceList"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(userPriceList2Field));
                    ImportKey<?> departmentStore2Key = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("departmentStore"),
                            LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStore2Field));

                    props = new ArrayList<ImportProperty<?>>();
                    props.add(new ImportProperty(userPriceList2Field, LM.findLCPByCompoundName("sidExternalizable").getMapping(userPriceList2Key)));
                    props.add(new ImportProperty(inPriceList2Field, LM.findLCPByCompoundName("inPriceList").getMapping(userPriceList2Key)));
                    props.add(new ImportProperty(inPriceListStock2Field, LM.findLCPByCompoundName("inPriceListStock").getMapping(userPriceList2Key, departmentStore2Key)));

                    data = new ArrayList<List<Object>>();
                    for (StockSupplier s : stockSuppliersList) {
                        data.add(Arrays.asList((Object) (s.userPriceListID == null ? null : ("PL" + s.userPriceListID)),
                                s.departmentStore == null ? null : ("WH" + s.departmentStore), s.inPriceList,
                                s.inPriceListStock));
                    }

                    table = new ImportTable(Arrays.asList(userPriceList2Field, departmentStore2Field, inPriceList2Field, inPriceListStock2Field), data);

                    session = LM.getBL().createSession();
                    service = new IntegrationService(session, table, Arrays.asList(userPriceList2Key, departmentStore2Key), props);
                    service.synchronize(true, false);
                    session.apply(LM.getBL());
                    session.close();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importLegalEntities(List<LegalEntity> legalEntitiesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (legalEntitiesList != null) {
                ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameLegalEntityField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField legalAddressField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField unpField = new ImportField(LM.findLCPByCompoundName("UNPLegalEntity"));
                ImportField okpoField = new ImportField(LM.findLCPByCompoundName("OKPOLegalEntity"));
                ImportField phoneField = new ImportField(LM.findLCPByCompoundName("dataPhoneLegalEntityDate"));
                ImportField emailField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField nameOwnershipField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField shortNameOwnershipField = new ImportField(LM.findLCPByCompoundName("shortNameOwnership"));
                ImportField accountField = new ImportField(LM.findLCPByCompoundName("numberAccount"));

                ImportField chainStoresIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameChainStoresField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField bankIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportField isSupplierLegalEntityField = new ImportField(LM.findLCPByCompoundName("isSupplierLegalEntity"));
                ImportField isCompanyLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCompanyLegalEntity"));
                ImportField isCustomerLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCustomerLegalEntity"));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ownership"),
                        LM.findLCPByCompoundName("shortNameToOwnership").getMapping(shortNameOwnershipField));

                ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank.account"),
                        LM.findLCPByCompoundName("Bank.accountNumber").getMapping(accountField));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("bank"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(bankIDField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("chainStores"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(chainStoresIDField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("country"),
                        LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(legalEntityKey)));
                props.add(new ImportProperty(nameLegalEntityField, LM.findLCPByCompoundName("name").getMapping(legalEntityKey)));
                props.add(new ImportProperty(nameLegalEntityField, LM.findLCPByCompoundName("fullNameLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(legalAddressField, LM.findLCPByCompoundName("dataAddressLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
                props.add(new ImportProperty(unpField, LM.findLCPByCompoundName("UNPLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(okpoField, LM.findLCPByCompoundName("OKPOLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(phoneField, LM.findLCPByCompoundName("dataPhoneLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
                props.add(new ImportProperty(emailField, LM.findLCPByCompoundName("emailLegalEntity").getMapping(legalEntityKey)));

                props.add(new ImportProperty(isSupplierLegalEntityField, LM.findLCPByCompoundName("isSupplierLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(isCompanyLegalEntityField, LM.findLCPByCompoundName("isCompanyLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(isCustomerLegalEntityField, LM.findLCPByCompoundName("isCustomerLegalEntity").getMapping(legalEntityKey)));

                props.add(new ImportProperty(nameOwnershipField, LM.findLCPByCompoundName("name").getMapping(ownershipKey)));
                props.add(new ImportProperty(shortNameOwnershipField, LM.findLCPByCompoundName("shortNameOwnership").getMapping(ownershipKey)));
                props.add(new ImportProperty(shortNameOwnershipField, LM.findLCPByCompoundName("ownershipLegalEntity").getMapping(legalEntityKey),
                        LM.object(LM.findClassByCompoundName("ownership")).getMapping(ownershipKey)));

                props.add(new ImportProperty(accountField, LM.findLCPByCompoundName("Bank.numberAccount").getMapping(accountKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("Bank.legalEntityAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(chainStoresIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(chainStoresKey)));
                props.add(new ImportProperty(nameChainStoresField, LM.findLCPByCompoundName("name").getMapping(chainStoresKey)));

                props.add(new ImportProperty(bankIDField, LM.findLCPByCompoundName("Bank.bankAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("bank")).getMapping(bankKey)));

                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("name").getMapping(countryKey)));
                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryLegalEntity").getMapping(legalEntityKey),
                        LM.object(LM.findClassByCompoundName("country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : legalEntitiesList) {
                    data.add(Arrays.asList((Object) (l.legalEntityID == null ? null : ("L" + l.legalEntityID)),
                            l.nameLegalEntity, l.address, l.unp, l.okpo, l.phone, l.email,
                            l.nameOwnership, l.shortNameOwnership, l.account,
                            l.chainStoresID == null ? null : ("CS" + l.chainStoresID), l.nameChainStores,
                            l.bankID == null ? null : ("B" + l.bankID), l.country, l.isSupplierLegalEntity,
                            l.isCompanyLegalEntity, l.isCustomerLegalEntity));
                }

                ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, nameLegalEntityField, legalAddressField,
                        unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                        accountField, chainStoresIDField, nameChainStoresField, bankIDField, nameCountryField,
                        isSupplierLegalEntityField, isCompanyLegalEntityField, isCustomerLegalEntityField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, ownershipKey, accountKey, bankKey, chainStoresKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importEmployees(List<Employee> employeesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (employeesList != null) {
                ImportField employeeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField employeeFirstNameField = new ImportField(LM.findLCPByCompoundName("userFirstName"));
                ImportField employeeLastNameField = new ImportField(LM.findLCPByCompoundName("userLastName"));
                ImportField positionIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField positionNameField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> employeeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("employee"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(employeeIDField));

                ImportKey<?> positionKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("position"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(positionIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(employeeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeFirstNameField, LM.findLCPByCompoundName("userFirstName").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeLastNameField, LM.findLCPByCompoundName("userLastName").getMapping(employeeKey)));
                props.add(new ImportProperty(positionIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(positionKey)));
                props.add(new ImportProperty(positionNameField, LM.findLCPByCompoundName("name").getMapping(positionKey)));
                props.add(new ImportProperty(positionIDField, LM.findLCPByCompoundName("positionEmployee").getMapping(employeeKey),
                        LM.object(LM.findClassByCompoundName("position")).getMapping(positionKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Employee e : employeesList) {
                    data.add(Arrays.asList((Object) (e.employeeID == null ? null : ("LE" + e.employeeID)),
                            e.firstName, e.lastName, e.position == null ? null : ("EP" + e.position), e.position));
                }

                ImportTable table = new ImportTable(Arrays.asList(employeeIDField, employeeFirstNameField,
                        employeeLastNameField, positionIDField, positionNameField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(employeeKey, positionKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWarehouseGroups(List<WarehouseGroup> warehouseGroupsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (warehouseGroupsList != null) {
                ImportField warehouseGroupIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameWarehouseGroupField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("warehouseGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseGroupIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(warehouseGroupIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseGroupKey)));
                props.add(new ImportProperty(nameWarehouseGroupField, LM.findLCPByCompoundName("name").getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (WarehouseGroup wg : warehouseGroupsList) {
                    data.add(Arrays.asList((Object) (wg.warehouseGroupID == null ? null : ("WG" + wg.warehouseGroupID)),
                            wg.name));
                }

                ImportTable table = new ImportTable(Arrays.asList(warehouseGroupIDField, nameWarehouseGroupField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWarehouses(List<Warehouse> warehousesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (warehousesList != null) {
                ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField warehouseGroupIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField warehouseIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameWarehouseField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField addressWarehouseField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("warehouseGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseGroupIDField));

                ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("warehouse"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(warehouseIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseKey)));
                props.add(new ImportProperty(nameWarehouseField, LM.findLCPByCompoundName("name").getMapping(warehouseKey)));
                props.add(new ImportProperty(addressWarehouseField, LM.findLCPByCompoundName("addressWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("legalEntityWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(warehouseGroupIDField, LM.findLCPByCompoundName("warehouseGroupWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("warehouseGroup")).getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Warehouse w : warehousesList) {
                    data.add(Arrays.asList((Object) (w.legalEntityID == null ? null : ("L" + w.legalEntityID)),
                            w.warehouseGroupID == null ? null : ("WG" + w.warehouseGroupID),
                            w.sid == null ? null : ("WH" + w.sid), w.name, w.address));
                }

                ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, warehouseGroupIDField,
                        warehouseIDField, nameWarehouseField, addressWarehouseField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, warehouseKey, warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importStores(List<LegalEntity> storesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (storesList != null) {
                ImportField storeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameStoreField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField addressStoreField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField chainStoresIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField storeTypeField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("store"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(storeIDField));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("chainStores"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(chainStoresIDField));

                ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("storeType"),
                        LM.findLCPByCompoundName("storeTypeNameChainStores").getMapping(storeTypeField, chainStoresIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(storeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(storeKey)));
                props.add(new ImportProperty(nameStoreField, LM.findLCPByCompoundName("name").getMapping(storeKey)));
                props.add(new ImportProperty(addressStoreField, LM.findLCPByCompoundName("addressStore").getMapping(storeKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("legalEntityStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("name").getMapping(storeTypeKey)));
                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("storeTypeStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("storeType")).getMapping(storeTypeKey)));
                props.add(new ImportProperty(chainStoresIDField, LM.findLCPByCompoundName("chainStoresStoreType").getMapping(storeTypeKey),
                        LM.object(LM.findClassByCompoundName("chainStores")).getMapping(chainStoresKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : storesList) {
                    Store s = (Store) l;
                    data.add(Arrays.asList((Object) ("S" + s.storeID), s.nameLegalEntity, s.address, "L" + s.legalEntityID, s.storeType, "CS" + s.chainStoresID));
                }

                ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, legalEntityIDField, storeTypeField, chainStoresIDField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, legalEntityKey, chainStoresKey, storeTypeKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importDepartmentStores(List<DepartmentStore> departmentStoresList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (departmentStoresList != null) {
                ImportField departmentStoreIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameDepartmentStoreField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField storeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("departmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStoreIDField));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("store"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(storeIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(departmentStoreIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(nameDepartmentStoreField, LM.findLCPByCompoundName("name").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(storeIDField, LM.findLCPByCompoundName("storeDepartmentStore").getMapping(departmentStoreKey),
                        LM.object(LM.findClassByCompoundName("store")).getMapping(storeKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (DepartmentStore s : departmentStoresList) {
                    data.add(Arrays.asList((Object) ("WH" + s.departmentStoreID), s.name, "S" + s.storeID));
                }
                ImportTable table = new ImportTable(Arrays.asList(departmentStoreIDField, nameDepartmentStoreField, storeIDField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importBanks(List<Bank> banksList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (banksList != null) {
                ImportField bankIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameBankField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField addressBankField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField departmentBankField = new ImportField(LM.findLCPByCompoundName("departmentBank"));
                ImportField mfoBankField = new ImportField(LM.findLCPByCompoundName("MFOBank"));
                ImportField cbuBankField = new ImportField(LM.findLCPByCompoundName("CBUBank"));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("bank"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(bankIDField));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(bankIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(bankKey)));
                props.add(new ImportProperty(nameBankField, LM.findLCPByCompoundName("name").getMapping(bankKey)));
                props.add(new ImportProperty(addressBankField, LM.findLCPByCompoundName("dataAddressBankDate").getMapping(bankKey, defaultDate)));
                props.add(new ImportProperty(departmentBankField, LM.findLCPByCompoundName("departmentBank").getMapping(bankKey)));
                props.add(new ImportProperty(mfoBankField, LM.findLCPByCompoundName("MFOBank").getMapping(bankKey)));
                props.add(new ImportProperty(cbuBankField, LM.findLCPByCompoundName("CBUBank").getMapping(bankKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Bank b : banksList) {
                    data.add(Arrays.asList((Object) ("B" + b.bankID), b.name, b.address, b.department, b.mfo, b.cbu));
                }
                ImportTable table = new ImportTable(Arrays.asList(bankIDField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importRateWastes(List<RateWaste> rateWastesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (rateWastesList != null) {
                ImportField writeOffRateIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameWriteOffRateField = new ImportField(LM.findLCPByCompoundName("name"));
                ImportField percentWriteOffRateField = new ImportField(LM.findLCPByCompoundName("percentWriteOffRate"));
                ImportField countryWriteOffRateField = new ImportField(LM.findLCPByCompoundName("name"));

                ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("writeOffRate"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(writeOffRateIDField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("country"),
                        LM.findLCPByCompoundName("countryName").getMapping(countryWriteOffRateField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(writeOffRateIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(nameWriteOffRateField, LM.findLCPByCompoundName("name").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(percentWriteOffRateField, LM.findLCPByCompoundName("percentWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(countryWriteOffRateField, LM.findLCPByCompoundName("countryWriteOffRate").getMapping(writeOffRateKey),
                        LM.object(LM.findClassByCompoundName("country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (RateWaste r : rateWastesList) {
                    data.add(Arrays.asList((Object) ("RW" + r.rateWasteID), r.name, r.coef, r.country));
                }
                ImportTable table = new ImportTable(Arrays.asList(writeOffRateIDField, nameWriteOffRateField, percentWriteOffRateField, countryWriteOffRateField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(writeOffRateKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importContracts(List<Contract> contractsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (contractsList != null) {
                ImportField contractIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField supplierIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField customerIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField numberContractField = new ImportField(LM.findLCPByCompoundName("numberContract"));
                ImportField dateFromContractField = new ImportField(LM.findLCPByCompoundName("dateFromContract"));
                ImportField dateToContractField = new ImportField(LM.findLCPByCompoundName("dateToContract"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));

                ImportKey<?> contractKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("userContractSku"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(contractIDField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(supplierIDField));

                ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(customerIDField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(contractIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(contractKey)));
                props.add(new ImportProperty(numberContractField, LM.findLCPByCompoundName("numberContract").getMapping(contractKey)));
                props.add(new ImportProperty(dateFromContractField, LM.findLCPByCompoundName("dateFromContract").getMapping(contractKey)));
                props.add(new ImportProperty(dateToContractField, LM.findLCPByCompoundName("dateToContract").getMapping(contractKey)));
                props.add(new ImportProperty(supplierIDField, LM.findLCPByCompoundName("supplierContractSku").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(customerIDField, LM.findLCPByCompoundName("customerContractSku").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(customerKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyContract").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("currency")).getMapping(currencyKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Contract c : contractsList) {
                    data.add(Arrays.asList((Object) ("CN" + c.contractID), c.number, c.dateFrom, c.dateTo,
                            c.supplierID == null ? c.supplierID : ("L" + c.supplierID),
                            c.customerID == null ? c.customerID : ("L" + c.customerID), c.currency));
                }
                ImportTable table = new ImportTable(Arrays.asList(contractIDField, numberContractField,
                        dateFromContractField, dateToContractField, supplierIDField, customerIDField,
                        currencyField), data);

                DataSession session = LM.getBL().createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(contractKey,
                        supplierKey, customerKey), props);
                service.synchronize(true, false);
                session.apply(LM.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}