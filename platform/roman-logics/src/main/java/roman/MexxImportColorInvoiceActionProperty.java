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
 * Time: 17:19
 */

public class MexxImportColorInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportColorInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать цвета", LM.mexxSupplier, "dat");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        ImportField colorCodeField = new ImportField(LM.sidColorSupplier);
        ImportField colorNameField = new ImportField(LM.LM.name);

        DataObject supplier = keys.get(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> colorKey = new ImportKey(LM.colorSupplier, LM.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, LM.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, LM.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, LM.LM.name.getMapping(colorKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            // Заголовки тоже читаем, чтобы определить нужный ли файл импортируется
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 0, '|');

            ImportTable table = new SingleSheetImporter(inputTable, 3, colorCodeField, colorNameField) {
                @Override
                protected boolean isCorrectRow(int rowNum) {
                    return (rowNum > 0 && inputTable.getCellString(0, 3).trim().equals("COLOR CODE")
                            && inputTable.getCellString(0, 4).trim().equals("COLOR DESCRIPTION"));
                }
            }.getTable();

            new IntegrationService(executeForm.form.session, table, Arrays.asList(colorKey), properties).synchronize(true, true, false);
            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
