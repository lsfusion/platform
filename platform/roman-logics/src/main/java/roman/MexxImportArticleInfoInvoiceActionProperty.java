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
 * Date: 03.03.11
 * Time: 16:28
 */

public class MexxImportArticleInfoInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportArticleInfoInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать данные артикулов", LM.mexxSupplier, "dat");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        ImportField sidField = new ImportField(LM.sidArticle);
        ImportField countryField = new ImportField(LM.LM.name);
        ImportField compositionField = new ImportField(LM.mainCompositionOriginArticle);
        ImportField originalNameField = new ImportField(LM.originalNameArticle);

        DataObject supplier = keys.get(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(LM.countrySupplier, LM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, LM.LM.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, LM.countrySupplierOfOriginArticle.getMapping(articleKey), LM.LM.object(LM.countrySupplier).getMapping(countryKey)));

        properties.add(new ImportProperty(compositionField, LM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, LM.originalNameArticle.getMapping(articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            // Заголовки тоже читаем, чтобы определить нужный ли файл импортируется
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 0, '|');

            ImportTable table = new MexxArticleInfoInvoiceImporter(inputTable, null, sidField, originalNameField,
                    10, countryField, compositionField).getTable();

            ImportKey<?>[] keysArray = {articleKey, countryKey};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
