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
        ImportField invoiceSIDField = new ImportField(RomanLM.sidDocument);
        ImportField sidField = new ImportField(RomanLM.sidArticle);
        ImportField dateInvoiceField = new ImportField(RomanLM.date);
        ImportField barCodeField = new ImportField(RomanLM.barcode);
        ImportField customCodeField = new ImportField(RomanLM.sidCustomCategoryOrigin);
        ImportField customCode6Field = new ImportField(RomanLM.sidCustomCategory6);
        ImportField unitPriceField = new ImportField(RomanLM.priceDataDocumentItem);

        DataObject supplier = context.getDataKeyValue(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey = new ImportKey(RomanLM.boxInvoice, RomanLM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, RomanLM.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(dateInvoiceField, RomanLM.date.getMapping(invoiceKey)));

        ImportKey<?> articleKey = new ImportKey(RomanLM.articleComposite, RomanLM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, RomanLM.sidArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(RomanLM.item, RomanLM.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, RomanLM.barcode.getMapping(itemKey)));

        ImportKey<?> customCategoryKey = new ImportKey(RomanLM.customCategoryOrigin, RomanLM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, RomanLM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, RomanLM.customCategoryOriginArticle.getMapping(articleKey),
                RomanLM.object(RomanLM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(RomanLM.customCategory6, RomanLM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, RomanLM.sidCustomCategory6.getMapping(customCategory6Key)));

        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, RomanLM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

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
