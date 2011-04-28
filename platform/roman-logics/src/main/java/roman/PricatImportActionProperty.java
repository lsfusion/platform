package roman;

import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.ParseException;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.*;

public class PricatImportActionProperty extends ActionProperty {
    private RomanBusinessLogics BL;

    private final FileActionClass valueClass = FileActionClass.getDefinedInstance(true, "Файл данных (*.edi, *.txt)", "edi txt");

    public PricatImportActionProperty(String sID, RomanBusinessLogics BL, ValueClass supplier) {
        super(sID, "Импортировать прайс", new ValueClass[]{supplier});
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
        ImportField colorField = new ImportField(BL.colorNamePricat);
        ImportField sizeField = new ImportField(BL.sizePricat);
        ImportField originalNameField = new ImportField(BL.originalNamePricat);
        ImportField countryField = new ImportField(BL.countryPricat);
        ImportField netWeightField = new ImportField(BL.netWeightPricat);
        ImportField compositionField = new ImportField(BL.compositionPricat);
        ImportField priceField = new ImportField(BL.pricePricat);
        ImportField rrpField = new ImportField(BL.rrpPricat);

        for (byte[] file : fileList) {
            PricatEDIInputTable inputTable = new PricatEDIInputTable(new ByteArrayInputStream(file));
            PricatImporter importer = new PricatImporter(executeForm.form.session, inputTable, supplier, barcodeField, articleField, colorCodeField, colorField,
                    sizeField, originalNameField, countryField, netWeightField, compositionField, priceField, rrpField);
            try {
                importer.doImport();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

    }

    private class PricatImporter extends SingleSheetImporter {
        ImportField[] fields;
        DataSession session;
        DataObject supplier;

        public PricatImporter(DataSession session, ImportInputTable inputTable, DataObject supplier, ImportField... fields) {
            super(inputTable, fields);
            this.session = session;
            this.fields = fields;
            this.supplier = supplier;
        }

        public void doImport() throws ParseException, java.text.ParseException, SQLException {
            ImportTable table = getTable();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            ImportKey<?> pricatKey = new ImportKey(BL.pricat, BL.barcodeToPricat.getMapping(fields[0]));
            properties.add(new ImportProperty(fields[0], BL.barcodePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[1], BL.articleNumberPricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[2], BL.colorCodePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[3], BL.colorNamePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[4], BL.sizePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[5], BL.originalNamePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[6], BL.countryPricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[7], BL.netWeightPricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[8], BL.compositionPricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[9], BL.pricePricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(fields[10], BL.rrpPricat.getMapping(pricatKey)));
            properties.add(new ImportProperty(supplier, BL.supplierPricat.getMapping(pricatKey)));

            ImportKey<?>[] keysArray = {pricatKey};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
        }

        @Override
        protected boolean isCorrectRow(int rowNum) {
            return true;
        }
    }

    @Override
    public DataClass getValueClass() {
        return valueClass;
    }
}
