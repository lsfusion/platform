package roman;

import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

public class HugoBossPricatCSVImportActionProperty extends ActionProperty {
    private RomanLogicsModule LM;

    private final FileActionClass valueClass = FileActionClass.getDefinedInstance(true, "Файлы данных (*.csv)", "csv");

    public HugoBossPricatCSVImportActionProperty(String sID, RomanLogicsModule LM, ValueClass supplier) {
        super(sID, "Импортировать прайс (CSV)", new ValueClass[]{supplier});
        this.LM = LM;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        ClassPropertyInterface supplierInterface = i.next();
        DataObject supplier = context.getKeyValue(supplierInterface);

        List<byte[]> fileList = valueClass.getFiles(context.getValueObject());

        ImportField barcodeField = new ImportField(LM.barcodePricat);
        ImportField articleField = new ImportField(LM.articleNumberPricat);
        ImportField colorCodeField = new ImportField(LM.colorCodePricat);
        ImportField sizeField = new ImportField(LM.sizePricat);
        ImportField priceField = new ImportField(LM.pricePricat);
        ImportField compositionField = new ImportField(LM.compositionPricat);
        ImportField customCategoryOriginalField = new ImportField(LM.customCategoryOriginalPricat);
        ImportField colorField = new ImportField(LM.colorNamePricat);
        ImportField netWeightField = new ImportField(LM.netWeightPricat);
        ImportField genderField = new ImportField(LM.genderPricat);
        ImportField seasonField = new ImportField(LM.seasonPricat);


        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        ImportKey<?> pricatKey = new ImportKey(LM.pricat, LM.barcodeToPricat.getMapping(barcodeField));
        properties.add(new ImportProperty(barcodeField, LM.barcodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(articleField, LM.articleNumberPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorCodeField, LM.colorCodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(sizeField, LM.sizePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(priceField, LM.pricePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(compositionField, LM.compositionPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(customCategoryOriginalField, LM.customCategoryOriginalPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorField, LM.colorNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(netWeightField, LM.netWeightPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(genderField, LM.genderPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(seasonField, LM.seasonPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(supplier, LM.supplierPricat.getMapping(pricatKey)));

        for (byte[] file : fileList) {
            ImportKey<?>[] keysArray = {pricatKey};
            try {
                CSVInputTable inputTable = new CSVInputTable(new InputStreamReader(new ByteArrayInputStream(file)), 2, ';');
                ImportTable table = new HugoBossPricatCSVImporter(inputTable, new Object[] {null, articleField, null, colorCodeField,
                        5, barcodeField, sizeField, priceField, 15, compositionField, 21, customCategoryOriginalField, 28, colorField,
                        32, netWeightField, 34, genderField, 49, seasonField}).getTable();
                new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize();
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
