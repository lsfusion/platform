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
import java.sql.SQLException;
import java.util.*;

public class PricatEDIImportActionProperty extends ActionProperty {
    private RomanBusinessLogics BL;

    private final FileActionClass valueClass = FileActionClass.getDefinedInstance(true, "Файлы данных (*.edi, *.txt)", "edi txt *.*");

    public PricatEDIImportActionProperty(String sID, RomanBusinessLogics BL, ValueClass supplier) {
        super(sID, "Импортировать прайс (EDI)", new ValueClass[]{supplier});
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
        ImportField customCategoryOriginalField = new ImportField(BL.customCategoryOriginalPricat);
        ImportField colorCodeField = new ImportField(BL.colorCodePricat);
        ImportField colorField = new ImportField(BL.colorNamePricat);
        ImportField sizeField = new ImportField(BL.sizePricat);
        ImportField originalNameField = new ImportField(BL.originalNamePricat);
        ImportField countryField = new ImportField(BL.countryPricat);
        ImportField netWeightField = new ImportField(BL.netWeightPricat);
        ImportField compositionField = new ImportField(BL.compositionPricat);
        ImportField priceField = new ImportField(BL.pricePricat);
        ImportField rrpField = new ImportField(BL.rrpPricat);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        ImportKey<?> pricatKey = new ImportKey(BL.pricat, BL.barcodeToPricat.getMapping(barcodeField));
        properties.add(new ImportProperty(barcodeField, BL.barcodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(articleField, BL.articleNumberPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(customCategoryOriginalField, BL.customCategoryOriginalPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorCodeField, BL.colorCodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorField, BL.colorNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(sizeField, BL.sizePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(originalNameField, BL.originalNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(countryField, BL.countryPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(netWeightField, BL.netWeightPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(compositionField, BL.compositionPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(priceField, BL.pricePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(rrpField, BL.rrpPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(supplier, BL.supplierPricat.getMapping(pricatKey)));

        for (byte[] file : fileList) {

            ImportKey<?>[] keysArray = {pricatKey};

            try {
                PricatEDIInputTable inputTable = new PricatEDIInputTable(new ByteArrayInputStream(file));

                ImportTable table = new EDIInvoiceImporter(inputTable, barcodeField, articleField, customCategoryOriginalField, colorCodeField, colorField,
                        sizeField, originalNameField, countryField, netWeightField, compositionField, priceField, rrpField).getTable();

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
