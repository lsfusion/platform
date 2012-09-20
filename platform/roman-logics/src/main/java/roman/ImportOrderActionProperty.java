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
        orderSIDField = new ImportField(LM.sidDocument);
        dateOrderField = new ImportField(LM.baseLM.date);
        dateFromOrderField = new ImportField(LM.dateFromOrder);
        dateToOrderField = new ImportField(LM.dateToOrder);
        dateFromOrderArticleField = new ImportField(LM.dateFromOrderArticle);
        dateToOrderArticleField = new ImportField(LM.dateToOrderArticle);
        barCodeField = new ImportField(LM.baseLM.barcode);
        sidField = new ImportField(LM.sidArticle);
        colorCodeField = new ImportField(LM.sidColorSupplier);
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
    }

    protected boolean hasBarCode() {
        return true;
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        DataObject supplier = context.getKeyValue(supplierInterface);

        initFields();

        List<byte[]> fileList = valueClass.getFiles(userValue);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> orderKey = new ImportKey(LM.order, LM.documentSIDSupplier.getMapping(orderSIDField, supplier));

        properties.add(new ImportProperty(orderSIDField, LM.sidDocument.getMapping(orderKey)));
        properties.add(new ImportProperty(dateOrderField, LM.baseLM.date.getMapping(orderKey)));
        properties.add(new ImportProperty(dateFromOrderField, LM.dateFromOrder.getMapping(orderKey)));
        properties.add(new ImportProperty(dateToOrderField, LM.dateToOrder.getMapping(orderKey)));
        properties.add(new ImportProperty(supplier, LM.supplierDocument.getMapping(orderKey)));

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, LM.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, LM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, LM.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, LM.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = null;
        if (hasBarCode()) {
            itemKey = new ImportKey(LM.item, LM.baseLM.barcodeToObject.getMapping(barCodeField));
            properties.add(new ImportProperty(barCodeField, LM.baseLM.barcode.getMapping(itemKey)));
        } else {
            itemKey = new ImportKey(LM.item, LM.itemSupplierArticleSIDColorSIDSizeSID.getMapping(supplier, sidField, colorCodeField, sizeField));
        }

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
        properties.add(new ImportProperty(collectionCodeField, LM.collectionSupplierArticle.getMapping(articleKey), LM.object(LM.collectionSupplier).getMapping(themeKey)));

        ImportKey<?> subCategoryKey = new ImportKey(LM.subCategorySupplier, LM.subCategorySIDSupplier.getMapping(subCategoryCodeField, supplier));
        properties.add(new ImportProperty(subCategoryCodeField, LM.sidSubCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCategorySupplier.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryNameField, LM.baseLM.name.getMapping(subCategoryKey)));
        properties.add(new ImportProperty(subCategoryCodeField, LM.subCategorySupplierArticle.getMapping(articleKey), LM.object(LM.subCategorySupplier).getMapping(subCategoryKey)));

        ImportKey<?> genderKey = new ImportKey(LM.genderSupplier, LM.genderSIDSupplier.getMapping(genderField, supplier));
        properties.add(new ImportProperty(genderField, LM.sidGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(supplier, LM.supplierGenderSupplier.getMapping(genderKey)));
        properties.add(new ImportProperty(genderField, LM.genderSupplierArticle.getMapping(articleKey), LM.object(LM.genderSupplier).getMapping(genderKey)));

        properties.add(new ImportProperty(unitPriceField, LM.priceDataDocumentItem.getMapping(orderKey, itemKey)));
        properties.add(new ImportProperty(RRPField, LM.RRPDocumentArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(unitPriceField, LM.priceDocumentArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(dateFromOrderArticleField, LM.dateFromOrderArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(dateToOrderArticleField, LM.dateToOrderArticle.getMapping(orderKey, articleKey)));

        properties.add(new ImportProperty(numberSkuField, LM.numberListArticle.getMapping(orderKey, articleKey)));
        properties.add(new ImportProperty(numberSkuField, LM.numberDataListSku.getMapping(orderKey, itemKey)));
        properties.add(new ImportProperty(unitQuantityField, LM.quantityDataListSku.getMapping(orderKey, itemKey), GroupType.SUM));


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
