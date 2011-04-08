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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BestsellerImportCompositionActionProperty extends BaseImportActionProperty {
    public BestsellerImportCompositionActionProperty(RomanBusinessLogics BL) {
        super(BL, "Импортировать состав", BL.bestsellerSupplier, "edi txt");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        DataObject supplier = keys.get(supplierInterface);

        ImportField sidField = new ImportField(BL.sidArticle);
        ImportField compositionField = new ImportField(BL.mainCompositionOriginArticle);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> articleKey = new ImportKey(BL.articleComposite, BL.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, BL.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, BL.mainCompositionOriginArticle.getMapping(articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());

            EDIInvoiceImporter importer = new EDIInvoiceImporter(new BestsellerCompositionEDIInputTable(inFile), sidField, compositionField);
            ImportTable table = importer.getTable();
            new IntegrationService(executeForm.form.session, table, Arrays.asList(articleKey), properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
