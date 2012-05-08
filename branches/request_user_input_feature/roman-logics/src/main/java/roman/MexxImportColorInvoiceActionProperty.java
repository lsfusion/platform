package roman;

import platform.interop.action.MessageClientAction;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    protected void executeRead(ExecutionContext context, Object userValue) throws SQLException {
        ImportField colorCodeField = new ImportField(LM.sidColorSupplier);
        ImportField colorNameField = new ImportField(LM.baseLM.name);

        DataObject supplier = context.getKeyValue(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> colorKey = new ImportKey(LM.colorSupplier, LM.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, LM.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, LM.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, LM.baseLM.name.getMapping(colorKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) userValue);
            // Заголовки тоже читаем, чтобы определить нужный ли файл импортируется
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 0, '|');

            ImportTable table = new SingleSheetImporter(inputTable, 3, colorCodeField, colorNameField) {
                @Override
                protected boolean isCorrectRow(int rowNum) {
                    return (rowNum > 0 && inputTable.getCellString(0, 3).trim().equals("COLOR CODE")
                            && inputTable.getCellString(0, 4).trim().equals("COLOR DESCRIPTION"));
                }
            }.getTable();

            new IntegrationService(context.getSession(), table, Arrays.asList(colorKey), properties).synchronize();
            context.addAction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
