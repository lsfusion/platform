package roman;

import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 02.03.11
 * Time: 14:59
 */

public class MexxImportPricesInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportPricesInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, "Импортировать цены", BL.mexxSupplier, "dat");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        ImportField invoiceSIDField = new ImportField(BL.sidDocument);
        ImportField sidField = new ImportField(BL.sidArticle);
        ImportField barCodeField = new ImportField(BL.barcode);
        ImportField customCodeField = new ImportField(BL.sidCustomCategoryOrigin);
        ImportField customCode6Field = new ImportField(BL.sidCustomCategory6);
        ImportField unitPriceField = new ImportField(BL.priceDataDocumentItem);

        DataObject supplier = keys.get(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey = new ImportKey(BL.boxInvoice, BL.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, BL.sidDocument.getMapping(invoiceKey)));

        ImportKey<?> articleKey = new ImportKey(BL.articleComposite, BL.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, BL.sidArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(BL.item, BL.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, BL.barcode.getMapping(itemKey)));

        ImportKey<?> customCategoryKey = new ImportKey(BL.customCategoryOrigin, BL.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, BL.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, BL.customCategoryOriginArticle.getMapping(articleKey),
                BL.object(BL.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(BL.customCategory6, BL.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, BL.sidCustomCategory6.getMapping(customCategory6Key)));

        properties.add(new ImportProperty(unitPriceField, BL.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, BL.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 1, '|');

            ImportTable table = new MexxPricesInvoiceImporter(inputTable, null, invoiceSIDField, null, null, null, null, sidField, null, null,
                    null, unitPriceField, null, barCodeField, null, new ImportField[] {customCodeField, customCode6Field}).getTable();

            ImportKey<?>[] keysArray = {invoiceKey, articleKey, itemKey, customCategoryKey, customCategory6Key};
            new IntegrationService(executeForm.form.session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
