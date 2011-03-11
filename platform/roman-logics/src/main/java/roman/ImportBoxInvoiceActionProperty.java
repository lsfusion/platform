package roman;

import jxl.read.biff.BiffException;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:01
 */

public abstract class ImportBoxInvoiceActionProperty extends BaseImportActionProperty {

    protected ImportField invoiceSIDField, boxNumberField, barCodeField, colorCodeField, sidField,
    colorNameField, sizeField, compositionField, countryField, customCodeField, customCode6Field,
    unitPriceField, unitQuantityField, unitNetWeightField, originalNameField, numberSkuField;

    public ImportBoxInvoiceActionProperty(RomanBusinessLogics BL, ValueClass supplierClass) {
        super(BL, "Импортировать инвойс", supplierClass);
    }

    public ImportBoxInvoiceActionProperty(RomanBusinessLogics BL, ValueClass supplierClass, String extensions) {
        super(BL, "Импортировать инвойс", supplierClass, extensions);
    }

    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new ExcelInputTable(inFile);
    }

    protected abstract SingleSheetImporter createExporter(ImportInputTable inputTable);

    private void initFields() {
        invoiceSIDField = new ImportField(BL.sidDocument);
        boxNumberField = new ImportField(BL.sidSupplierBox);
        barCodeField = new ImportField(BL.barcode);
        colorCodeField = new ImportField(BL.sidColorSupplier);
        sidField = new ImportField(BL.sidArticle);
        colorNameField = new ImportField(BL.name);
        sizeField = new ImportField(BL.sidSizeSupplier);
        compositionField = new ImportField(BL.mainCompositionOriginArticle);
        countryField = new ImportField(BL.name);
        customCodeField = new ImportField(BL.sidCustomCategoryOrigin);
        customCode6Field = new ImportField(BL.sidCustomCategory6);
        unitPriceField = new ImportField(BL.priceDataDocumentItem);
        unitQuantityField = new ImportField(BL.quantityDataListSku);
        unitNetWeightField = new ImportField(BL.netWeightArticle);
        originalNameField = new ImportField(BL.originalNameArticle);
        numberSkuField = new ImportField(BL.numberDataListSku);
    }

    public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        DataObject supplier = keys.get(supplierInterface);
        FormInstance remoteForm = executeForm.form;
        DataSession session = remoteForm.session;

        initFields();

        ImportTable table;
        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            table = createExporter(createTable(inFile)).getTable();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey = new ImportKey(BL.boxInvoice, BL.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, BL.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(supplier, BL.supplierDocument.getMapping(invoiceKey)));

        ImportKey<?> boxKey = new ImportKey(BL.supplierBox, BL.supplierBoxSIDSupplier.getMapping(boxNumberField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, BL.boxInvoiceSupplierBox.getMapping(boxKey), BL.object(BL.boxInvoice).getMapping(invoiceKey)));
        properties.add(new ImportProperty(boxNumberField, BL.sidSupplierBox.getMapping(boxKey)));

        ImportKey<?> articleKey = new ImportKey(BL.articleComposite, BL.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, BL.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, BL.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, BL.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, BL.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, BL.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(BL.item, BL.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, BL.barcode.getMapping(itemKey)));
        properties.add(new ImportProperty(sidField, BL.articleCompositeItem.getMapping(itemKey), BL.object(BL.articleComposite).getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(BL.countrySupplier, BL.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, BL.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, BL.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, BL.countrySupplierOfOriginArticle.getMapping(articleKey), BL.object(BL.countrySupplier).getMapping(countryKey)));

        ImportKey<?> customCategoryKey = new ImportKey(BL.customCategoryOrigin, BL.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, BL.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, BL.customCategoryOriginArticle.getMapping(articleKey),
                BL.object(BL.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(BL.customCategory6, BL.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, BL.sidCustomCategory6.getMapping(customCategory6Key)));

        ImportKey<?> colorKey = new ImportKey(BL.colorSupplier, BL.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, BL.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, BL.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, BL.name.getMapping(colorKey)));
        properties.add(new ImportProperty(colorCodeField, BL.colorSupplierItem.getMapping(itemKey), BL.object(BL.colorSupplier).getMapping(colorKey)));

        ImportKey<?> sizeKey = new ImportKey(BL.sizeSupplier, BL.sizeSIDSupplier.getMapping(sizeField, supplier));
        properties.add(new ImportProperty(sizeField, BL.sidSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(supplier, BL.supplierSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(sizeField, BL.sizeSupplierItem.getMapping(itemKey), BL.object(BL.sizeSupplier).getMapping(sizeKey)));

        properties.add(new ImportProperty(numberSkuField, BL.numberListArticle.getMapping(boxKey, articleKey)));
        properties.add(new ImportProperty(numberSkuField, BL.numberDataListSku.getMapping(boxKey, itemKey)));
        properties.add(new ImportProperty(unitQuantityField, BL.quantityDataListSku.getMapping(boxKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, BL.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, BL.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        ImportKey<?>[] keysArray = {invoiceKey, boxKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key};
        new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

        actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
    }
}
