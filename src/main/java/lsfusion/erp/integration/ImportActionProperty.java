package lsfusion.erp.integration;

import org.xBaseJ.xBaseJException;
import lsfusion.server.classes.*;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

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
                ImportField idItemGroupField = new ImportField(LM.findLCPByCompoundName("idItemGroup"));
                ImportField idParentGroupField = new ImportField(LM.findLCPByCompoundName("idItemGroup"));

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("itemGroupId").getMapping(idItemGroupField));
                ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("itemGroupId").getMapping(idParentGroupField));

                List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
                propsParent.add(new ImportProperty(idParentGroupField, LM.findLCPByCompoundName("parentItemGroup").getMapping(itemGroupKey),
                        LM.object(LM.findClassByCompoundName("ItemGroup")).getMapping(parentGroupKey)));
                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup p : parentGroupsList) {
                    data.add(Arrays.asList((Object) p.sid, p.parent));
                }
                ImportTable table = new ImportTable(Arrays.asList(idItemGroupField, idParentGroupField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importItemGroups(List<ItemGroup> itemGroupsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (itemGroupsList != null) {
                ImportField idItemGroupField = new ImportField(LM.findLCPByCompoundName("idItemGroup"));
                ImportField itemGroupName = new ImportField(LM.findLCPByCompoundName("nameItemGroup"));

                ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                        LM.findLCPByCompoundName("itemGroupId").getMapping(idItemGroupField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
                props.add(new ImportProperty(idItemGroupField, LM.findLCPByCompoundName("idItemGroup").getMapping(itemGroupKey)));
                props.add(new ImportProperty(itemGroupName, LM.findLCPByCompoundName("nameItemGroup").getMapping(itemGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (ItemGroup i : itemGroupsList) {
                    data.add(Arrays.asList((Object) i.sid, i.name));
                }
                ImportTable table = new ImportTable(Arrays.asList(idItemGroupField, itemGroupName), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
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

                ImportField idWareField = new ImportField(LM.findLCPByCompoundName("idWare"));
                ImportField wareNameField = new ImportField(LM.findLCPByCompoundName("nameWare"));
                ImportField warePriceField = new ImportField(LM.findLCPByCompoundName("warePrice"));

                ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ware"),
                        LM.findLCPByCompoundName("wareId").getMapping(idWareField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idWareField, LM.findLCPByCompoundName("idWare").getMapping(wareKey)));
                props.add(new ImportProperty(wareNameField, LM.findLCPByCompoundName("nameWare").getMapping(wareKey)));
                props.add(new ImportProperty(warePriceField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, defaultDate)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Ware w : waresList) {
                    data.add(Arrays.asList((Object) w.idWare, w.name, w.price));
                }
                ImportTable table = new ImportTable(Arrays.asList(idWareField, wareNameField, warePriceField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(wareKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
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

        ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));
        ImportField idItemGroupField = new ImportField(LM.findLCPByCompoundName("idItemGroup"));
        ImportField captionItemField = new ImportField(LM.findLCPByCompoundName("captionItem"));
        ImportField idUOMField = new ImportField(LM.findLCPByCompoundName("idUOM"));
        ImportField nameUOMField = new ImportField(LM.findLCPByCompoundName("nameUOM"));
        ImportField shortNameUOMField = new ImportField(LM.findLCPByCompoundName("shortNameUOM"));
        ImportField idBrandField = new ImportField(LM.findLCPByCompoundName("idBrand"));
        ImportField nameBrandField = new ImportField(LM.findLCPByCompoundName("nameBrand"));
        ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("nameCountry"));
        ImportField idBarcodeField = new ImportField(LM.findLCPByCompoundName("idBarcode"));
        ImportField extIdBarcodeField = new ImportField(LM.findLCPByCompoundName("extIdBarcode"));
        ImportField dateField = new ImportField(DateClass.instance);
        ImportField isWeightItemField = new ImportField(LM.findLCPByCompoundName("isWeightItem"));
        ImportField netWeightItemField = new ImportField(LM.findLCPByCompoundName("netWeightItem"));
        ImportField grossWeightItemField = new ImportField(LM.findLCPByCompoundName("grossWeightItem"));
        ImportField compositionItemField = new ImportField(LM.findLCPByCompoundName("compositionItem"));
        ImportField valueVATItemCountryDateField = new ImportField(LM.findLCPByCompoundName("valueVATItemCountryDate"));
        ImportField idWareField = new ImportField(LM.findLCPByCompoundName("idWare"));
        ImportField priceWareField = new ImportField(LM.findLCPByCompoundName("dataWarePriceDate"));
        ImportField ndsWareField = new ImportField(LM.findLCPByCompoundName("valueRate"));
        ImportField idWriteOffRateField = new ImportField(LM.findLCPByCompoundName("idWriteOffRate"));
        ImportField idRetailCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("idCalcPriceListType"));
        ImportField nameRetailCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("namePriceListType"));
        ImportField retailMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));
        ImportField idBaseCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("idCalcPriceListType"));
        ImportField nameBaseCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("namePriceListType"));
        ImportField baseMarkupCalcPriceListTypeField = new ImportField(LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku"));
        ImportField idBarcodePackField = new ImportField(LM.findLCPByCompoundName("idBarcode"));
        ImportField extIdBarcodePackField = new ImportField(LM.findLCPByCompoundName("extIdBarcode"));
        ImportField amountBarcodePackField = new ImportField(LM.findLCPByCompoundName("amountBarcode"));
        ImportField idManufacturerField = new ImportField(LM.findLCPByCompoundName("idManufacturer"));
        ImportField nameManufacturerField = new ImportField(LM.findLCPByCompoundName("nameManufacturer"));
        ImportField codeCustomsGroupField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));
        ImportField nameCustomsZoneField = new ImportField(LM.findLCPByCompoundName("nameCustomsZone"));

        DataObject defaultCountryObject = (DataObject) LM.findLCPByCompoundName("defaultCountry").readClasses(context.getSession());

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                LM.findLCPByCompoundName("itemId").getMapping(idItemField));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ItemGroup"),
                LM.findLCPByCompoundName("itemGroupId").getMapping(idItemGroupField));

        ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                LM.findLCPByCompoundName("UOMId").getMapping(idUOMField));

        ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Brand"),
                LM.findLCPByCompoundName("brandId").getMapping(idBrandField));

        ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

        ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Barcode"),
                LM.findLCPByCompoundName(/*"barcodeIdDate"*/"extBarcodeId").getMapping(extIdBarcodeField));

        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(valueVATItemCountryDateField));

        ImportKey<?> wareKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ware"),
                LM.findLCPByCompoundName("wareId").getMapping(idWareField));

        ImportKey<?> rangeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(ndsWareField));

        ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WriteOffRate"),
                LM.findLCPByCompoundName("writeOffRateId").getMapping(idWriteOffRateField));

        ImportKey<?> baseCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CalcPriceListType"),
                LM.findLCPByCompoundName("calcPriceListTypeId").getMapping(idBaseCalcPriceListTypeField));

        ImportKey<?> retailCalcPriceListTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CalcPriceListType"),
                LM.findLCPByCompoundName("calcPriceListTypeId").getMapping(idRetailCalcPriceListTypeField));

        ImportKey<?> barcodePackKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Barcode"),
                LM.findLCPByCompoundName(/*"barcodeIdDate"*/"extBarcodeId").getMapping(extIdBarcodePackField));

        ImportKey<?> manufacturerKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Manufacturer"),
                LM.findLCPByCompoundName("manufacturerId").getMapping(idManufacturerField));

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("CustomsGroup"),
                LM.findLCPByCompoundName("customsGroupCode").getMapping(codeCustomsGroupField));

        ImportKey<?> customsZoneKey = new ImportKey((CustomClass) LM.findClassByCompoundName("CustomsZone"),
                LM.findLCPByCompoundName("customsZoneName").getMapping(nameCustomsZoneField));

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

        props.add(new ImportProperty(idItemGroupField, LM.findLCPByCompoundName("itemGroupItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("ItemGroup")).getMapping(itemGroupKey)));

        props.add(new ImportProperty(idItemField, LM.findLCPByCompoundName("idItem").getMapping(itemKey)));
        props.add(new ImportProperty(captionItemField, LM.findLCPByCompoundName("captionItem").getMapping(itemKey)));

        props.add(new ImportProperty(idUOMField, LM.findLCPByCompoundName("idUOM").getMapping(UOMKey)));
        props.add(new ImportProperty(nameUOMField, LM.findLCPByCompoundName("nameUOM").getMapping(UOMKey)));
        props.add(new ImportProperty(shortNameUOMField, LM.findLCPByCompoundName("shortNameUOM").getMapping(UOMKey)));
        props.add(new ImportProperty(idUOMField, LM.findLCPByCompoundName("UOMItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("UOM")).getMapping(UOMKey)));

        props.add(new ImportProperty(nameBrandField, LM.findLCPByCompoundName("nameBrand").getMapping(brandKey)));
        props.add(new ImportProperty(idBrandField, LM.findLCPByCompoundName("idBrand").getMapping(brandKey)));
        props.add(new ImportProperty(idBrandField, LM.findLCPByCompoundName("brandItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Brand")).getMapping(brandKey)));

        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("nameCountry").getMapping(countryKey)));
        props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

        props.add(new ImportProperty(extIdBarcodeField, LM.findLCPByCompoundName("extIdBarcode").getMapping(barcodeKey)));
        props.add(new ImportProperty(idBarcodeField, LM.findLCPByCompoundName("idBarcode").getMapping(barcodeKey)));
        props.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dataDateBarcode").getMapping(barcodeKey)));

        props.add(new ImportProperty(idItemField, LM.findLCPByCompoundName("skuBarcode").getMapping(barcodeKey),
                LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));

        props.add(new ImportProperty(isWeightItemField, LM.findLCPByCompoundName("isWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(netWeightItemField, LM.findLCPByCompoundName("netWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(grossWeightItemField, LM.findLCPByCompoundName("grossWeightItem").getMapping(itemKey)));
        props.add(new ImportProperty(compositionItemField, LM.findLCPByCompoundName("compositionItem").getMapping(itemKey)));
        props.add(new ImportProperty(valueVATItemCountryDateField, LM.findLCPByCompoundName("dataVATItemCountryDate").getMapping(itemKey, defaultCountryObject, dateField),
                LM.object(LM.findClassByCompoundName("Range")).getMapping(VATKey)));

        props.add(new ImportProperty(idWareField, LM.findLCPByCompoundName("idWare").getMapping(wareKey)));
        props.add(new ImportProperty(idWareField, LM.findLCPByCompoundName("wareItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Ware")).getMapping(wareKey)));

        props.add(new ImportProperty(priceWareField, LM.findLCPByCompoundName("dataWarePriceDate").getMapping(wareKey, dateField)));
        props.add(new ImportProperty(ndsWareField, LM.findLCPByCompoundName("dataRangeWareDate").getMapping(wareKey, dateField, rangeKey),
                LM.object(LM.findClassByCompoundName("Range")).getMapping(rangeKey)));

        props.add(new ImportProperty(idWriteOffRateField, LM.findLCPByCompoundName("writeOffRateCountryItem").getMapping(defaultCountryObject, itemKey),
                LM.object(LM.findClassByCompoundName("WriteOffRate")).getMapping(writeOffRateKey)));

        props.add(new ImportProperty(idRetailCalcPriceListTypeField, LM.findLCPByCompoundName("idCalcPriceListType").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(nameRetailCalcPriceListTypeField, LM.findLCPByCompoundName("namePriceListType").getMapping(retailCalcPriceListTypeKey)));
        props.add(new ImportProperty(retailMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(retailCalcPriceListTypeKey, itemKey)));
        props.add(new ImportProperty(idBaseCalcPriceListTypeField, LM.findLCPByCompoundName("idCalcPriceListType").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(nameBaseCalcPriceListTypeField, LM.findLCPByCompoundName("namePriceListType").getMapping(baseCalcPriceListTypeKey)));
        props.add(new ImportProperty(baseMarkupCalcPriceListTypeField, LM.findLCPByCompoundName("dataMarkupCalcPriceListTypeSku").getMapping(baseCalcPriceListTypeKey, itemKey)));

        props.add(new ImportProperty(extIdBarcodePackField, LM.findLCPByCompoundName("extIdBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(idBarcodePackField, LM.findLCPByCompoundName("idBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dataDateBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(amountBarcodePackField, LM.findLCPByCompoundName("amountBarcode").getMapping(barcodePackKey)));
        props.add(new ImportProperty(extIdBarcodePackField, LM.findLCPByCompoundName("Purchase.packBarcodeSku").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Barcode")).getMapping(barcodePackKey)));
        props.add(new ImportProperty(extIdBarcodePackField, LM.findLCPByCompoundName("Sale.packBarcodeSku").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Barcode")).getMapping(barcodePackKey)));
        props.add(new ImportProperty(idItemField, LM.findLCPByCompoundName("skuBarcode").getMapping(barcodePackKey),
                LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));

        props.add(new ImportProperty(idManufacturerField, LM.findLCPByCompoundName("idManufacturer").getMapping(manufacturerKey)));
        props.add(new ImportProperty(nameManufacturerField, LM.findLCPByCompoundName("nameManufacturer").getMapping(manufacturerKey)));
        props.add(new ImportProperty(idManufacturerField, LM.findLCPByCompoundName("manufacturerItem").getMapping(itemKey),
                LM.object(LM.findClassByCompoundName("Manufacturer")).getMapping(manufacturerKey)));

        props.add(new ImportProperty(codeCustomsGroupField, LM.findLCPByCompoundName("codeCustomsGroup").getMapping(customsGroupKey)));
        props.add(new ImportProperty(nameCustomsZoneField, LM.findLCPByCompoundName("nameCustomsZone").getMapping(customsZoneKey)));
        props.add(new ImportProperty(nameCustomsZoneField, LM.findLCPByCompoundName("customsZoneCustomsGroup").getMapping(customsGroupKey),
                LM.object(LM.findClassByCompoundName("CustomsZone")).getMapping(customsZoneKey)));
        props.add(new ImportProperty(codeCustomsGroupField, LM.findLCPByCompoundName("customsGroupCountryItem").getMapping(countryKey, itemKey),
                LM.object(LM.findClassByCompoundName("CustomsGroup")).getMapping(customsGroupKey)));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (Item i : dataItems) {
            data.add(Arrays.asList((Object) i.idItem, i.itemGroupId, i.nameItem, i.nameUOM, i.shortNameUOM, i.idUOM,
                    i.brandName, i.idBrand, i.country, i.barcode, i.idBarcode, i.date, i.isWeightItem, i.netWeightItem,
                    i.grossWeightItem, i.compositionItem, i.retailVAT, i.idWare, i.priceWare, i.wareVAT, i.idWriteOffRate,
                    "retail", "Розничная надбавка", i.retailMarkup, "wholesale", "Оптовая надбавка",
                    i.baseMarkup, null, valueWithPrefix(i.idBarcodePack, "P", null), i.amountPack,
                    i.idManufacturer, i.nameManufacturer, i.codeCustomsGroup, i.codeCustomsZone));
        }

        ImportTable table = new ImportTable(Arrays.asList(idItemField, idItemGroupField, captionItemField, nameUOMField,
                shortNameUOMField, idUOMField, nameBrandField, idBrandField, nameCountryField, idBarcodeField,
                extIdBarcodeField, dateField, isWeightItemField, netWeightItemField, grossWeightItemField,
                compositionItemField, valueVATItemCountryDateField, idWareField, priceWareField, ndsWareField,
                idWriteOffRateField, idRetailCalcPriceListTypeField, nameRetailCalcPriceListTypeField,
                retailMarkupCalcPriceListTypeField, idBaseCalcPriceListTypeField, nameBaseCalcPriceListTypeField,
                baseMarkupCalcPriceListTypeField, idBarcodePackField, extIdBarcodePackField, amountBarcodePackField,
                idManufacturerField, nameManufacturerField, codeCustomsGroupField, nameCustomsZoneField), data);

        DataSession session = context.createSession();
        session.sql.pushVolatileStats(null);
        IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, UOMKey,
                brandKey, countryKey, barcodeKey, VATKey, wareKey, rangeKey, writeOffRateKey, retailCalcPriceListTypeKey,
                baseCalcPriceListTypeKey, barcodePackKey, manufacturerKey, customsGroupKey, customsZoneKey), props);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.sql.popVolatileStats(null);
        session.close();
    }

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

                ImportField idUserInvoiceField = new ImportField(LM.findLCPByCompoundName("idUserInvoice"));
                ImportField idCustomerStockField = new ImportField(LM.findLCPByCompoundName("idStock"));
                ImportField idSupplierField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idSupplierWarehouseField = new ImportField(LM.findLCPByCompoundName("idWarehouse"));

                ImportField numberUserInvoiceField = new ImportField(LM.findLCPByCompoundName("numberObject"));
                ImportField seriesUserInvoiceField = new ImportField(LM.findLCPByCompoundName("seriesObject"));
                ImportField createPricingUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.createPricingUserInvoice"));
                ImportField createShipmentUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.createShipmentUserInvoice"));
                ImportField showManufacturingPriceUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.showManufacturingPriceUserInvoice"));
                ImportField showWholesalePriceUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.showWholesalePriceUserInvoice"));
                ImportField dateUserInvoiceField = new ImportField(LM.findLCPByCompoundName("Purchase.dateUserInvoice"));
                ImportField timeUserInvoiceField = new ImportField(TimeClass.instance);

                ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));
                ImportField idUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("idUserInvoiceDetail"));
                ImportField quantityUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail"));
                ImportField priceUserInvoiceDetail = new ImportField(LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail"));
                ImportField expiryDateUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("expiryDateUserInvoiceDetail"));
                ImportField dataRateExchangeUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("dataRateExchangeUserInvoiceDetail"));
                ImportField homePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("homePriceUserInvoiceDetail"));
                ImportField priceDutyUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("priceDutyUserInvoiceDetail"));
                ImportField binUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("idBin"));
                ImportField chargePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail"));
                ImportField manufacturingPriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.manufacturingPriceUserInvoiceDetail"));
                ImportField wholesalePriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.wholesalePriceUserInvoiceDetail"));
                ImportField wholesaleMarkupUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.wholesaleMarkupUserInvoiceDetail"));
                ImportField retailPriceUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail"));
                ImportField retailMarkupUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail"));
                ImportField certificateTextUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("certificateTextUserInvoiceDetail"));
                ImportField skipCreateWareUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("skipCreateWareUserInvoiceDetail"));
                ImportField userContractSkuField = new ImportField(LM.findLCPByCompoundName("idUserContractSku"));

                ImportField numberDeclarationField = new ImportField(LM.findLCPByCompoundName("numberObject"));
                ImportField dateDeclarationField = new ImportField(LM.findLCPByCompoundName("dateDeclaration"));

                ImportField numberComplianceField = new ImportField(LM.findLCPByCompoundName("numberObject"));
                ImportField fromDateComplianceField = new ImportField(LM.findLCPByCompoundName("dateDeclaration"));
                ImportField toDateComplianceField = new ImportField(LM.findLCPByCompoundName("dateDeclaration"));

                ImportField isHomeCurrencyUserInvoiceField = new ImportField(LM.findLCPByCompoundName("isHomeCurrencyUserInvoice"));
                ImportField showDeclarationUserInvoiceField = new ImportField(LM.findLCPByCompoundName("showDeclarationUserInvoice"));

                ImportField shortNameCurrencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                ImportField codeCustomsGroupField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));
                ImportField valueVATUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.valueVATUserInvoiceDetail"));

                ImportKey<?> userInvoiceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName(posted ? "Purchase.UserInvoicePosted" : "Purchase.UserInvoice"),
                        LM.findLCPByCompoundName("userInvoiceId").getMapping(idUserInvoiceField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idSupplierField));

                ImportKey<?> customerStockKey = new ImportKey((CustomClass) LM.findClassByCompoundName("Stock"),
                        LM.findLCPByCompoundName("stockId").getMapping(idCustomerStockField));

                ImportKey<?> supplierWarehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                        LM.findLCPByCompoundName("warehouseId").getMapping(idSupplierWarehouseField));

                ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.UserInvoiceDetail"),
                        LM.findLCPByCompoundName("userInvoiceDetailId").getMapping(idUserInvoiceDetailField));

                ImportKey<?> itemKey = new ImportKey((CustomClass) LM.findClassByCompoundName("Sku"),
                        LM.findLCPByCompoundName("itemId").getMapping(idItemField));

                ImportKey<?> userContractSkuKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserContractSku"),
                        LM.findLCPByCompoundName("userContractSkuId").getMapping(userContractSkuField));

                ImportKey<?> declarationKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Declaration"),
                        LM.findLCPByCompoundName("declarationId").getMapping(numberDeclarationField));

                ImportKey<?> complianceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Compliance"),
                        LM.findLCPByCompoundName("complianceId").getMapping(numberComplianceField));

                ImportKey<?> binKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bin"),
                        LM.findLCPByCompoundName("binId").getMapping(binUserInvoiceDetailField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(shortNameCurrencyField));

                ImportKey<?> customsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("CustomsGroup"),
                        LM.findLCPByCompoundName("customsGroupCode").getMapping(codeCustomsGroupField));

                ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                        LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(valueVATUserInvoiceDetailField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(numberUserInvoiceField, LM.findLCPByCompoundName("numberObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(seriesUserInvoiceField, LM.findLCPByCompoundName("seriesObject").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(idUserInvoiceField, LM.findLCPByCompoundName("idUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(createShipmentUserInvoiceField, LM.findLCPByCompoundName("Purchase.createShipmentUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(dateUserInvoiceField, LM.findLCPByCompoundName("Purchase.dateUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(timeUserInvoiceField, LM.findLCPByCompoundName("Purchase.timeUserInvoice").getMapping(userInvoiceKey)));

                props.add(new ImportProperty(idSupplierField, LM.findLCPByCompoundName("Purchase.supplierUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(idSupplierWarehouseField, LM.findLCPByCompoundName("Purchase.supplierStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Stock")).getMapping(supplierWarehouseKey)));

                props.add(new ImportProperty(idCustomerStockField, LM.findLCPByCompoundName("Purchase.customerUserInvoice").getMapping(userInvoiceKey),
                        LM.findLCPByCompoundName("legalEntityStock").getMapping(customerStockKey)));
                props.add(new ImportProperty(idCustomerStockField, LM.findLCPByCompoundName("Purchase.customerStockUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Stock")).getMapping(customerStockKey)));

                props.add(new ImportProperty(idUserInvoiceDetailField, LM.findLCPByCompoundName("idUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(quantityUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.quantityUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceUserInvoiceDetail, LM.findLCPByCompoundName("Purchase.priceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(expiryDateUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.expiryDateUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(dataRateExchangeUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.dataRateExchangeUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(homePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.homePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceDutyUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.priceDutyUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(showManufacturingPriceUserInvoiceField, LM.findLCPByCompoundName("Purchase.showManufacturingPriceUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(manufacturingPriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.manufacturingPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(chargePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.chargePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(showWholesalePriceUserInvoiceField, LM.findLCPByCompoundName("Purchase.showWholesalePriceUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(wholesalePriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.wholesalePriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(wholesaleMarkupUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.wholesaleMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                // розничная расценка
                props.add(new ImportProperty(createPricingUserInvoiceField, LM.findLCPByCompoundName("Purchase.createPricingUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(retailPriceUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailPriceUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(retailMarkupUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.retailMarkupUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(idItemField, LM.findLCPByCompoundName("Purchase.skuInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Sku")).getMapping(itemKey)));

                props.add(new ImportProperty(idUserInvoiceField, LM.findLCPByCompoundName("Purchase.userInvoiceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName(posted ? "Purchase.UserInvoicePosted" : "Purchase.UserInvoice")).getMapping(userInvoiceKey)));

                props.add(new ImportProperty(certificateTextUserInvoiceDetailField, LM.findLCPByCompoundName("certificateTextUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(skipCreateWareUserInvoiceDetailField, LM.findLCPByCompoundName("skipCreateWareUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                props.add(new ImportProperty(userContractSkuField, LM.findLCPByCompoundName("Purchase.contractSkuInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Contract")).getMapping(userContractSkuKey)));

                props.add(new ImportProperty(numberDeclarationField, LM.findLCPByCompoundName("numberObject").getMapping(declarationKey)));
                props.add(new ImportProperty(numberDeclarationField, LM.findLCPByCompoundName("idDeclaration").getMapping(declarationKey)));
                props.add(new ImportProperty(dateDeclarationField, LM.findLCPByCompoundName("dateDeclaration").getMapping(declarationKey)));
                props.add(new ImportProperty(numberDeclarationField, LM.findLCPByCompoundName("declarationUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Declaration")).getMapping(declarationKey)));

                props.add(new ImportProperty(numberComplianceField, LM.findLCPByCompoundName("numberObject").getMapping(complianceKey)));
                props.add(new ImportProperty(numberComplianceField, LM.findLCPByCompoundName("idCompliance").getMapping(complianceKey)));
                props.add(new ImportProperty(fromDateComplianceField, LM.findLCPByCompoundName("dateCompliance").getMapping(complianceKey)));
                props.add(new ImportProperty(fromDateComplianceField, LM.findLCPByCompoundName("fromDateCompliance").getMapping(complianceKey)));
                props.add(new ImportProperty(toDateComplianceField, LM.findLCPByCompoundName("toDateCompliance").getMapping(complianceKey)));
                props.add(new ImportProperty(numberComplianceField, LM.findLCPByCompoundName("complianceUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Compliance")).getMapping(complianceKey)));

                props.add(new ImportProperty(binUserInvoiceDetailField, LM.findLCPByCompoundName("idBin").getMapping(binKey)));
                props.add(new ImportProperty(binUserInvoiceDetailField, LM.findLCPByCompoundName("nameBin").getMapping(binKey)));
                props.add(new ImportProperty(binUserInvoiceDetailField, LM.findLCPByCompoundName("binUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Bin")).getMapping(binKey)));

                props.add(new ImportProperty(isHomeCurrencyUserInvoiceField, LM.findLCPByCompoundName("isHomeCurrencyUserInvoice").getMapping(userInvoiceKey)));
                props.add(new ImportProperty(showDeclarationUserInvoiceField, LM.findLCPByCompoundName("showDeclarationUserInvoice").getMapping(userInvoiceKey)));

                props.add(new ImportProperty(shortNameCurrencyField, LM.findLCPByCompoundName("Purchase.currencyUserInvoice").getMapping(userInvoiceKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));

                props.add(new ImportProperty(codeCustomsGroupField, LM.findLCPByCompoundName("customsGroupUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("CustomsGroup")).getMapping(customsGroupKey)));

                props.add(new ImportProperty(valueVATUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.VATUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClassByCompoundName("Range")).getMapping(VATKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (UserInvoiceDetail u : dataUserInvoiceDetail) {
                    data.add(Arrays.asList((Object) u.idUserInvoice, u.series, u.number, u.createPricing, u.createShipment,
                            u.showManufacturingPrice, showWholesalePrice, u.isHomeCurrency, u.showDeclaration,
                            u.date, new Time(12, 0, 0), u.sid, u.idItem, u.quantity, u.supplier, u.customerWarehouse,
                            u.supplierWarehouse, u.price, u.chargePrice, u.manufacturingPrice,
                            u.wholesalePrice, u.rateExchange, u.homePrice, u.priceDuty, u.wholesaleMarkup, u.retailPrice,
                            u.retailMarkup, u.certificateText, true, u.idContract, u.numberDeclaration, u.dateDeclaration,
                            u.numberCompliance, u.fromDateCompliance, u.toDateCompliance, u.expiryDate, u.bin,
                            u.shortNameCurrency, u.codeCustomsGroup, u.retailVAT));
                }
                ImportTable table = new ImportTable(Arrays.asList(idUserInvoiceField, seriesUserInvoiceField,
                        numberUserInvoiceField, createPricingUserInvoiceField, createShipmentUserInvoiceField,
                        showManufacturingPriceUserInvoiceField, showWholesalePriceUserInvoiceField,
                        isHomeCurrencyUserInvoiceField, showDeclarationUserInvoiceField, dateUserInvoiceField,
                        timeUserInvoiceField, idUserInvoiceDetailField, idItemField, quantityUserInvoiceDetailField,
                        idSupplierField, idCustomerStockField, idSupplierWarehouseField, priceUserInvoiceDetail,
                        chargePriceUserInvoiceDetailField, manufacturingPriceUserInvoiceDetailField, wholesalePriceUserInvoiceDetailField,
                        dataRateExchangeUserInvoiceDetailField, homePriceUserInvoiceDetailField,
                        priceDutyUserInvoiceDetailField, wholesaleMarkupUserInvoiceDetailField,
                        retailPriceUserInvoiceDetailField, retailMarkupUserInvoiceDetailField,
                        certificateTextUserInvoiceDetailField, skipCreateWareUserInvoiceDetailField,
                        userContractSkuField, numberDeclarationField, dateDeclarationField, numberComplianceField,
                        fromDateComplianceField, toDateComplianceField, expiryDateUserInvoiceDetailField,
                        binUserInvoiceDetailField, shortNameCurrencyField, codeCustomsGroupField, valueVATUserInvoiceDetailField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userInvoiceKey, userInvoiceDetailKey,
                        itemKey, supplierKey, customerStockKey, userContractSkuKey, declarationKey, complianceKey, binKey,
                        currencyKey, customsGroupKey, VATKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
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
                session.sql.pushVolatileStats(null);

                ObjectValue dataPriceListTypeObject = LM.findLCPByCompoundName("dataPriceListTypeId").readClasses(session, new DataObject("Coordinated", StringClass.get(100)));
                if (dataPriceListTypeObject instanceof NullValue) {
                    dataPriceListTypeObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("DataPriceListType"));
                    Object defaultCurrency = LM.findLCPByCompoundName("currencyShortName").read(session, new DataObject("BLR", StringClass.get(3)));
                    LM.findLCPByCompoundName("namePriceListType").change("Поставщика (согласованная)", session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("currencyDataPriceListType").change(defaultCurrency, session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("idDataPriceListType").change("Coordinated", session, (DataObject) dataPriceListTypeObject);
                }

                ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));
                ImportField idLegalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idUserPriceListField = new ImportField(LM.findLCPByCompoundName("idUserPriceList"));
                ImportField idDepartmentStoreField = new ImportField(LM.findLCPByCompoundName("idDepartmentStore"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(LM.findLCPByCompoundName("pricePriceListDetailDataPriceListType"));
                ImportField inPriceListPriceListTypeField = new ImportField(LM.findLCPByCompoundName("inPriceListDataPriceListType"));
                ImportField inPriceListStockField = new ImportField(LM.findLCPByCompoundName("inPriceListStock"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idLegalEntityField));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("departmentStoreId").getMapping(idDepartmentStoreField));

                ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceList"),
                        LM.findLCPByCompoundName("userPriceListId").getMapping(idUserPriceListField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("itemId").getMapping(idItemField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceListDetail"),
                        LM.findLCPByCompoundName("userPriceListDetailIdSkuIdUserPriceList").getMapping(idItemField, idUserPriceListField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idUserPriceListField, LM.findLCPByCompoundName("idUserPriceList").getMapping(userPriceListKey)));
                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("companyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(idItemField, LM.findLCPByCompoundName("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));
                props.add(new ImportProperty(idUserPriceListField, LM.findLCPByCompoundName("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("UserPriceList")).getMapping(userPriceListKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
                props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, LM.findLCPByCompoundName("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListPriceListTypeField, LM.findLCPByCompoundName("inPriceListDataPriceListType").getMapping(userPriceListKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListStockField, LM.findLCPByCompoundName("inPriceListStock").getMapping(userPriceListKey, departmentStoreKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (PriceListStore a : dataPriceListStores) {
                    data.add(Arrays.asList((Object) a.item, a.supplier,a.idUserPriceList, a.departmentStore,
                            a.currency, a.price, a.inPriceList, a.inPriceListStock));
                }
                ImportTable table = new ImportTable(Arrays.asList(idItemField, idLegalEntityField, idUserPriceListField,
                        idDepartmentStoreField, currencyField, pricePriceListDetailDataPriceListTypeField,
                        inPriceListPriceListTypeField, inPriceListStockField), data);

                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey,
                        departmentStoreKey, userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
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
                session.sql.pushVolatileStats(null);

                ObjectValue dataPriceListTypeObject = LM.findLCPByCompoundName("dataPriceListTypeId").readClasses(session, new DataObject("Offered", StringClass.get(100)));
                if (dataPriceListTypeObject instanceof NullValue) {
                    dataPriceListTypeObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("DataPriceListType"));
                    Object defaultCurrency = LM.findLCPByCompoundName("currencyShortName").read(session, new DataObject("BLR", StringClass.get(3)));
                    LM.findLCPByCompoundName("namePriceListType").change("Поставщика (предлагаемая)", session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("currencyDataPriceListType").change(defaultCurrency, session, (DataObject) dataPriceListTypeObject);
                    LM.findLCPByCompoundName("idDataPriceListType").change("Offered", session, (DataObject) dataPriceListTypeObject);
                }

                ImportField itemField = new ImportField(LM.findLCPByCompoundName("idItem"));
                ImportField legalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idUserPriceListField = new ImportField(LM.findLCPByCompoundName("idUserPriceList"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
                ImportField pricePriceListDetailDataPriceListTypeField = new ImportField(LM.findLCPByCompoundName("pricePriceListDetailDataPriceListType"));
                ImportField inPriceListPriceListTypeField = new ImportField(LM.findLCPByCompoundName("inPriceListDataPriceListType"));
                ImportField allStocksUserPriceListField = new ImportField(LM.findLCPByCompoundName("allStocksUserPriceList"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(legalEntityField));

                ImportKey<?> userPriceListKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceList"),
                        LM.findLCPByCompoundName("userPriceListId").getMapping(idUserPriceListField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("itemId").getMapping(itemField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                ImportKey<?> userPriceListDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserPriceListDetail"),
                        LM.findLCPByCompoundName("userPriceListDetailIdSkuIdUserPriceList").getMapping(itemField, idUserPriceListField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idUserPriceListField, LM.findLCPByCompoundName("idUserPriceList").getMapping(userPriceListKey)));
                props.add(new ImportProperty(legalEntityField, LM.findLCPByCompoundName("companyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(itemField, LM.findLCPByCompoundName("skuUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("Item")).getMapping(itemKey)));
                props.add(new ImportProperty(idUserPriceListField, LM.findLCPByCompoundName("userPriceListUserPriceListDetail").getMapping(userPriceListDetailKey),
                        LM.object(LM.findClassByCompoundName("UserPriceList")).getMapping(userPriceListKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyUserPriceList").getMapping(userPriceListKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
                props.add(new ImportProperty(pricePriceListDetailDataPriceListTypeField, LM.findLCPByCompoundName("priceUserPriceListDetailDataPriceListType").getMapping(userPriceListDetailKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(inPriceListPriceListTypeField, LM.findLCPByCompoundName("inPriceListDataPriceListType").getMapping(userPriceListKey, dataPriceListTypeObject)));
                props.add(new ImportProperty(allStocksUserPriceListField, LM.findLCPByCompoundName("allStocksUserPriceList").getMapping(userPriceListKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (PriceListSupplier a : dataPriceListSuppliers) {
                    data.add(Arrays.asList((Object) a.item, a.supplier, a.idUserPriceList, a.currency, a.price,
                            a.inPriceList, a.inPriceList));
                }
                ImportTable table = new ImportTable(Arrays.asList(itemField, legalEntityField, idUserPriceListField,
                        currencyField, pricePriceListDetailDataPriceListTypeField, inPriceListPriceListTypeField,
                        allStocksUserPriceListField), data);

                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userPriceListKey,
                        userPriceListDetailKey, itemKey, legalEntityKey, currencyKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        }
    }

    private void importLegalEntities(List<LegalEntity> legalEntitiesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (legalEntitiesList != null) {
                ImportField idLegalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField nameLegalEntityField = new ImportField(LM.findLCPByCompoundName("nameLegalEntity"));
                ImportField legalAddressField = new ImportField(LM.findLCPByCompoundName("addressLegalEntity"));
                ImportField unpField = new ImportField(LM.findLCPByCompoundName("UNPLegalEntity"));
                ImportField okpoField = new ImportField(LM.findLCPByCompoundName("OKPOLegalEntity"));
                ImportField phoneField = new ImportField(LM.findLCPByCompoundName("dataPhoneLegalEntityDate"));
                ImportField emailField = new ImportField(LM.findLCPByCompoundName("emailLegalEntity"));
                ImportField nameOwnershipField = new ImportField(LM.findLCPByCompoundName("nameOwnership"));
                ImportField shortNameOwnershipField = new ImportField(LM.findLCPByCompoundName("shortNameOwnership"));
                ImportField accountField = new ImportField(LM.findLCPByCompoundName("Bank.numberAccount"));

                ImportField idChainStoresField = new ImportField(LM.findLCPByCompoundName("idChainStores"));
                ImportField nameChainStoresField = new ImportField(LM.findLCPByCompoundName("nameChainStores"));
                ImportField idBankField = new ImportField(LM.findLCPByCompoundName("idBank"));
                ImportField nameCountryField = new ImportField(LM.findLCPByCompoundName("nameCountry"));

                ImportField isSupplierLegalEntityField = new ImportField(LM.findLCPByCompoundName("isSupplierLegalEntity"));
                ImportField isCompanyLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCompanyLegalEntity"));
                ImportField isCustomerLegalEntityField = new ImportField(LM.findLCPByCompoundName("isCustomerLegalEntity"));

                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idLegalEntityField));

                ImportKey<?> ownershipKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Ownership"),
                        LM.findLCPByCompoundName("shortNameToOwnership").getMapping(shortNameOwnershipField));

                ImportKey<?> accountKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank.Account"),
                        LM.findLCPByCompoundName("Bank.accountNumber").getMapping(accountField));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank"),
                        LM.findLCPByCompoundName("bankId").getMapping(idBankField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ChainStores"),
                        LM.findLCPByCompoundName("chainStoresId").getMapping(idChainStoresField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                        LM.findLCPByCompoundName("countryName").getMapping(nameCountryField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("idLegalEntity").getMapping(legalEntityKey)));
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
                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("Bank.legalEntityAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(idChainStoresField, LM.findLCPByCompoundName("idChainStores").getMapping(chainStoresKey)));
                props.add(new ImportProperty(nameChainStoresField, LM.findLCPByCompoundName("nameChainStores").getMapping(chainStoresKey)));

                props.add(new ImportProperty(idBankField, LM.findLCPByCompoundName("Bank.bankAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("Bank")).getMapping(bankKey)));

                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("Bank.currencyAccount").getMapping(accountKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));

                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("nameCountry").getMapping(countryKey)));
                props.add(new ImportProperty(nameCountryField, LM.findLCPByCompoundName("countryLegalEntity").getMapping(legalEntityKey),
                        LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : legalEntitiesList) {
                    data.add(Arrays.asList((Object) l.idLegalEntity, l.nameLegalEntity,
                            l.address, l.unp, l.okpo, l.phone, l.email, l.nameOwnership, l.shortNameOwnership, l.account,
                            l.idChainStores, l.nameChainStores, l.idBank, l.country,
                            l.isSupplierLegalEntity, l.isCompanyLegalEntity, l.isCustomerLegalEntity, "BLR"));
                }

                ImportTable table = new ImportTable(Arrays.asList(idLegalEntityField, nameLegalEntityField, legalAddressField,
                        unpField, okpoField, phoneField, emailField, nameOwnershipField, shortNameOwnershipField,
                        accountField, idChainStoresField, nameChainStoresField, idBankField, nameCountryField,
                        isSupplierLegalEntityField, isCompanyLegalEntityField, isCustomerLegalEntityField,
                        currencyField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, ownershipKey, accountKey, bankKey, chainStoresKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importEmployees(List<Employee> employeesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (employeesList != null) {
                ImportField idEmployeeField = new ImportField(LM.findLCPByCompoundName("idEmployee"));
                ImportField employeeFirstNameField = new ImportField(LM.findLCPByCompoundName("firstNameContact"));
                ImportField employeeLastNameField = new ImportField(LM.findLCPByCompoundName("lastNameContact"));
                ImportField idPositionField = new ImportField(LM.findLCPByCompoundName("idPosition"));
                ImportField namePositionField = new ImportField(LM.findLCPByCompoundName("namePosition"));

                ImportKey<?> employeeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Employee"),
                        LM.findLCPByCompoundName("employeeId").getMapping(idEmployeeField));

                ImportKey<?> positionKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Position"),
                        LM.findLCPByCompoundName("positionId").getMapping(idPositionField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idEmployeeField, LM.findLCPByCompoundName("idEmployee").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeFirstNameField, LM.findLCPByCompoundName("firstNameContact").getMapping(employeeKey)));
                props.add(new ImportProperty(employeeLastNameField, LM.findLCPByCompoundName("lastNameContact").getMapping(employeeKey)));
                props.add(new ImportProperty(idPositionField, LM.findLCPByCompoundName("idPosition").getMapping(positionKey)));
                props.add(new ImportProperty(namePositionField, LM.findLCPByCompoundName("namePosition").getMapping(positionKey)));
                props.add(new ImportProperty(idPositionField, LM.findLCPByCompoundName("positionEmployee").getMapping(employeeKey),
                        LM.object(LM.findClassByCompoundName("Position")).getMapping(positionKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Employee e : employeesList) {
                    data.add(Arrays.asList((Object) e.idEmployee, e.firstName, e.lastName, e.position, e.position));
                }

                ImportTable table = new ImportTable(Arrays.asList(idEmployeeField, employeeFirstNameField,
                        employeeLastNameField, idPositionField, namePositionField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(employeeKey, positionKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWarehouseGroups(List<WarehouseGroup> warehouseGroupsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (warehouseGroupsList != null) {
                ImportField idWarehouseGroupField = new ImportField(LM.findLCPByCompoundName("idWarehouseGroup"));
                ImportField nameWarehouseGroupField = new ImportField(LM.findLCPByCompoundName("nameWarehouseGroup"));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WarehouseGroup"),
                        LM.findLCPByCompoundName("warehouseGroupId").getMapping(idWarehouseGroupField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idWarehouseGroupField, LM.findLCPByCompoundName("idWarehouseGroup").getMapping(warehouseGroupKey)));
                props.add(new ImportProperty(nameWarehouseGroupField, LM.findLCPByCompoundName("nameWarehouseGroup").getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (WarehouseGroup wg : warehouseGroupsList) {
                    data.add(Arrays.asList((Object) wg.idWarehouseGroup, wg.name));
                }

                ImportTable table = new ImportTable(Arrays.asList(idWarehouseGroupField, nameWarehouseGroupField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWarehouses(List<Warehouse> warehousesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (warehousesList != null) {
                ImportField idLegalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idWarehouseGroupField = new ImportField(LM.findLCPByCompoundName("idWarehouseGroup"));
                ImportField idWarehouseField = new ImportField(LM.findLCPByCompoundName("idWarehouse"));
                ImportField nameWarehouseField = new ImportField(LM.findLCPByCompoundName("nameWarehouse"));
                ImportField addressWarehouseField = new ImportField(LM.findLCPByCompoundName("addressWarehouse"));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idLegalEntityField));

                ImportKey<?> warehouseGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WarehouseGroup"),
                        LM.findLCPByCompoundName("warehouseGroupId").getMapping(idWarehouseGroupField));

                ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                        LM.findLCPByCompoundName("warehouseId").getMapping(idWarehouseField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idWarehouseField, LM.findLCPByCompoundName("idWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("idLegalEntity").getMapping(legalEntityKey)));
                props.add(new ImportProperty(nameWarehouseField, LM.findLCPByCompoundName("nameWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(addressWarehouseField, LM.findLCPByCompoundName("addressWarehouse").getMapping(warehouseKey)));
                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("legalEntityWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));
                props.add(new ImportProperty(idWarehouseGroupField, LM.findLCPByCompoundName("warehouseGroupWarehouse").getMapping(warehouseKey),
                        LM.object(LM.findClassByCompoundName("WarehouseGroup")).getMapping(warehouseGroupKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Warehouse w : warehousesList) {
                    data.add(Arrays.asList((Object) w.idLegalEntity, w.idWarehouseGroup, w.idWarehouse,
                            w.warehouseName, w.warehouseAddress));
                }

                ImportTable table = new ImportTable(Arrays.asList(idLegalEntityField, idWarehouseGroupField,
                        idWarehouseField, nameWarehouseField, addressWarehouseField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(legalEntityKey, warehouseKey, warehouseGroupKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importStores(List<LegalEntity> storesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (storesList != null) {
                ImportField idStoreField = new ImportField(LM.findLCPByCompoundName("idStore"));
                ImportField nameStoreField = new ImportField(LM.findLCPByCompoundName("nameStore"));
                ImportField addressStoreField = new ImportField(LM.findLCPByCompoundName("addressStore"));
                ImportField idLegalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idChainStoresField = new ImportField(LM.findLCPByCompoundName("idChainStores"));
                ImportField storeTypeField = new ImportField(LM.findLCPByCompoundName("nameStoreType"));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Store"),
                        LM.findLCPByCompoundName("storeId").getMapping(idStoreField));

                ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idLegalEntityField));

                ImportKey<?> chainStoresKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ChainStores"),
                        LM.findLCPByCompoundName("chainStoresId").getMapping(idChainStoresField));

                ImportKey<?> storeTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("StoreType"),
                        LM.findLCPByCompoundName("storeTypeNameChainStores").getMapping(storeTypeField, idChainStoresField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idStoreField, LM.findLCPByCompoundName("idStore").getMapping(storeKey)));
                props.add(new ImportProperty(nameStoreField, LM.findLCPByCompoundName("nameStore").getMapping(storeKey)));
                props.add(new ImportProperty(addressStoreField, LM.findLCPByCompoundName("addressStore").getMapping(storeKey)));
                props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("legalEntityStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(legalEntityKey)));

                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("nameStoreType").getMapping(storeTypeKey)));
                props.add(new ImportProperty(storeTypeField, LM.findLCPByCompoundName("storeTypeStore").getMapping(storeKey),
                        LM.object(LM.findClassByCompoundName("StoreType")).getMapping(storeTypeKey)));
                props.add(new ImportProperty(idChainStoresField, LM.findLCPByCompoundName("chainStoresStoreType").getMapping(storeTypeKey),
                        LM.object(LM.findClassByCompoundName("ChainStores")).getMapping(chainStoresKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (LegalEntity l : storesList) {
                    Store s = (Store) l;
                    data.add(Arrays.asList((Object) s.idStore, s.nameLegalEntity, s.address,
                            s.idLegalEntity, s.storeType, s.idChainStores));
                }

                ImportTable table = new ImportTable(Arrays.asList(idStoreField, nameStoreField, addressStoreField, idLegalEntityField, storeTypeField, idChainStoresField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(storeKey, legalEntityKey, chainStoresKey, storeTypeKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importDepartmentStores(List<DepartmentStore> departmentStoresList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (departmentStoresList != null) {
                ImportField idDepartmentStoreField = new ImportField(LM.findLCPByCompoundName("idDepartmentStore"));
                ImportField nameDepartmentStoreField = new ImportField(LM.findLCPByCompoundName("nameDepartmentStore"));
                ImportField idStoreField = new ImportField(LM.findLCPByCompoundName("idStore"));

                ImportKey<?> departmentStoreKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DepartmentStore"),
                        LM.findLCPByCompoundName("departmentStoreId").getMapping(idDepartmentStoreField));

                ImportKey<?> storeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Store"),
                        LM.findLCPByCompoundName("storeId").getMapping(idStoreField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idDepartmentStoreField, LM.findLCPByCompoundName("idDepartmentStore").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(nameDepartmentStoreField, LM.findLCPByCompoundName("nameDepartmentStore").getMapping(departmentStoreKey)));
                props.add(new ImportProperty(idStoreField, LM.findLCPByCompoundName("storeDepartmentStore").getMapping(departmentStoreKey),
                        LM.object(LM.findClassByCompoundName("Store")).getMapping(storeKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (DepartmentStore s : departmentStoresList) {
                    data.add(Arrays.asList((Object) s.idDepartmentStore, s.name, s.idStore));
                }
                ImportTable table = new ImportTable(Arrays.asList(idDepartmentStoreField, nameDepartmentStoreField, idStoreField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(departmentStoreKey, storeKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importBanks(List<Bank> banksList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (banksList != null) {
                ImportField idBankField = new ImportField(LM.findLCPByCompoundName("idBank"));
                ImportField nameBankField = new ImportField(LM.findLCPByCompoundName("nameBank"));
                ImportField addressBankField = new ImportField(LM.findLCPByCompoundName("dataAddressBankDate"));
                ImportField departmentBankField = new ImportField(LM.findLCPByCompoundName("departmentBank"));
                ImportField mfoBankField = new ImportField(LM.findLCPByCompoundName("MFOBank"));
                ImportField cbuBankField = new ImportField(LM.findLCPByCompoundName("CBUBank"));

                ImportKey<?> bankKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Bank"),
                        LM.findLCPByCompoundName("bankId").getMapping(idBankField));

                DataObject defaultDate = new DataObject(new java.sql.Date(2001 - 1900, 0, 01), DateClass.instance);

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idBankField, LM.findLCPByCompoundName("idBank").getMapping(bankKey)));
                props.add(new ImportProperty(nameBankField, LM.findLCPByCompoundName("nameBank").getMapping(bankKey)));
                props.add(new ImportProperty(addressBankField, LM.findLCPByCompoundName("dataAddressBankDate").getMapping(bankKey, defaultDate)));
                props.add(new ImportProperty(departmentBankField, LM.findLCPByCompoundName("departmentBank").getMapping(bankKey)));
                props.add(new ImportProperty(mfoBankField, LM.findLCPByCompoundName("MFOBank").getMapping(bankKey)));
                props.add(new ImportProperty(cbuBankField, LM.findLCPByCompoundName("CBUBank").getMapping(bankKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Bank b : banksList) {
                    data.add(Arrays.asList((Object) b.idBank, b.name, b.address, b.department, b.mfo, b.cbu));
                }
                ImportTable table = new ImportTable(Arrays.asList(idBankField, nameBankField, addressBankField, departmentBankField, mfoBankField, cbuBankField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(bankKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importRateWastes(List<RateWaste> rateWastesList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (rateWastesList != null) {
                ImportField idWriteOffRateField = new ImportField(LM.findLCPByCompoundName("idWriteOffRate"));
                ImportField nameWriteOffRateField = new ImportField(LM.findLCPByCompoundName("nameWriteOffRate"));
                ImportField percentWriteOffRateField = new ImportField(LM.findLCPByCompoundName("percentWriteOffRate"));
                ImportField countryWriteOffRateField = new ImportField(LM.findLCPByCompoundName("nameWriteOffRate"));

                ImportKey<?> writeOffRateKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("WriteOffRate"),
                        LM.findLCPByCompoundName("writeOffRateId").getMapping(idWriteOffRateField));

                ImportKey<?> countryKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Country"),
                        LM.findLCPByCompoundName("countryName").getMapping(countryWriteOffRateField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idWriteOffRateField, LM.findLCPByCompoundName("idWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(nameWriteOffRateField, LM.findLCPByCompoundName("nameWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(percentWriteOffRateField, LM.findLCPByCompoundName("percentWriteOffRate").getMapping(writeOffRateKey)));
                props.add(new ImportProperty(countryWriteOffRateField, LM.findLCPByCompoundName("countryWriteOffRate").getMapping(writeOffRateKey),
                        LM.object(LM.findClassByCompoundName("Country")).getMapping(countryKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (RateWaste r : rateWastesList) {
                    data.add(Arrays.asList((Object) r.idRateWaste, r.name, r.coef, r.country));
                }
                ImportTable table = new ImportTable(Arrays.asList(idWriteOffRateField, nameWriteOffRateField, percentWriteOffRateField, countryWriteOffRateField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(writeOffRateKey, countryKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void importContracts(List<Contract> contractsList) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        try {
            if (contractsList != null) {
                ImportField idUserContractSkuField = new ImportField(LM.findLCPByCompoundName("idUserContractSku"));
                ImportField idSupplierField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField idCustomerField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
                ImportField numberContractField = new ImportField(LM.findLCPByCompoundName("numberContract"));
                ImportField dateFromContractField = new ImportField(LM.findLCPByCompoundName("dateFromContract"));
                ImportField dateToContractField = new ImportField(LM.findLCPByCompoundName("dateToContract"));
                ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));

                ImportKey<?> userContractSkuKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UserContractSku"),
                        LM.findLCPByCompoundName("userContractSkuId").getMapping(idUserContractSkuField));

                ImportKey<?> supplierKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idSupplierField));

                ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                        LM.findLCPByCompoundName("legalEntityId").getMapping(idCustomerField));

                ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                        LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(idUserContractSkuField, LM.findLCPByCompoundName("idUserContractSku").getMapping(userContractSkuKey)));
                props.add(new ImportProperty(numberContractField, LM.findLCPByCompoundName("numberContract").getMapping(userContractSkuKey)));
                props.add(new ImportProperty(dateFromContractField, LM.findLCPByCompoundName("dateFromContract").getMapping(userContractSkuKey)));
                props.add(new ImportProperty(dateToContractField, LM.findLCPByCompoundName("dateToContract").getMapping(userContractSkuKey)));
                props.add(new ImportProperty(idSupplierField, LM.findLCPByCompoundName("supplierContractSku").getMapping(userContractSkuKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(supplierKey)));
                props.add(new ImportProperty(idCustomerField, LM.findLCPByCompoundName("customerContractSku").getMapping(userContractSkuKey),
                        LM.object(LM.findClassByCompoundName("LegalEntity")).getMapping(customerKey)));
                props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyContract").getMapping(userContractSkuKey),
                        LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));

                List<List<Object>> data = new ArrayList<List<Object>>();
                for (Contract c : contractsList) {
                    data.add(Arrays.asList((Object) c.idContract, c.number, c.dateFrom, c.dateTo,
                            c.idSupplier, c.idCustomer, c.currency));
                }
                ImportTable table = new ImportTable(Arrays.asList(idUserContractSkuField, numberContractField,
                        dateFromContractField, dateToContractField, idSupplierField, idCustomerField,
                        currencyField), data);

                DataSession session = context.createSession();
                session.sql.pushVolatileStats(null);
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userContractSkuKey,
                        supplierKey, customerKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.sql.popVolatileStats(null);
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