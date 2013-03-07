package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;
import roman.actions.ImportPreviewClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.query.GroupType;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import roman.actions.InvoiceProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:01
 */

public abstract class ImportBoxInvoiceActionProperty extends BaseImportActionProperty {

    protected ImportField invoiceSIDField, dateInvoiceField, boxNumberField, barCodeField, itemSupplierArticleColorSizeField,
            colorCodeField, sidField, colorNameField, sizeField, brandCodeField, brandNameField, themeCodeField, themeNameField,
            collectionCodeField, collectionNameField, subCategoryCodeField, subCategoryNameField,
            genderField, compositionField, countryField, customCodeField, customCode6Field, unitPriceField,
            unitQuantityField, unitNetWeightField, originalNameField, numberSkuField, RRPField, sidDestinationDataSupplierBoxField;

    public ImportBoxInvoiceActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass) {
        super(RomanLM, "Импортировать инвойс", supplierClass);
    }

    public ImportBoxInvoiceActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass, String extensions) {
        super(RomanLM, "Импортировать инвойс", supplierClass, extensions);
    }

    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException, ParseException, SAXException, OpenXML4JException {
        return new ExcelInputTable(inFile);
    }

    protected abstract SingleSheetImporter createImporter(ImportInputTable inputTable);

    private void initFields() {
        invoiceSIDField = new ImportField(RomanLM.sidDocument);
        dateInvoiceField = new ImportField(RomanLM.date);
        boxNumberField = new ImportField(RomanLM.sidSupplierBox);
        barCodeField = new ImportField(RomanLM.barcode);
        itemSupplierArticleColorSizeField = new ImportField(RomanLM.barcode);  //если нет баркода
        colorCodeField = new ImportField(RomanLM.sidColorSupplier);
        sidField = new ImportField(RomanLM.sidArticle);
        colorNameField = new ImportField(RomanLM.baseLM.name);
        sizeField = new ImportField(RomanLM.sidSizeSupplier);
        brandCodeField = new ImportField(RomanLM.sidBrandSupplier);
        brandNameField = new ImportField(RomanLM.nameBrandSupplierArticle);
        themeCodeField = new ImportField(RomanLM.sidThemeSupplier);
        themeNameField = new ImportField(RomanLM.nameThemeSupplierArticle);
        collectionCodeField = new ImportField(RomanLM.sidCollectionSupplier);
        collectionNameField = new ImportField(RomanLM.nameCollectionSupplierArticle);
        subCategoryCodeField = new ImportField(RomanLM.sidSubCategorySupplier);
        subCategoryNameField = new ImportField(RomanLM.nameSubCategorySupplierArticle);
        genderField = new ImportField(RomanLM.sidGenderSupplier);
        compositionField = new ImportField(RomanLM.mainCompositionOriginArticle);
        countryField = new ImportField(RomanLM.baseLM.name);
        customCodeField = new ImportField(RomanLM.sidCustomCategoryOrigin);
        customCode6Field = new ImportField(RomanLM.sidCustomCategory6);
        unitPriceField = new ImportField(RomanLM.priceDataDocumentItem);
        unitQuantityField = new ImportField(RomanLM.quantityDataListSku);
        unitNetWeightField = new ImportField(RomanLM.netWeightArticle);
        originalNameField = new ImportField(RomanLM.originalNameArticle);
        numberSkuField = new ImportField(RomanLM.numberDataListSku);
        RRPField = new ImportField(RomanLM.RRPDocumentArticle);
        sidDestinationDataSupplierBoxField = new ImportField(RomanLM.sidDestinationDataSupplierBox);
    }

    protected boolean isSimpleInvoice() {
        return false;
    }

    protected boolean hasBarCode() {
        return true;
    }

    protected boolean hasBarCodeKey() {
        return true;
    }


    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        DataObject supplier = context.getKeyValue(supplierInterface);

        initFields();

        List<byte[]> fileList = valueClass.getFiles(userValue);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey;

        if (!isSimpleInvoice()) {
            invoiceKey = new ImportKey(RomanLM.boxInvoice, RomanLM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        } else {
            invoiceKey = new ImportKey(RomanLM.simpleInvoice, RomanLM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        }
        properties.add(new ImportProperty(invoiceSIDField, RomanLM.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(dateInvoiceField, RomanLM.date.getMapping(invoiceKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierDocument.getMapping(invoiceKey)));

        ImportKey<?> boxKey = null;
        ImportKey<?> destinationKey = null;
        if (!isSimpleInvoice()) {
            boxKey = new ImportKey(RomanLM.supplierBox, RomanLM.supplierBoxSIDSupplier.getMapping(boxNumberField, supplier));
            properties.add(new ImportProperty(invoiceSIDField, RomanLM.boxInvoiceSupplierBox.getMapping(boxKey), RomanLM.object(RomanLM.boxInvoice).getMapping(invoiceKey)));
            properties.add(new ImportProperty(boxNumberField, RomanLM.sidSupplierBox.getMapping(boxKey)));
            properties.add(new ImportProperty(boxNumberField, RomanLM.barcode.getMapping(boxKey)));
            destinationKey = new ImportKey(RomanLM.store, RomanLM.destinationSIDSupplier.getMapping(sidDestinationDataSupplierBoxField, supplier));
            properties.add(new ImportProperty(sidDestinationDataSupplierBoxField, RomanLM.sidDestinationSupplier.getMapping(destinationKey, supplier)));
            properties.add(new ImportProperty(sidDestinationDataSupplierBoxField, RomanLM.destinationDataSupplierBox.getMapping(boxKey), RomanLM.object(RomanLM.destination).getMapping(destinationKey)));
        }

        ImportKey<?> articleKey = new ImportKey(RomanLM.articleComposite, RomanLM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, RomanLM.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, RomanLM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, RomanLM.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, RomanLM.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = null;
        if (hasBarCodeKey()) {
            itemKey = new ImportKey(RomanLM.item, RomanLM.barcodeToObject.getMapping(barCodeField));
        } else {
            itemKey = new ImportKey(RomanLM.item, RomanLM.itemSupplierArticleSIDColorSIDSizeSID.getMapping(supplier, sidField, colorCodeField, sizeField));
        }
        if (hasBarCode())
            properties.add(new ImportProperty(barCodeField, RomanLM.barcode.getMapping(itemKey)));

        properties.add(new ImportProperty(unitNetWeightField, RomanLM.netWeightDataSku.getMapping(itemKey)));
        properties.add(new ImportProperty(sidField, RomanLM.articleCompositeItem.getMapping(itemKey), RomanLM.object(RomanLM.articleComposite).getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(RomanLM.countrySupplier, RomanLM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, RomanLM.baseLM.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, RomanLM.countrySupplierOfOriginArticle.getMapping(articleKey), RomanLM.object(RomanLM.countrySupplier).getMapping(countryKey)));

        ImportKey<?> customCategoryKey = new ImportKey(RomanLM.customCategoryOrigin, RomanLM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, RomanLM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCode6Field, RomanLM.sidCustomCategory6CustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, RomanLM.customCategoryOriginArticle.getMapping(articleKey),
                RomanLM.object(RomanLM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(RomanLM.customCategory6, RomanLM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, RomanLM.sidCustomCategory6.getMapping(customCategory6Key)));

        ImportKey<?> colorKey = new ImportKey(RomanLM.colorSupplier, RomanLM.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, RomanLM.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, RomanLM.baseLM.name.getMapping(colorKey)));
        properties.add(new ImportProperty(colorCodeField, RomanLM.colorSupplierItem.getMapping(itemKey), RomanLM.object(RomanLM.colorSupplier).getMapping(colorKey)));

        ImportKey<?> sizeKey = new ImportKey(RomanLM.sizeSupplier, RomanLM.sizeSIDSupplier.getMapping(sizeField, supplier));
        properties.add(new ImportProperty(sizeField, RomanLM.sidSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(sizeField, RomanLM.sizeSupplierItem.getMapping(itemKey), RomanLM.object(RomanLM.sizeSupplier).getMapping(sizeKey)));

        ImportKey<?> brandKey = new ImportKey(RomanLM.brandSupplier, RomanLM.brandSIDSupplier.getMapping(brandCodeField, supplier));
        properties.add(new ImportProperty(brandCodeField, RomanLM.sidBrandSupplier.getMapping(brandKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierBrandSupplier.getMapping(brandKey)));
        properties.add(new ImportProperty(brandNameField, RomanLM.baseLM.name.getMapping(brandKey)));
        properties.add(new ImportProperty(brandCodeField, RomanLM.brandSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.brandSupplier).getMapping(brandKey)));

        ImportKey<?> themeKey = new ImportKey(RomanLM.themeSupplier, RomanLM.themeSIDSupplier.getMapping(themeCodeField, supplier));
        properties.add(new ImportProperty(themeCodeField, RomanLM.sidThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(themeNameField, RomanLM.baseLM.name.getMapping(themeKey)));
        properties.add(new ImportProperty(themeCodeField, RomanLM.themeSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.themeSupplier).getMapping(themeKey)));

        ImportKey<?> collectionKey = new ImportKey(RomanLM.collectionSupplier, RomanLM.collectionSIDSupplier.getMapping(collectionCodeField, supplier));
        properties.add(new ImportProperty(collectionCodeField, RomanLM.sidCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionNameField, RomanLM.baseLM.name.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionCodeField, RomanLM.collectionSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.collectionSupplier).getMapping(collectionKey)));

        ImportKey<?> subCategoryKey = new ImportKey(RomanLM.subCategorySupplier, RomanLM.subCategorySIDSupplier.getMapping(subCategoryCodeField, supplier));
        properties.add(new ImportProperty(subCategoryCodeField, RomanLM.sidSubCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierSubCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryNameField, RomanLM.baseLM.name.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryCodeField, RomanLM.subCategorySupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.subCategorySupplier).getMapping(subCategoryKey)));

        ImportKey<?> genderKey = new ImportKey(RomanLM.genderSupplier, RomanLM.genderSIDSupplier.getMapping(genderField, supplier));
        properties.add(new ImportProperty(genderField, RomanLM.sidGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(genderField, RomanLM.genderSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.genderSupplier).getMapping(genderKey)));


        if (!isSimpleInvoice()) {
            properties.add(new ImportProperty(numberSkuField, RomanLM.numberListArticle.getMapping(boxKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, RomanLM.numberDataListSku.getMapping(boxKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, RomanLM.quantityDataListSku.getMapping(boxKey, itemKey), GroupType.SUM));
        } else {
            properties.add(new ImportProperty(numberSkuField, RomanLM.numberListArticle.getMapping(invoiceKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, RomanLM.numberDataListSku.getMapping(invoiceKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, RomanLM.quantityDataListSku.getMapping(invoiceKey, itemKey), GroupType.SUM));
        }

        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(RRPField, RomanLM.RRPDocumentArticle.getMapping(invoiceKey, articleKey)));
        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        ImportTable table = null;

        for (byte[] file : fileList) {
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream(file);
                ImportInputTable t = createTable(inFile);
                if (table == null)
                    table = createImporter(t).getTable();
                else
                    table.add(createImporter(t).getTable());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Integer index = -1;
        Integer invoiceSIDIndex = -1;
        Integer dateInvoiceIndex = -1;
        Integer unitQuantityIndex = -1;
        Integer unitPriceIndex = -1;
        Integer unitNetWeightIndex = -1;
        Map<String, InvoiceProperties> invoiceMap = new HashMap<String, InvoiceProperties>();
        for (int i = 0; i < table.fields.size(); i++) {
            if (table.fields.get(i).equals(customCodeField))
                index = i;
            else if (table.fields.get(i).equals(invoiceSIDField))
                invoiceSIDIndex = i;
            else if (table.fields.get(i).equals(dateInvoiceField))
                dateInvoiceIndex = i;
            else if (table.fields.get(i).equals(unitQuantityField))
                unitQuantityIndex = i;
            else if (table.fields.get(i).equals(unitPriceField))
                unitPriceIndex = i;
            else if (table.fields.get(i).equals(unitNetWeightField))
                unitNetWeightIndex = i;
        }

        if (index != -1) {
            for (List<Object> editingRow : table.data) {
                String val = null;
                if (editingRow.get(index) != null) {
                    val = editingRow.get(index).toString();
                    while (val.length() < 10)
                        val = val + "0";
                }
                editingRow.set(index, val);
            }
        }
        if (invoiceSIDIndex != -1) {
            for (List<Object> editingRow : table.data) {
                String invoiceSID = (String) editingRow.get(invoiceSIDIndex);
                Date dateInvoice = dateInvoiceIndex != -1 ? (Date) editingRow.get(dateInvoiceIndex) : null;
                Double unitQuantity = (unitQuantityIndex != -1 && editingRow.get(unitQuantityIndex) != null) ? (Double) editingRow.get(unitQuantityIndex) : 0.0;
                Double unitPrice = (unitPriceIndex != -1 && editingRow.get(unitPriceIndex) != null) ? (Double) editingRow.get(unitPriceIndex) : 0.0;
                Double unitNetWeight = (unitNetWeightIndex != -1 && editingRow.get(unitNetWeightIndex) != null) ? (Double) editingRow.get(unitNetWeightIndex) : 0.0;
                Double sumPrice = unitQuantity * unitPrice;
                Double sumNetWeight = unitQuantity * unitNetWeight;
                InvoiceProperties invoice = invoiceMap.containsKey(invoiceSID) ? invoiceMap.get(invoiceSID) : new InvoiceProperties(invoiceSID, dateInvoice, 0.0, 0.0, 0.0);
                invoice.quantityDocument += unitQuantity;
                invoice.sumDocument += sumPrice;
                invoice.netWeightDocument += sumNetWeight;

                invoiceMap.put(invoiceSID, invoice);
            }
        }

        ImportKey<?>[] keysArray;
        if (!isSimpleInvoice()) {
            keysArray = new ImportKey<?>[]{invoiceKey, boxKey, destinationKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key, brandKey, themeKey, collectionKey, subCategoryKey, genderKey};
        } else {
            keysArray = new ImportKey<?>[]{invoiceKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key, brandKey, themeKey, collectionKey, subCategoryKey, genderKey};
        }

        ArrayList<InvoiceProperties> invoicePropertiesList = new ArrayList<InvoiceProperties>();
        for (InvoiceProperties ip : invoiceMap.values()) {
            invoicePropertiesList.add(ip);
        }
        HashSet<String> result = (HashSet<String>) context.requestUserInteraction(new ImportPreviewClientAction(invoicePropertiesList));
        if (result != null) {
            List<List<Object>> editedData = new ArrayList<List<Object>>();
            for (List<Object> editingRow : table.data) {
                if (editingRow.get(invoiceSIDIndex) != null) {
                    if (result.contains(editingRow.get(invoiceSIDIndex)))
                        editedData.add(editingRow);
                }
            }
            if (!editedData.isEmpty()) {
                table.data = editedData;
                new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize(false, false);

                context.delayUserInterfaction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
            }
        }

    }
}
