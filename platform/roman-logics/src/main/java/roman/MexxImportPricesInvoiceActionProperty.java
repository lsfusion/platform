package roman;

import platform.interop.action.MessageClientAction;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: DAle
 * Date: 02.03.11
 * Time: 14:59
 */

public class MexxImportPricesInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportPricesInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать цены", LM.mexxSupplier, "dat");
    }

    @Override
    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        ImportField invoiceSIDField = new ImportField(LM.sidDocument);
        ImportField sidField = new ImportField(LM.sidArticle);
        ImportField dateInvoiceField = new ImportField(LM.date);
        ImportField barCodeField = new ImportField(LM.barcode);
        ImportField customCodeField = new ImportField(LM.sidCustomCategoryOrigin);
        ImportField customCode6Field = new ImportField(LM.sidCustomCategory6);
        ImportField unitPriceField = new ImportField(LM.priceDataDocumentItem);

        DataObject supplier = context.getKeyValue(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey = new ImportKey(LM.boxInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, LM.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(dateInvoiceField, LM.date.getMapping(invoiceKey)));

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(LM.item, LM.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, LM.barcode.getMapping(itemKey)));

        ImportKey<?> customCategoryKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, LM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, LM.customCategoryOriginArticle.getMapping(articleKey),
                LM.object(LM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, LM.sidCustomCategory6.getMapping(customCategory6Key)));

        properties.add(new ImportProperty(unitPriceField, LM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, LM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) userValue);
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 1, '|');

            ImportTable table = new MexxPricesInvoiceImporter(inputTable, null, invoiceSIDField, dateInvoiceField, null, null, null, sidField, null, null,
                    null, unitPriceField, null, barCodeField, null, new ImportField[] {customCodeField, customCode6Field}).getTable();

            ImportKey<?>[] keysArray = {invoiceKey, articleKey, itemKey, customCategoryKey, customCategory6Key};
            new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize();

            context.delayUserInterfaction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
