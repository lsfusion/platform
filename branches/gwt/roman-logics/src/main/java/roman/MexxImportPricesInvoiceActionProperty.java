package roman;

import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

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
    public MexxImportPricesInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать цены", LM.mexxSupplier, "dat");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        ImportField invoiceSIDField = new ImportField(LM.sidDocument);
        ImportField sidField = new ImportField(LM.sidArticle);
        ImportField barCodeField = new ImportField(LM.baseLM.barcode);
        ImportField customCodeField = new ImportField(LM.sidCustomCategoryOrigin);
        ImportField customCode6Field = new ImportField(LM.sidCustomCategory6);
        ImportField unitPriceField = new ImportField(LM.priceDataDocumentItem);

        DataObject supplier = keys.get(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey = new ImportKey(LM.boxInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        properties.add(new ImportProperty(invoiceSIDField, LM.sidDocument.getMapping(invoiceKey)));

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(LM.item, LM.baseLM.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, LM.baseLM.barcode.getMapping(itemKey)));

        ImportKey<?> customCategoryKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, LM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, LM.customCategoryOriginArticle.getMapping(articleKey),
                LM.object(LM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, LM.sidCustomCategory6.getMapping(customCategory6Key)));

        properties.add(new ImportProperty(unitPriceField, LM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(unitPriceField, LM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 1, '|');

            ImportTable table = new MexxPricesInvoiceImporter(inputTable, null, invoiceSIDField, null, null, null, null, sidField, null, null,
                    null, unitPriceField, null, barCodeField, null, new ImportField[] {customCodeField, customCode6Field}).getTable();

            ImportKey<?>[] keysArray = {invoiceKey, articleKey, itemKey, customCategoryKey, customCategory6Key};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
