package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.query.GroupType;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

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
            colorCodeField, sidField, colorNameField, sizeField, themeCodeField, themeNameField,
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
        invoiceSIDField = new ImportField(LM.sidDocument);
        dateInvoiceField = new ImportField(LM.baseLM.date);
        boxNumberField = new ImportField(LM.sidSupplierBox);
        barCodeField = new ImportField(LM.baseLM.barcode);
        itemSupplierArticleColorSizeField = new ImportField(LM.baseLM.barcode);  //если нет баркода
        colorCodeField = new ImportField(LM.sidColorSupplier);
        sidField = new ImportField(LM.sidArticle);
        colorNameField = new ImportField(LM.baseLM.name);
        sizeField = new ImportField(LM.sidSizeSupplier);
        themeCodeField = new ImportField(LM.sidThemeSupplier);
        themeNameField = new ImportField(LM.nameThemeSupplierArticle);
        collectionCodeField = new ImportField(LM.sidCollectionSupplier);
        collectionNameField = new ImportField(LM.nameCollectionSupplierArticle);
        subCategoryCodeField = new ImportField(LM.sidSubCategorySupplier);
        subCategoryNameField = new ImportField(LM.nameSubCategorySupplierArticle);
        genderField = new ImportField(LM.sidGenderSupplier);
        compositionField = new ImportField(LM.mainCompositionOriginArticle);
        countryField = new ImportField(LM.baseLM.name);
        customCodeField = new ImportField(LM.sidCustomCategoryOrigin);
        customCode6Field = new ImportField(LM.sidCustomCategory6);
        unitPriceField = new ImportField(LM.priceDataDocumentItem);
        unitQuantityField = new ImportField(LM.quantityDataListSku);
        unitNetWeightField = new ImportField(LM.netWeightArticle);
        originalNameField = new ImportField(LM.originalNameArticle);
        numberSkuField = new ImportField(LM.numberDataListSku);
        RRPField = new ImportField(LM.RRPDocumentArticle);
        sidDestinationDataSupplierBoxField = new ImportField(LM.sidDestinationDataSupplierBox);
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
            invoiceKey = new ImportKey(LM.boxInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        } else {
            invoiceKey = new ImportKey(LM.simpleInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        }
        properties.add(new ImportProperty(invoiceSIDField, LM.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(dateInvoiceField, LM.baseLM.date.getMapping(invoiceKey)));
        properties.add(new ImportProperty(supplier, LM.supplierDocument.getMapping(invoiceKey)));

        ImportKey<?> boxKey = null;
        ImportKey<?> destinationKey = null;
        if (!isSimpleInvoice()) {
            boxKey = new ImportKey(LM.supplierBox, LM.supplierBoxSIDSupplier.getMapping(boxNumberField, supplier));
            properties.add(new ImportProperty(invoiceSIDField, LM.boxInvoiceSupplierBox.getMapping(boxKey), LM.object(LM.boxInvoice).getMapping(invoiceKey)));
            properties.add(new ImportProperty(boxNumberField, LM.sidSupplierBox.getMapping(boxKey)));
            properties.add(new ImportProperty(boxNumberField, LM.baseLM.barcode.getMapping(boxKey)));
            destinationKey = new ImportKey(LM.store, LM.destinationSIDSupplier.getMapping(sidDestinationDataSupplierBoxField, supplier));
            properties.add(new ImportProperty(sidDestinationDataSupplierBoxField, LM.sidDestinationSupplier.getMapping(destinationKey, supplier)));
            properties.add(new ImportProperty(sidDestinationDataSupplierBoxField, LM.destinationDataSupplierBox.getMapping(boxKey), LM.object(LM.destination).getMapping(destinationKey)));
        }

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, LM.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, LM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, LM.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, LM.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = null;
        if (hasBarCodeKey()) {
            itemKey = new ImportKey(LM.item, LM.baseLM.barcodeToObject.getMapping(barCodeField));
        } else {
            itemKey = new ImportKey(LM.item, LM.itemSupplierArticleSIDColorSIDSizeSID.getMapping(supplier, sidField, colorCodeField, sizeField));
        }
        if (hasBarCode())
            properties.add(new ImportProperty(barCodeField, LM.baseLM.barcode.getMapping(itemKey)));

        properties.add(new ImportProperty(unitNetWeightField, LM.netWeightDataSku.getMapping(itemKey)));
        properties.add(new ImportProperty(sidField, LM.articleCompositeItem.getMapping(itemKey), LM.object(LM.articleComposite).getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(LM.countrySupplier, LM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, LM.baseLM.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, LM.countrySupplierOfOriginArticle.getMapping(articleKey), LM.object(LM.countrySupplier).getMapping(countryKey)));

        ImportKey<?> customCategoryKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, LM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCode6Field, LM.sidCustomCategory6CustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, LM.customCategoryOriginArticle.getMapping(articleKey),
                LM.object(LM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, LM.sidCustomCategory6.getMapping(customCategory6Key)));

        ImportKey<?> colorKey = new ImportKey(LM.colorSupplier, LM.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, LM.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, LM.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, LM.baseLM.name.getMapping(colorKey)));
        properties.add(new ImportProperty(colorCodeField, LM.colorSupplierItem.getMapping(itemKey), LM.object(LM.colorSupplier).getMapping(colorKey)));

        ImportKey<?> sizeKey = new ImportKey(LM.sizeSupplier, LM.sizeSIDSupplier.getMapping(sizeField, supplier));
        properties.add(new ImportProperty(sizeField, LM.sidSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(supplier, LM.supplierSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(sizeField, LM.sizeSupplierItem.getMapping(itemKey), LM.object(LM.sizeSupplier).getMapping(sizeKey)));

        ImportKey<?> themeKey = new ImportKey(LM.themeSupplier, LM.themeSIDSupplier.getMapping(themeCodeField, supplier));
        properties.add(new ImportProperty(themeCodeField, LM.sidThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(supplier, LM.supplierThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(themeNameField, LM.baseLM.name.getMapping(themeKey)));
        properties.add(new ImportProperty(themeCodeField, LM.themeSupplierArticle.getMapping(articleKey), LM.object(LM.themeSupplier).getMapping(themeKey)));

        ImportKey<?> collectionKey = new ImportKey(LM.collectionSupplier, LM.collectionSIDSupplier.getMapping(collectionCodeField, supplier));
        properties.add(new ImportProperty(collectionCodeField, LM.sidCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionNameField, LM.baseLM.name.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionCodeField, LM.collectionSupplierArticle.getMapping(articleKey), LM.object(LM.collectionSupplier).getMapping(collectionKey)));

        ImportKey<?> subCategoryKey = new ImportKey(LM.subCategorySupplier, LM.subCategorySIDSupplier.getMapping(subCategoryCodeField, supplier));
        properties.add(new ImportProperty(subCategoryCodeField, LM.sidSubCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryNameField, LM.baseLM.name.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryCodeField, LM.subCategorySupplierArticle.getMapping(articleKey), LM.object(LM.subCategorySupplier).getMapping(subCategoryKey)));

        ImportKey<?> genderKey = new ImportKey(LM.genderSupplier, LM.genderSIDSupplier.getMapping(genderField, supplier));
        properties.add(new ImportProperty(genderField, LM.sidGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(supplier, LM.supplierGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(genderField, LM.genderSupplierArticle.getMapping(articleKey), LM.object(LM.genderSupplier).getMapping(genderKey)));


        if (!isSimpleInvoice()) {
            properties.add(new ImportProperty(numberSkuField, LM.numberListArticle.getMapping(boxKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, LM.numberDataListSku.getMapping(boxKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, LM.quantityDataListSku.getMapping(boxKey, itemKey), GroupType.SUM));
        } else {
            properties.add(new ImportProperty(numberSkuField, LM.numberListArticle.getMapping(invoiceKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, LM.numberDataListSku.getMapping(invoiceKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, LM.quantityDataListSku.getMapping(invoiceKey, itemKey), GroupType.SUM));
        }

        properties.add(new ImportProperty(unitPriceField, LM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(RRPField, LM.RRPDocumentArticle.getMapping(invoiceKey, articleKey)));
        properties.add(new ImportProperty(unitPriceField, LM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        for (byte[] file : fileList) {
            ImportTable table;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream(file);
                ImportInputTable t = createTable(inFile);
                table = createImporter(t).getTable();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Integer index = -1;
            for (int i = 0; i < table.fields.size(); i++) {
                if (table.fields.get(i).equals(customCodeField))
                    index = i;
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

            ImportKey<?>[] keysArray;
            if (!isSimpleInvoice()) {
                keysArray = new ImportKey<?>[]{invoiceKey, boxKey, destinationKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key, themeKey, collectionKey, subCategoryKey, genderKey};
            } else {
                keysArray = new ImportKey<?>[]{invoiceKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key, themeKey, collectionKey, subCategoryKey, genderKey};
            }
            new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize(false, false);
        }

        context.delayUserInterfaction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
    }
}
