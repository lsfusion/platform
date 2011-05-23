package roman;

import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

public class HugoBossPricatCSVImportActionProperty extends ActionProperty {
    private RomanBusinessLogics BL;

    private final FileActionClass valueClass = FileActionClass.getDefinedInstance(true, "Файлы данных (*.csv)", "csv");

    public HugoBossPricatCSVImportActionProperty(String sID, RomanBusinessLogics BL, ValueClass supplier) {
        super(sID, "Импортировать прайс (CSV)", new ValueClass[]{supplier});
        this.BL = BL;
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        ClassPropertyInterface supplierInterface = i.next();
        DataObject supplier = keys.get(supplierInterface);

        List<byte[]> fileList = valueClass.getFiles(value.getValue());

        ImportField barcodeField = new ImportField(BL.barcodePricat);
        ImportField articleField = new ImportField(BL.articleNumberPricat);
        ImportField colorCodeField = new ImportField(BL.colorCodePricat);
        ImportField sizeField = new ImportField(BL.sizePricat);
        ImportField priceField = new ImportField(BL.pricePricat);
        ImportField compositionField = new ImportField(BL.compositionPricat);
        ImportField customCategoryOriginalField = new ImportField(BL.customCategoryOriginalPricat);
        ImportField colorField = new ImportField(BL.colorNamePricat);
        ImportField netWeightField = new ImportField(BL.netWeightPricat);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        ImportKey<?> pricatKey = new ImportKey(BL.pricat, BL.barcodeToPricat.getMapping(barcodeField));
        properties.add(new ImportProperty(barcodeField, BL.barcodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(articleField, BL.articleNumberPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorCodeField, BL.colorCodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(sizeField, BL.sizePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(priceField, BL.pricePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(compositionField, BL.compositionPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(customCategoryOriginalField, BL.customCategoryOriginalPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorField, BL.colorNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(netWeightField, BL.netWeightPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(supplier, BL.supplierPricat.getMapping(pricatKey)));

        for (byte[] file : fileList) {
            ImportKey<?>[] keysArray = {pricatKey};
            try {
                CSVInputTable inputTable = new CSVInputTable(new InputStreamReader(new ByteArrayInputStream(file)), 2, ';');
                ImportTable table = new HugoBossPricatCSVImporter(inputTable, new Object[] {null, articleField, null, colorCodeField,
                        5, barcodeField, sizeField, priceField, 15, compositionField, 21, customCategoryOriginalField, 28, colorField,
                        32, netWeightField}).getTable();
                new IntegrationService(executeForm.form.session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

    @Override
    public DataClass getValueClass() {
        return valueClass;
    }
}
