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
 * Date: 03.03.11
 * Time: 16:28
 */

public class MexxImportArticleInfoInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportArticleInfoInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, "Импортировать данные артикулов", BL.mexxSupplier, "dat");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        ImportField sidField = new ImportField(BL.sidArticle);
        ImportField countryField = new ImportField(BL.name);
        ImportField compositionField = new ImportField(BL.mainCompositionOriginArticle);
        ImportField originalNameField = new ImportField(BL.originalNameArticle);

        DataObject supplier = keys.get(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> articleKey = new ImportKey(BL.articleComposite, BL.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, BL.sidArticle.getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(BL.countrySupplier, BL.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, BL.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, BL.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, BL.countrySupplierOfOriginArticle.getMapping(articleKey), BL.object(BL.countrySupplier).getMapping(countryKey)));

        properties.add(new ImportProperty(compositionField, BL.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, BL.originalNameArticle.getMapping(articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            // Заголовки тоже читаем, чтобы определить нужный ли файл импортируется
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 0, '|');

            ImportTable table = new MexxArticleInfoInvoiceImporter(inputTable, null, sidField, originalNameField,
                    10, countryField, compositionField).getTable();

            ImportKey<?>[] keysArray = {articleKey, countryKey};
            new IntegrationService(executeForm.form.session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
