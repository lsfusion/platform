package roman;

import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomReadValueActionProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

public class GerryWeberPricatCSVImportActionProperty extends CustomReadValueActionProperty {
    private RomanLogicsModule LM;

    private final CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(true, false, "Файлы данных (*.txt)", "txt");

    public GerryWeberPricatCSVImportActionProperty(String sID, RomanLogicsModule LM, ValueClass supplier) {
        super(sID, "Импортировать прайс (CSV)", new ValueClass[]{supplier});
        this.LM = LM;
    }


    @Override
    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        ClassPropertyInterface supplierInterface = i.next();
        DataObject supplier = context.getDataKeyValue(supplierInterface);

        List<byte[]> fileList = valueClass.getFiles(userValue);

        ImportField barcodeField = new ImportField(LM.barcodePricat);
        ImportField articleField = new ImportField(LM.articleNumberPricat);
        ImportField originalNameField = new ImportField(LM.originalNamePricat);
        ImportField colorCodeField = new ImportField(LM.colorCodePricat);
        ImportField colorField = new ImportField(LM.colorNamePricat);
        ImportField sizeField = new ImportField(LM.sizePricat);
        ImportField customCategoryOriginalField = new ImportField(LM.customCategoryOriginalPricat);
        ImportField compositionField = new ImportField(LM.compositionPricat);
        ImportField priceField = new ImportField(LM.pricePricat);
        ImportField seasonField = new ImportField(LM.seasonPricat);
        ImportField themeCodeField = new ImportField(LM.themeCodePricat);
        ImportField themeNameField = new ImportField(LM.themeNamePricat);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        ImportKey<?> pricatKey = new ImportKey(LM.pricat, LM.barcodeToPricat.getMapping(barcodeField));
        properties.add(new ImportProperty(barcodeField, LM.barcodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(seasonField, LM.seasonPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(articleField, LM.articleNumberPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(originalNameField, LM.originalNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorCodeField, LM.colorCodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(colorField, LM.colorNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(sizeField, LM.sizePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(customCategoryOriginalField, LM.customCategoryOriginalPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(themeCodeField, LM.themeCodePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(themeNameField, LM.themeNamePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(compositionField, LM.compositionPricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(priceField, LM.pricePricat.getMapping(pricatKey)));
        properties.add(new ImportProperty(supplier, LM.supplierPricat.getMapping(pricatKey)));



        for (byte[] file : fileList) {
            ImportKey<?>[] keysArray = {pricatKey};
            try {
                CSVInputTable inputTable = new CSVInputTable(new InputStreamReader(new ByteArrayInputStream(file)), 0, ';');
                ImportTable table = new GerryWeberPricatCSVImporter(inputTable, new Object[] {null, null, seasonField, articleField, originalNameField, colorCodeField,
                                        colorField, sizeField, barcodeField, themeCodeField, themeNameField, 13, customCategoryOriginalField, 14, compositionField, 16, priceField}).getTable();



                new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    }

    @Override
    protected DataClass getReadType() {
        return valueClass;
    }
}