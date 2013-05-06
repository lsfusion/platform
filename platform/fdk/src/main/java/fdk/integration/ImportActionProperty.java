package fdk.integration;

import org.xBaseJ.xBaseJException;
import platform.server.classes.*;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            context.getSession().apply(context.getBL());

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

            importPriceListStores(importData.getPriceListStoresList(), importData.getNumberOfPriceListsAtATime());

            importPriceListSuppliers(importData.getPriceListSuppliersList(), importData.getNumberOfPriceListsAtATime());

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

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupID));
                ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(parentGroupID));

                List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
                propsParent.add(new ImportProperty(parentGroupID, LM.findLCPByCompoundName("parentItemGroup").getMapping(itemGroupKey),
                        LM.object(LM.findClassByCompoundName("ItemGroup")).getMapping(parentGroupKey)));
                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup p : parentGroupsList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(p.sid, "IG", null), valueWithPrefix(p.parent, "IG", null)));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField itemGroupName = new ImportField(LM.findLCPByCompoundName("nameItemGroup"));

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupID));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
                props.add(new ImportProperty(itemGroupID, LM.findLCPByCompoundName("sidExternalizable").getMapping(itemGroupKey)));
                props.add(new ImportProperty(itemGroupName, LM.findLCPByCompoundName("nameItemGroup").getMapping(itemGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup i : itemGroupsList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(i.sid, "IG", null), i.name));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField wareNameField = new ImportField(LM.findLCPByCompoundName("nameWare"));
                ImportField warePriceField = new ImportField(LM.findLCPByCompoundName("warePrice"));

                ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ware"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(wareIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(wareKey)));
                props.add(new ImportProperty(wareNameField, LM.findLCPByCompoundName("nameWare").getMapping(wareKey)));
                props.add(new ImportProperty(warePriceField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, defaultDate)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Ware w : waresList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(w.wareID, "W", null), w.name, w.price));
                }
                ImportTable table = new ImportTable(Arrays.asList(wareIDField, wareNameField, warePriceField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
        if (dataItems.size() == 0) return;

        ImportField itemIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField itemGroupIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField itemCaptionField = new ImportField(LM.findLCPByCompoundName("captionItem"));
        ImportField UOMIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField nameUOMField = new ImportField(LM.findLCPByCompoundName("nameUOM"));
        ImportField shortNameUOMField = new ImportField(LM.findLCPByCompoundName("shortNameUOM"));
        ImportField brandIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField nameBrandField = new ImportField(LM.findLCPByCompoundName("nameBrand"));
        ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("nameCountry"));
        ImportField barcodeField = new ImportField(LM.findLCPByCompoundName("idBarcode"));
        ImportField barcodeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField dateField = new ImportField(DateClass.instance);
        ImportField isWeightItemField = new ImportField(LM.findLCPByCompoundName("isWeightItem"));
        ImportField netWeightItemField = new ImportField(LM.findLCPByCompoundName("netWeightItem"));
        ImportField grossWeightItemField = new ImportField(LM.findLCPByCompoundName("grossWeightItem"));
        ImportField compositionField = new ImportField(LM.findLCPByCompoundName("compositionItem"));
        ImportField valueVATItemCountryDateField = new ImportField(LM.findLCPByCompoundName("valueVATItemCountryDate"));
        ImportField wareIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField priceWareField = new ImportField(LM.findLCPByCompoundName("dataWarePriceDate"));
        ImportField ndsWareField = new ImportField(LM.findLCPByCompoundName("valueRate"));
        ImportField writeOffRateIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField retailCalcPriceListTypeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField retailCalcPriceListTypeNameField = new ImportField(LM.findLCPByCompoundName("namePriceListType"));
        ImportField retailMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));
        ImportField baseCalcPriceListTypeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField baseCalcPriceListTypeNameField = new ImportField(LM.findLCPByCompoundName("namePriceListType"));
        ImportField baseMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));
        ImportField barcodePackField = new ImportField(LM.findLCPByCompoundName("idBarcode"));
        ImportField barcodePackIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
        ImportField amountBarcodePackField = new ImportField(LM.findLCPByCompoundName("amountBarcode"));

        DataObject defaultCountryObject = (DataObject) LM.findLCPByCompoundName("defaultCountry").readClasses(context.getSession());

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(itemIDField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(itemGroupIDField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(UOMIDField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Brand"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(brandIDField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Barcode"),
                LM.findLCPByCompoundName(/*"barcodeIdDate"*/"externalizableSID").getMapping(barcodeIDField));

        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(valueVATItemCountryDateField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ware"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(wareIDField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(ndsWareField));

        ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WriteOffRate"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(writeOffRateIDField));

        ImportKey<?> baseCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CalcPriceListType"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(baseCalcPriceListTypeIDField));

        ImportKey<?> retailCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CalcPriceListType"),
                LM.findLCPByCompoundName("externalizableSID").getMapping(retailCalcPriceListTypeIDField));

        ImportKey<?> barcodePackKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Barcode"),
                LM.findLCPByCompoundName(/*"barcodeIdDate"*/"externalizableSID").getMapping(barcodePackIDField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(itemGroupIDField, LM.findLCPByCompoundName("itemGroupItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("ItemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(itemIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(itemKey)));
        props.add(new ImportProperty(itemCaptionField, LM.findLCPByCompoundName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(UOMIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, LM.findLCPByCompoundName("nameUOM").getMapping(UOMKey)));
        props.add(new ImportProperty(shortNameUOMField, LM.findLCPByCompoundName("shortNameUOM").getMapping(UOMKey)));
        props.add(new ImportProperty(UOMIDField, LM.findLCPByCompoundName("UOMItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, LM.findLCPByCompoundName("nameBrand").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(brandKey)));
        props.add(new ImportProperty(brandIDField, LM.findLCPByCompoundName("brandItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Brand")).getMapping(brandKey)));

        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("nameCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

        props.add(new ImportProperty(barcodeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(barcodeKey)));
        props.add(new ImportProperty(barcodeField, LM.findLCPByCompoundName("idBarcode").getMapping(barcodeKey)));
        props.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dataDateBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(itemIDField, LM.findLCPByCompoundName("skuBarcode").getMapping(barcodeKey),
                LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));

        props.add(new ImportProperty(isWeightItemField, LM.findLCPByCompoundName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(netWeightItemField, LM.findLCPByCompoundName("netWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(grossWeightItemField, LM.findLCPByCompoundName("grossWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionField, LM.findLCPByCompoundName("compositionItem").getMapping(itemKey)));
        props.add(new ImportProperty(valueVATItemCountryDateField, LM.findLCPByCompoundName("dataVATItemCountryDate").getMapping(itemKey, defaultCountryObject, dateField),
                LM.object(LM.findClassByCompoundName("Range")).getMapping(VATKey)));

        props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(wareKey)));
        props.add(new ImportProperty(wareIDField, LM.findLCPByCompoundName("wareItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Ware")).getMapping(wareKey)));

        props.add(new ImportProperty(priceWareField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, LM.findLCPByCompoundName("dataRangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(LM.findClassByCompoundName("Range")).getMapping(rangeKey)));

        props.add(new ImportProperty(writeOffRateIDField, LM.findLCPByCompoundName("writeOffRateCountryItem").getMapping(defaultCountryObject, itemKey),
                LM.object(LM.findClassByCompoundName("WriteOffRate")).getMapping(writeOffRateKey)));

        props.add(new ImportProperty(retailCalcPriceListTypeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(retailCalcPriceListTypeNameField, LM.findLCPByCompoundName("namePriceListType").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(retailMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(retailCalcPriceListTypeKey, itemKey)));
        props.add(new ImportProperty(baseCalcPriceListTypeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(baseCalcPriceListTypeNameField, LM.findLCPByCompoundName("namePriceListType").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(baseMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(baseCalcPriceListTypeKey, itemKey)));

        props.add(new ImportProperty(barcodePackIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(barcodePackKey)));
        props.add(new ImportProperty(barcodePackField, LM.findLCPByCompoundName("idBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dataDateBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(amountBarcodePackField, LM.findLCPByCompoundName("amountBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(barcodePackIDField, LM.findLCPByCompoundName("Purchase.packBarcodeSku").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Barcode")).getMapping(barcodePackKey)));
        props.add(new ImportProperty(barcodePackIDField, LM.findLCPByCompoundName("Sale.packBarcodeSku").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Barcode")).getMapping(barcodePackKey)));
        props.add(new ImportProperty(itemIDField, LM.findLCPByCompoundName("skuBarcode").getMapping(barcodePackKey),
                LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (Item i : dataItems) {
            data.add(Arrays.asList((Object) (valueWithPrefix(i.itemID, "I", null)), valueWithPrefix(i.k_grtov, "IG", null),
                    i.name, i.uomName, i.uomShortName, valueWithPrefix(i.uomID, "UOM", null), i.brandName, i.brandID,
                    i.country, i.barcode, valueWithPrefix(i.barcodeID, "BI", null), i.date, i.isWeightItem, i.netWeightItem,
                    i.grossWeightItem, i.composition, i.retailVAT, valueWithPrefix(i.wareID, "W", null), i.priceWare,
                    i.wareVAT, valueWithPrefix(i.writeOffRateID, "RW", null), "cplt_retail", "Розничная надбавка",
                    i.retailMarkup, "cplt_wholesale", "Оптовая надбавка", i.baseMarkup, null,
                    valueWithPrefix(i.packBarcodeID, "BP", null), i.amountPack));
        }

        ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, nameUOMField,
                shortNameUOMField, UOMIDField, nameBrandField, brandIDField, nameCountryField, barcodeField, barcodeIDField,
                dateField, isWeightItemField, netWeightItemField, grossWeightItemField, compositionField,
                valueVATItemCountryDateField, wareIDField, priceWareField, ndsWareField, writeOffRateIDField,
                retailCalcPriceListTypeIDField, retailCalcPriceListTypeNameField, retailMarkupCalcPriceListTypeField,
                baseCalcPriceListTypeIDField, baseCalcPriceListTypeNameField, baseMarkupCalcPriceListTypeField,
                barcodePackField, barcodePackIDField, amountBarcodePackField), data);

        DataSession session = context.createSession();
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, VATKey, wareKey, rangeKey, writeOffRateKey, retailCalcPriceListTypeKey,
                baseCalcPriceListTypeKey, barcodePackKey), props);
        service.synchronize(true, false);
        session.apply(context.getBL());
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

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemIDField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStoreIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(priceField, LM.findLCPByCompoundName("dataRetailPriceItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));
                props.add(new ImportProperty(markupField, LM.findLCPByCompoundName("dataMarkupItemDepartmentDate").getMapping(itemKey, departmentStoreKey, dateField)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Price p : dataPrice) {
                    data.add(Arrays.asList((Object) valueWithPrefix(p.item, "I", null),
                            valueWithPrefix(p.departmentStore, "WH", null), p.date, p.price, p.markup));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemIDField, departmentStoreIDField, dateField, priceField, markupField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, departmentStoreKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
                System.out.println("done prices " + start);
            }
        }
    }

    public Boolean showManufacturingPrice;
    public Boolean showWholesalePrice;

    private void importUserInvoices(List<UserInvoiceDetail> userInvoiceDetailsList, Boolean posted, Integer numberAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        if (userInvoiceDetailsList != null) {

            if (numberAtATime == null)
                numberAtATime = userInvoiceDetailsList.size();

            for (int start = 0; true; start += numberAtATime) {

                int finish = (start + numberAtATime) < userInvoiceDetailsList.size() ? (start + numberAtATime) : userInvoiceDetailsList.size();
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
                ImportField showManufacturingPriceUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.showManufacturingPriceUserInvoice"));
                ImportField showWholesalePriceUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.showWholesalePriceUserInvoice"));
                ImportField dateUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.dateUserInvoice"));
                ImportField timeUserInvoiceField = new ImportField(TimeClass.instance);

                ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField userInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField quantityUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail"));
                ImportField priceUserInvoiceDetail = new ImportField(LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail"));
                ImportField manufacturingPriceInvoiceDetail = new ImportField(LM.findLCPByCompoundName("Purchase.manufacturingPriceInvoiceDetail"));
                ImportField chargePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail"));
                ImportField manufacturingPriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.manufacturingPriceUserInvoiceDetail"));
                ImportField wholesalePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.wholesalePriceUserInvoiceDetail"));
                ImportField wholesaleMarkupUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.wholesaleMarkupUserInvoiceDetail"));
                ImportField retailPriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail"));
                ImportField retailMarkupUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail"));
                ImportField certificateTextUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("certificateTextUserInvoiceDetail"));
                ImportField skipCreateWareUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("skipCreateWareUserInvoiceDetail"));
                ImportField contractIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                ImportKey<?> userInvoiceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName(posted ? "Purchase.UserInvoicePosted" : "Purchase.UserInvoice"),
                        LM.findLCPByCompoundName("numberSeriesToUserInvoice").getMapping(numberUserInvoiceField, seriesUserInvoiceField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(supplierIDField));

                ImportKey<?> customerDepartmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(customerDepartmentStoreIDField));

                ImportKey<?> supplierWarehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(supplierWarehouseIDField));

                ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.UserInvoiceDetail"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(userInvoiceDetailField));

                ImportKey<?> itemKey = new ImportKey((CustomClass) LM.findClassByCompoundName("Sku"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                ImportKey<?> contractKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserContractSku"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(contractIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(numberUserInvoiceField, LM.findLCPByCompoundName("numberObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(seriesUserInvoiceField, LM.findLCPByCompoundName("seriesObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(createShipmentUserInvoiceField, LM.findLCPByCompoundName("Purchase.createShipmentUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(dateUserInvoiceField, LM.findLCPByCompoundName("Purchase.dateUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(timeUserInvoiceField, LM.findLCPByCompoundName("Purchase.timeUserInvoice").getMapping(userInvoiceKey)));

                props.add(new ImportProperty(supplierIDField, LM.findLCPByCompoundName("Purchase.supplierUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(supplierWarehouseIDField, LM.findLCPByCompoundName("Purchase.supplierStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Stock")).getMapping(supplierWarehouseKey)));

                props.add(new ImportProperty(customerDepartmentStoreIDField, LM.findLCPByCompoundName("Purchase.customerUserInvoice").getMapping(userInvoiceKey),
                        LM.findLCPByCompoundName("legalEntityStock").getMapping(customerDepartmentStoreKey)));
                props.add(new ImportProperty(customerDepartmentStoreIDField, LM.findLCPByCompoundName("Purchase.customerStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Stock")).getMapping(customerDepartmentStoreKey)));

                props.add(new ImportProperty(userInvoiceDetailField, LM.findLCPByCompoundName("sidExternalizable").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(quantityUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceUserInvoiceDetail, LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(showManufacturingPriceUserInvoiceField, LM.findLCPByCompoundName("Purchase.showManufacturingPriceUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(manufacturingPriceInvoiceDetail, LM.findLCPByCompoundName("Purchase.manufacturingPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(showManufacturingPriceUserInvoiceField, LM.findLCPByCompoundName("Purchase.showManufacturingPriceUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(chargePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(showWholesalePriceUserInvoiceField, LM.findLCPByCompoundName("Purchase.showWholesalePriceUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(wholesalePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.wholesalePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(wholesaleMarkupUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.wholesaleMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                // розничная расценка
                props.add(new ImportProperty(createPricingUserInvoiceField, LM.findLCPByCompoundName("Purchase.createPricingUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(retailPriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(retailMarkupUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("Purchase.skuInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Sku")).getMapping(itemKey)));

                props.add(new ImportProperty(userInvoiceField, LM.findLCPByCompoundName("Purchase.userInvoiceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName(posted ? "Purchase.UserInvoicePosted" : "Purchase.UserInvoice")).getMapping(userInvoiceKey)));

                props.add(new ImportProperty(certificateTextUserInvoiceDetailField, LM.findLCPByCompoundName("certificateTextUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(skipCreateWareUserInvoiceDetailField, LM.findLCPByCompoundName("skipCreateWareUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(contractIDField, LM.findLCPByCompoundName("Purchase.contractSkuInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Contract")).getMapping(contractKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (UserInvoiceDetail u : dataUserInvoiceDetail) {
                    data.add(Arrays.asList((Object) u.number, u.series, u.createPricing, u.createShipment,
                            showManufacturingPrice, showWholesalePrice,
                            valueWithPrefix(u.sid, "UID", null), u.date, new Time(12, 0, 0),
                            valueWithPrefix(u.itemID, (u.isWare ? "W" : "I"), null), u.quantity,
                            valueWithPrefix(u.supplier, "L", null), valueWithPrefix(u.customerWarehouse, "WH", null),
                            valueWithPrefix(u.supplierWarehouse, "WH", null), u.price, u.price, u.chargePrice,
                            u.manufacturingPrice, u.wholesalePrice, u.wholesaleMarkup, u.retailPrice, u.retailMarkup,
                            u.textCompliance, true, valueWithPrefix(u.contractID, "CN", null)));
                }
                ImportTable table = new ImportTable(Arrays.asList(numberUserInvoiceField, seriesUserInvoiceField,
                        createPricingUserInvoiceField, createShipmentUserInvoiceField,
                        showManufacturingPriceUserInvoiceField, showWholesalePriceUserInvoiceField,
                        userInvoiceDetailField,
                        dateUserInvoiceField, timeUserInvoiceField, itemField, quantityUserInvoiceDetailField, supplierIDField,
                        customerDepartmentStoreIDField, supplierWarehouseIDField, priceUserInvoiceDetail,
                        manufacturingPriceInvoiceDetail,
                        chargePriceUserInvoiceDetailField,
                        manufacturingPriceUserInvoiceDetailField, wholesalePriceUserInvoiceDetailField, wholesaleMarkupUserInvoiceDetailField,
                        retailPriceUserInvoiceDetailField, retailMarkupUserInvoiceDetailField,
                        certificateTextUserInvoiceDetailField, skipCreateWareUserInvoiceDetailField,
                        contractIDField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userInvoiceKey, userInvoiceDetailKey,
                        itemKey, supplierKey, customerDepartmentStoreKey, contractKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
            }
        }
    }

    private void importPriceListStores(List<PriceListStore> priceListStoresList, Integer numberAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        if (priceListStoresList != null) {

            if (numberAtATime == null)
                numberAtATime = priceListStoresList.size();

            for (int start = 0; true; start += numberAtATime) {

                int finish = (start + numberAtATime) < priceListStoresList.size() ? (start + numberAtATime) : priceListStoresList.size();
                List<PriceListStore> dataPriceListStores = start < finish ? priceListStoresList.subList(start, finish) : new ArrayList<PriceListStore>();
                if (dataPriceListStores.isEmpty())
                    return;

                DataSession session = context.createSession();

                ObjectValue dataPriceListTypeObject = LM.findLCPByCompoundName("externalizableSID").readClasses(session, new DataObject("PLTCoordinated", StringClass.get(100)));
                if (dataPriceListTypeObject instanceof NullValue) {
                    dataPriceListTypeObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("DataPriceListType"));
                    Object defaultCurrency = LM.findLCPByCompoundName("currencyShortName").read(session, new DataObject("BLR", StringClass.get(3)));
                    LM.findLCPByCompoundName("namePriceListType").change("Поставщика (согласованная)", session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("currencyDataPriceListType").change(defaultCurrency, session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("sidExternalizable").change("PLTCoordinated", session, (DataObject) dataPriceListTypeObject);
                }

                ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField legalEntityField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField userPriceListField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField departmentStoreField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(LM.findLCPByCompoundName("pricePriceListDetailDataPriceListType"));
                ImportField inPriceListPriceListTypeField = new ImportField(LM.findLCPByCompoundName("inPriceListDataPriceListType"));
                ImportField inPriceListStockField = new ImportField(LM.findLCPByCompoundName("inPriceListStock"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStoreField));

                ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceList"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(userPriceListField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceListDetail"),
                        LM.findLCPByCompoundName("userPriceListDetailSkuSIDUserPriceListSID").getMapping(itemField, userPriceListField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(userPriceListField, LM.findLCPByCompoundName("sidExternalizable").getMapping(userPriceListKey)));
                props.add(new ImportProperty(legalEntityField, LM.findLCPByCompoundName("companyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));
                props.add(new ImportProperty(userPriceListField, LM.findLCPByCompoundName("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("UserPriceList")).getMapping(userPriceListKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
                props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, LM.findLCPByCompoundName("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListPriceListTypeField, LM.findLCPByCompoundName("inPriceListDataPriceListType").getMapping(userPriceListKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListStockField, LM.findLCPByCompoundName("inPriceListStock").getMapping(userPriceListKey, departmentStoreKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (PriceListStore a : dataPriceListStores) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(a.item, "I", null)), valueWithPrefix(a.supplier, "L", null),
                            valueWithPrefix(a.userPriceListID, "PL", null), valueWithPrefix(a.departmentStore, "WH", null),
                            a.currency, a.price, a.inPriceList, a.inPriceListStock));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemField, legalEntityField, userPriceListField,
                        departmentStoreField, currencyField, pricePriceListDetailDataPriceListTypeField,
                        inPriceListPriceListTypeField, inPriceListStockField), data);

                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey,
                        departmentStoreKey, userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
            }
        }
    }

    private void importPriceListSuppliers(List<PriceListSupplier> priceListSuppliersList, Integer numberAtATime) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        if (priceListSuppliersList != null) {

            if (numberAtATime == null)
                numberAtATime = priceListSuppliersList.size();

            for (int start = 0; true; start += numberAtATime) {

                int finish = (start + numberAtATime) < priceListSuppliersList.size() ? (start + numberAtATime) : priceListSuppliersList.size();
                List<PriceListSupplier> dataPriceListSuppliers = start < finish ? priceListSuppliersList.subList(start, finish) : new ArrayList<PriceListSupplier>();
                if (dataPriceListSuppliers.isEmpty())
                    return;

                DataSession session = context.createSession();

                ObjectValue dataPriceListTypeObject = LM.findLCPByCompoundName("externalizableSID").readClasses(session, new DataObject("PLTOffered", StringClass.get(100)));
                if (dataPriceListTypeObject instanceof NullValue) {
                    dataPriceListTypeObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("DataPriceListType"));
                    Object defaultCurrency = LM.findLCPByCompoundName("currencyShortName").read(session, new DataObject("BLR", StringClass.get(3)));
                    LM.findLCPByCompoundName("namePriceListType").change("Поставщика (предлагаемая)", session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("currencyDataPriceListType").change(defaultCurrency, session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("sidExternalizable").change("PLTOffered", session, (DataObject) dataPriceListTypeObject);
                }

                ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField legalEntityField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField userPriceListField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(LM.findLCPByCompoundName("pricePriceListDetailDataPriceListType"));
                ImportField inPriceListPriceListTypeField = new ImportField(LM.findLCPByCompoundName("inPriceListDataPriceListType"));
                ImportField inPriceListField = new ImportField(LM.findLCPByCompoundName("inPriceList"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityField));

                ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceList"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(userPriceListField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceListDetail"),
                        LM.findLCPByCompoundName("userPriceListDetailSkuSIDUserPriceListSID").getMapping(itemField, userPriceListField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(userPriceListField, LM.findLCPByCompoundName("sidExternalizable").getMapping(userPriceListKey)));
                props.add(new ImportProperty(legalEntityField, LM.findLCPByCompoundName("companyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));
                props.add(new ImportProperty(userPriceListField, LM.findLCPByCompoundName("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("UserPriceList")).getMapping(userPriceListKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
                props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, LM.findLCPByCompoundName("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListPriceListTypeField, LM.findLCPByCompoundName("inPriceListDataPriceListType").getMapping(userPriceListKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListField, LM.findLCPByCompoundName("inPriceList").getMapping(userPriceListKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (PriceListSupplier a : dataPriceListSuppliers) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(a.item, "I", null)), valueWithPrefix(a.supplier, "L", null),
                            valueWithPrefix(a.userPriceListID, "PL", null), a.currency, a.price, a.inPriceList, a.inPriceList));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemField, legalEntityField, userPriceListField,
                        currencyField, pricePriceListDetailDataPriceListTypeField, inPriceListPriceListTypeField,
                        inPriceListField), data);

                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey,
                        userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
            }
        }
    }

    private void importLegalEntities(List<LegalEntity> legalEntitiesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (legalEntitiesList != null) {
                ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameLegalEntityField = new ImportField(LM.findLCPByCompoundName("nameLegalEntity"));
                ImportField legalAddressField = new ImportField(LM.findLCPByCompoundName("addressLegalEntity"));
                ImportField unpField = new ImportField(LM.findLCPByCompoundName("UNPLegalEntity"));
                ImportField okpoField = new ImportField(LM.findLCPByCompoundName("OKPOLegalEntity"));
                ImportField phoneField = new ImportField(LM.findLCPByCompoundName("dataPhoneLegalEntityDate"));
                ImportField emailField = new ImportField(LM.findLCPByCompoundName("emailLegalEntity"));
                ImportField nameOwnershipField = new ImportField(LM.findLCPByCompoundName("nameOwnership"));
                ImportField shortNameOwnershipField = new ImportField(LM.findLCPByCompoundName("shortNameOwnership"));
                ImportField accountField = new ImportField(LM.findLCPByCompoundName("Bank.numberAccount"));

                ImportField chainStoresIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameChainStoresField = new ImportField(LM.findLCPByCompoundName("nameChainStores"));
                ImportField bankIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("nameCountry"));

                ImportField isSupplierLegalEntityField = new ImportField(LM.findLCPByCompoundName("isSupplierLegalEntity"));
                ImportField isCompanyLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCompanyLegalEntity"));
                ImportField isCustomerLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCustomerLegalEntity"));

                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ownership"),
                        LM.findLCPByCompoundName("shortNameToOwnership").getMapping(shortNameOwnershipField));

                ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank.Account"),
                        LM.findLCPByCompoundName("Bank.accountNumber").getMapping(accountField));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(bankIDField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ChainStores"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(chainStoresIDField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                        LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(legalEntityKey)));
                props.add(new ImportProperty(nameLegalEntityField, LM.findLCPByCompoundName("nameLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(nameLegalEntityField, LM.findLCPByCompoundName("fullNameLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(legalAddressField, LM.findLCPByCompoundName("dataAddressLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
                props.add(new ImportProperty(unpField, LM.findLCPByCompoundName("UNPLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(okpoField, LM.findLCPByCompoundName("OKPOLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(phoneField, LM.findLCPByCompoundName("dataPhoneLegalEntityDate").getMapping(legalEntityKey, defaultDate)));
                props.add(new ImportProperty(emailField, LM.findLCPByCompoundName("emailLegalEntity").getMapping(legalEntityKey)));

                props.add(new ImportProperty(isSupplierLegalEntityField, LM.findLCPByCompoundName("isSupplierLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(isCompanyLegalEntityField, LM.findLCPByCompoundName("isCompanyLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(isCustomerLegalEntityField, LM.findLCPByCompoundName("isCustomerLegalEntity").getMapping(legalEntityKey)));

                props.add(new ImportProperty(nameOwnershipField, LM.findLCPByCompoundName("nameOwnership").getMapping(ownershipKey)));
                props.add(new ImportProperty(shortNameOwnershipField, LM.findLCPByCompoundName("shortNameOwnership").getMapping(ownershipKey)));
                props.add(new ImportProperty(shortNameOwnershipField, LM.findLCPByCompoundName("ownershipLegalEntity").getMapping(legalEntityKey),
                        LM.object(LM.findClassByCompoundName("Ownership")).getMapping(ownershipKey)));

                props.add(new ImportProperty(accountField, LM.findLCPByCompoundName("Bank.numberAccount").getMapping(accountKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("Bank.legalEntityAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(chainStoresIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(chainStoresKey)));
                props.add(new ImportProperty(nameChainStoresField, LM.findLCPByCompoundName("nameChainStores").getMapping(chainStoresKey)));

                props.add(new ImportProperty(bankIDField, LM.findLCPByCompoundName("Bank.bankAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("Bank")).getMapping(bankKey)));

                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("Bank.currencyAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));

                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("nameCountry").getMapping(countryKey)));
                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryLegalEntity").getMapping(legalEntityKey),
                        LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : legalEntitiesList) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(l.legalEntityID, "L", null)), l.nameLegalEntity,
                            l.address, l.unp, l.okpo, l.phone, l.email, l.nameOwnership, l.shortNameOwnership, l.account,
                            valueWithPrefix(l.chainStoresID, "CS", null), l.nameChainStores,
                            valueWithPrefix(l.bankID, "B", null), l.country, l.isSupplierLegalEntity,
                            l.isCompanyLegalEntity, l.isCustomerLegalEntity, "BLR"));
                }

                ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, nameLegalEntityField, legalAddressField,
                        unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                        accountField, chainStoresIDField, nameChainStoresField, bankIDField, nameCountryField,
                        isSupplierLegalEntityField, isCompanyLegalEntityField, isCustomerLegalEntityField,
                        currencyField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, ownershipKey, accountKey, bankKey, chainStoresKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField employeeFirstNameField = new ImportField(LM.findLCPByCompoundName("firstNameContact"));
                ImportField employeeLastNameField = new ImportField(LM.findLCPByCompoundName("lastNameContact"));
                ImportField positionIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField positionNameField = new ImportField(LM.findLCPByCompoundName("namePosition"));

                ImportKey<?> employeeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Employee"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(employeeIDField));

                ImportKey<?> positionKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Position"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(positionIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(employeeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeFirstNameField, LM.findLCPByCompoundName("firstNameContact").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeLastNameField, LM.findLCPByCompoundName("lastNameContact").getMapping(employeeKey)));
                props.add(new ImportProperty(positionIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(positionKey)));
                props.add(new ImportProperty(positionNameField, LM.findLCPByCompoundName("namePosition").getMapping(positionKey)));
                props.add(new ImportProperty(positionIDField, LM.findLCPByCompoundName("positionEmployee").getMapping(employeeKey),
                        LM.object(LM.findClassByCompoundName("Position")).getMapping(positionKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Employee e : employeesList) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(e.employeeID, "LE", null)), e.firstName, e.lastName,
                            valueWithPrefix(e.position, "EP", null), e.position));
                }

                ImportTable table = new ImportTable(Arrays.asList(employeeIDField, employeeFirstNameField,
                        employeeLastNameField, positionIDField, positionNameField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(employeeKey, positionKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameWarehouseGroupField = new ImportField(LM.findLCPByCompoundName("nameWarehouseGroup"));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WarehouseGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseGroupIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(warehouseGroupIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseGroupKey)));
                props.add(new ImportProperty(nameWarehouseGroupField, LM.findLCPByCompoundName("nameWarehouseGroup").getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (WarehouseGroup wg : warehouseGroupsList) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(wg.warehouseGroupID, "WG", null)), wg.name));
                }

                ImportTable table = new ImportTable(Arrays.asList(warehouseGroupIDField, nameWarehouseGroupField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameWarehouseField = new ImportField(LM.findLCPByCompoundName("nameWarehouse"));
                ImportField addressWarehouseField = new ImportField(LM.findLCPByCompoundName("addressWarehouse"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WarehouseGroup"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseGroupIDField));

                ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(warehouseIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseKey)));
                props.add(new ImportProperty(nameWarehouseField, LM.findLCPByCompoundName("nameWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(addressWarehouseField, LM.findLCPByCompoundName("addressWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("legalEntityWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(warehouseGroupIDField, LM.findLCPByCompoundName("warehouseGroupWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("WarehouseGroup")).getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Warehouse w : warehousesList) {
                    data.add(Arrays.asList((Object) (valueWithPrefix(w.legalEntityID, "L", null)),
                            valueWithPrefix(w.warehouseGroupID, "WG", null),
                            valueWithPrefix(w.warehouseID, "WH", null), w.warehouseName, w.warehouseAddress));
                }

                ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, warehouseGroupIDField,
                        warehouseIDField, nameWarehouseField, addressWarehouseField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, warehouseKey, warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameStoreField = new ImportField(LM.findLCPByCompoundName("nameStore"));
                ImportField addressStoreField = new ImportField(LM.findLCPByCompoundName("addressStore"));
                ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField chainStoresIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField storeTypeField = new ImportField(LM.findLCPByCompoundName("nameStoreType"));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Store"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(storeIDField));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ChainStores"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(chainStoresIDField));

                ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("StoreType"),
                        LM.findLCPByCompoundName("storeTypeNameChainStores").getMapping(storeTypeField, chainStoresIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(storeIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(storeKey)));
                props.add(new ImportProperty(nameStoreField, LM.findLCPByCompoundName("nameStore").getMapping(storeKey)));
                props.add(new ImportProperty(addressStoreField, LM.findLCPByCompoundName("addressStore").getMapping(storeKey)));
                props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("legalEntityStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("nameStoreType").getMapping(storeTypeKey)));
                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("storeTypeStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("StoreType")).getMapping(storeTypeKey)));
                props.add(new ImportProperty(chainStoresIDField, LM.findLCPByCompoundName("chainStoresStoreType").getMapping(storeTypeKey),
                        LM.object(LM.findClassByCompoundName("ChainStores")).getMapping(chainStoresKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : storesList) {
                    Store s = (Store) l;
                    data.add(Arrays.asList((Object) valueWithPrefix(s.storeID, "S", null), s.nameLegalEntity, s.address,
                            valueWithPrefix(s.legalEntityID, "L", null), s.storeType,
                            valueWithPrefix(s.chainStoresID, "CS", null)));
                }

                ImportTable table = new ImportTable(Arrays.asList(storeIDField, nameStoreField, addressStoreField, legalEntityIDField, storeTypeField, chainStoresIDField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, legalEntityKey, chainStoresKey, storeTypeKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameDepartmentStoreField = new ImportField(LM.findLCPByCompoundName("nameDepartmentStore"));
                ImportField storeIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(departmentStoreIDField));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Store"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(storeIDField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(departmentStoreIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(nameDepartmentStoreField, LM.findLCPByCompoundName("nameDepartmentStore").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(storeIDField, LM.findLCPByCompoundName("storeDepartmentStore").getMapping(departmentStoreKey),
                        LM.object(LM.findClassByCompoundName("Store")).getMapping(storeKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (DepartmentStore s : departmentStoresList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(s.departmentStoreID, "WH", null), s.name,
                            valueWithPrefix(s.storeID, "S", null)));
                }
                ImportTable table = new ImportTable(Arrays.asList(departmentStoreIDField, nameDepartmentStoreField, storeIDField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameBankField = new ImportField(LM.findLCPByCompoundName("nameBank"));
                ImportField addressBankField = new ImportField(LM.findLCPByCompoundName("dataAddressBankDate"));
                ImportField departmentBankField = new ImportField(LM.findLCPByCompoundName("departmentBank"));
                ImportField mfoBankField = new ImportField(LM.findLCPByCompoundName("MFOBank"));
                ImportField cbuBankField = new ImportField(LM.findLCPByCompoundName("CBUBank"));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(bankIDField));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(bankIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(bankKey)));
                props.add(new ImportProperty(nameBankField, LM.findLCPByCompoundName("nameBank").getMapping(bankKey)));
                props.add(new ImportProperty(addressBankField, LM.findLCPByCompoundName("dataAddressBankDate").getMapping(bankKey, defaultDate)));
                props.add(new ImportProperty(departmentBankField, LM.findLCPByCompoundName("departmentBank").getMapping(bankKey)));
                props.add(new ImportProperty(mfoBankField, LM.findLCPByCompoundName("MFOBank").getMapping(bankKey)));
                props.add(new ImportProperty(cbuBankField, LM.findLCPByCompoundName("CBUBank").getMapping(bankKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Bank b : banksList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(b.bankID, "B", null), b.name, b.address, b.department,
                            b.mfo, b.cbu));
                }
                ImportTable table = new ImportTable(Arrays.asList(bankIDField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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
                ImportField nameWriteOffRateField = new ImportField(LM.findLCPByCompoundName("nameWriteOffRate"));
                ImportField percentWriteOffRateField = new ImportField(LM.findLCPByCompoundName("percentWriteOffRate"));
                ImportField countryWriteOffRateField = new ImportField(LM.findLCPByCompoundName("nameWriteOffRate"));

                ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WriteOffRate"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(writeOffRateIDField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                        LM.findLCPByCompoundName("countryName").getMapping(countryWriteOffRateField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(writeOffRateIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(nameWriteOffRateField, LM.findLCPByCompoundName("nameWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(percentWriteOffRateField, LM.findLCPByCompoundName("percentWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(countryWriteOffRateField, LM.findLCPByCompoundName("countryWriteOffRate").getMapping(writeOffRateKey),
                        LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (RateWaste r : rateWastesList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(r.rateWasteID, "RW", null), r.name, r.coef, r.country));
                }
                ImportTable table = new ImportTable(Arrays.asList(writeOffRateIDField, nameWriteOffRateField, percentWriteOffRateField, countryWriteOffRateField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(writeOffRateKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
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

                ImportKey<?> contractKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserContractSku"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(contractIDField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(supplierIDField));

                ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(customerIDField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(contractIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(contractKey)));
                props.add(new ImportProperty(numberContractField, LM.findLCPByCompoundName("numberContract").getMapping(contractKey)));
                props.add(new ImportProperty(dateFromContractField, LM.findLCPByCompoundName("dateFromContract").getMapping(contractKey)));
                props.add(new ImportProperty(dateToContractField, LM.findLCPByCompoundName("dateToContract").getMapping(contractKey)));
                props.add(new ImportProperty(supplierIDField, LM.findLCPByCompoundName("supplierContractSku").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(customerIDField, LM.findLCPByCompoundName("customerContractSku").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(customerKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyContract").getMapping(contractKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Contract c : contractsList) {
                    data.add(Arrays.asList((Object) valueWithPrefix(c.contractID, "CN", null), c.number, c.dateFrom, c.dateTo,
                            valueWithPrefix(c.supplierID, "L", null), valueWithPrefix(c.customerID, "L", null), c.currency));
                }
                ImportTable table = new ImportTable(Arrays.asList(contractIDField, numberContractField,
                        dateFromContractField, dateToContractField, supplierIDField, customerIDField,
                        currencyField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(contractKey,
                        supplierKey, customerKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String valueWithPrefix(String value, String prefix, String defaultValue) {
        if (value == null)
            return defaultValue;
        else return prefix + value;
    }
}