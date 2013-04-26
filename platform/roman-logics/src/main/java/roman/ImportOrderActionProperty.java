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
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public abstract class ImportOrderActionProperty extends BaseImportActionProperty {

    protected ImportField orderSIDField, dateOrderField, barCodeField, dateFromOrderField, dateToOrderField,
            dateFromOrderArticleField, dateToOrderArticleField, colorCodeField, sidField, colorNameField, sizeField,
            themeCodeField, themeNameField, collectionCodeField, collectionNameField, subCategoryCodeField, subCategoryNameField,
            genderField, compositionField, countryField, customCodeField,
            customCode6Field, unitPriceField, unitQuantityField, unitNetWeightField, originalNameField, numberSkuField, RRPField;

    public ImportOrderActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass) {
        super(RomanLM, "Импортировать заказ", supplierClass);
    }

    public ImportOrderActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass, String extensions) {
        super(RomanLM, "Импортировать заказ", supplierClass, extensions);
    }

    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException, ParseException, SAXException, OpenXML4JException {
        return new ExcelInputTable(inFile);
    }

    protected abstract SingleSheetImporter createImporter(ImportInputTable inputTable);

    private void initFields() {
        orderSIDField = new ImportField(RomanLM.sidDocument);
        dateOrderField = new ImportField(RomanLM.date);
        dateFromOrderField = new ImportField(RomanLM.dateFromOrder);
        dateToOrderField = new ImportField(RomanLM.dateToOrder);
        dateFromOrderArticleField = new ImportField(RomanLM.dateFromOrderArticle);
        dateToOrderArticleField = new ImportField(RomanLM.dateToOrderArticle);
        barCodeField = new ImportField(RomanLM.barcode);
        sidField = new ImportField(RomanLM.sidArticle);
        colorCodeField = new ImportField(RomanLM.sidColorSupplier);
        colorNameField = new ImportField(RomanLM.name);
        sizeField = new ImportField(RomanLM.sidSizeSupplier);
        themeCodeField = new ImportField(RomanLM.sidThemeSupplier);
        themeNameField = new ImportField(RomanLM.nameThemeSupplierArticle);
        collectionCodeField = new ImportField(RomanLM.sidCollectionSupplier);
        collectionNameField = new ImportField(RomanLM.nameCollectionSupplierArticle);
        subCategoryCodeField = new ImportField(RomanLM.sidSubCategorySupplier);
        subCategoryNameField = new ImportField(RomanLM.nameSubCategorySupplierArticle);
        genderField = new ImportField(RomanLM.sidGenderSupplier);
        compositionField = new ImportField(RomanLM.mainCompositionOriginArticle);
        countryField = new ImportField(RomanLM.name);
        customCodeField = new ImportField(RomanLM.sidCustomCategoryOrigin);
        customCode6Field = new ImportField(RomanLM.sidCustomCategory6);
        unitPriceField = new ImportField(RomanLM.priceDataDocumentItem);
        unitQuantityField = new ImportField(RomanLM.quantityDataListSku);
        unitNetWeightField = new ImportField(RomanLM.netWeightArticle);
        originalNameField = new ImportField(RomanLM.originalNameArticle);
        numberSkuField = new ImportField(RomanLM.numberDataListSku);
        RRPField = new ImportField(RomanLM.RRPDocumentArticle);
    }

    protected boolean hasBarCode() {
        return true;
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        DataObject supplier = context.getKeyValue(supplierInterface);

        initFields();

        List<byte[]> fileList = valueClass.getFiles(userValue);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> orderKey = new ImportKey(RomanLM.order, RomanLM.documentSIDSupplier.getMapping(orderSIDField, supplier));

        properties.add(new ImportProperty(orderSIDField, RomanLM.sidDocument.getMapping(orderKey)));
        properties.add(new ImportProperty(dateOrderField, RomanLM.date.getMapping(orderKey)));
        properties.add(new ImportProperty(dateFromOrderField, RomanLM.dateFromOrder.getMapping(orderKey)));
        properties.add(new ImportProperty(dateToOrderField, RomanLM.dateToOrder.getMapping(orderKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierDocument.getMapping(orderKey)));

        ImportKey<?> articleKey = new ImportKey(RomanLM.articleComposite, RomanLM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, RomanLM.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, RomanLM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, RomanLM.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, RomanLM.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey;
        if (hasBarCode()) {
            itemKey = new ImportKey(RomanLM.item, RomanLM.barcodeToObject.getMapping(barCodeField));
            properties.add(new ImportProperty(barCodeField, RomanLM.barcode.getMapping(itemKey)));
        } else {
            itemKey = new ImportKey(RomanLM.item, RomanLM.itemSupplierArticleSIDColorSIDSizeSID.getMapping(supplier, sidField, colorCodeField, sizeField));
        }

        properties.add(new ImportProperty(unitNetWeightField, RomanLM.netWeightDataSku.getMapping(itemKey)));
        properties.add(new ImportProperty(sidField, RomanLM.articleCompositeItem.getMapping(itemKey), RomanLM.object(RomanLM.articleComposite).getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(RomanLM.countrySupplier, RomanLM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, RomanLM.name.getMapping(countryKey)));
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
        properties.add(new ImportProperty(colorNameField, RomanLM.name.getMapping(colorKey)));
        properties.add(new ImportProperty(colorCodeField, RomanLM.colorSupplierItem.getMapping(itemKey), RomanLM.object(RomanLM.colorSupplier).getMapping(colorKey)));

        ImportKey<?> sizeKey = new ImportKey(RomanLM.sizeSupplier, RomanLM.sizeSIDSupplier.getMapping(sizeField, supplier));
        properties.add(new ImportProperty(sizeField, RomanLM.sidSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(sizeField, RomanLM.sizeSupplierItem.getMapping(itemKey), RomanLM.object(RomanLM.sizeSupplier).getMapping(sizeKey)));

        ImportKey<?> themeKey = new ImportKey(RomanLM.themeSupplier, RomanLM.themeSIDSupplier.getMapping(themeCodeField, supplier));
        properties.add(new ImportProperty(themeCodeField, RomanLM.sidThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(themeNameField, RomanLM.name.getMapping(themeKey)));
        properties.add(new ImportProperty(themeCodeField, RomanLM.themeSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.themeSupplier).getMapping(themeKey)));

        ImportKey<?> collectionKey = new ImportKey(RomanLM.collectionSupplier, RomanLM.collectionSIDSupplier.getMapping(collectionCodeField, supplier));
        properties.add(new ImportProperty(collectionCodeField, RomanLM.sidCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierCollectionSupplier.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionNameField, RomanLM.name.getMapping(collectionKey)));
        properties.add(new ImportProperty(collectionCodeField, RomanLM.collectionSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.collectionSupplier).getMapping(themeKey)));

        ImportKey<?> subCategoryKey = new ImportKey(RomanLM.subCategorySupplier, RomanLM.subCategorySIDSupplier.getMapping(subCategoryCodeField, supplier));
        properties.add(new ImportProperty(subCategoryCodeField, RomanLM.sidSubCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryNameField, RomanLM.name.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryCodeField, RomanLM.subCategorySupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.subCategorySupplier).getMapping(subCategoryKey)));

        ImportKey<?> genderKey = new ImportKey(RomanLM.genderSupplier, RomanLM.genderSIDSupplier.getMapping(genderField, supplier));
        properties.add(new ImportProperty(genderField, RomanLM.sidGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(genderField, RomanLM.genderSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.genderSupplier).getMapping(genderKey)));

        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDataDocumentItem.getMapping(orderKey, itemKey)));
        properties.add(new ImportProperty(RRPField, RomanLM.RRPDocumentArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDocumentArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(dateFromOrderArticleField, RomanLM.dateFromOrderArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(dateToOrderArticleField, RomanLM.dateToOrderArticle.getMapping(orderKey, articleKey)));

        properties.add(new ImportProperty(numberSkuField, RomanLM.numberListArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(numberSkuField, RomanLM.numberDataListSku.getMapping(orderKey, itemKey)));
        properties.add(new ImportProperty(unitQuantityField, RomanLM.quantityDataListSku.getMapping(orderKey, itemKey), GroupType.SUM));


        for (byte[] file : fileList) {
            ImportTable table;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream(file);
                table = createImporter(createTable(inFile)).getTable();
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

            Integer dateFromIndex = -1;
            Integer dateToIndex = -1;
            Integer sidIndex = -1;
            for (int i = 0; i < table.fields.size(); i++) {
                if (table.fields.get(i).equals(dateFromOrderField))
                    dateFromIndex = i;
                else if (table.fields.get(i).equals(dateToOrderField))
                    dateToIndex = i;
                else if (table.fields.get(i).equals(sidField))
                    sidIndex = i;
            }
            Map<String, Date> dateFromMin = new HashMap<String, Date>();
            Map<String, Date> dateToMax = new HashMap<String, Date>();
            if ((dateFromIndex != -1 && dateToIndex != -1)) {
                for (List<Object> editingRow : table.data) {
                    if (editingRow.get(dateFromIndex) != null) {
                        String orderSID = (String) editingRow.get(sidIndex);
                        Date dateFrom = (Date) editingRow.get(dateFromIndex);
                        Date dateMin = dateFromMin.get(orderSID);
                        if (dateMin != null) {
                            if (dateFrom.before(dateMin))
                                dateFromMin.put(orderSID, dateFrom);
                        } else
                            dateFromMin.put(orderSID, dateFrom);
                    }
                    if (editingRow.get(dateToIndex) != null) {
                        Date dateTo = (Date) editingRow.get(dateToIndex);
                        String orderSID = (String) editingRow.get(sidIndex);
                        Date dateMax = dateToMax.get(orderSID);
                        if (dateMax != null) {
                            if (dateTo.after(dateMax))
                                dateToMax.put(orderSID, dateTo);
                        } else
                            dateToMax.put(orderSID, dateTo);
                    }
                }
                for (List<Object> editingRow : table.data) {
                    String orderSID = (String) editingRow.get(sidIndex);
                    if ((dateFromMin.containsKey(orderSID)) && (editingRow.get(dateFromIndex) != null))
                        editingRow.set(dateFromIndex, dateFromMin.get(orderSID));
                    if ((dateToMax.containsKey(orderSID)) && (editingRow.get(dateToIndex) != null))
                        editingRow.set(dateToIndex, dateToMax.get(orderSID));
                }
            }
            ImportKey<?>[] keysArray = new ImportKey<?>[]{orderKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key, themeKey, collectionKey, subCategoryKey, genderKey};

            new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize(true, false);
        }

        context.delayUserInterfaction(new

                                      MessageClientAction("Данные были успешно приняты", "Импорт")

        );
    }
}
