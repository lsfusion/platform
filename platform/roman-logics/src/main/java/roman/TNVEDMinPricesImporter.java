package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.server.form.instance.FormInstance;
import platform.server.integration.*;
import platform.server.logics.ObjectValue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TNVEDMinPricesImporter extends TNVEDImporter {
    private ImportTable table, tableCountry;
    private ImportField category10IdField = new ImportField(LM.sidCustomCategory10);
    private ImportField subcategoryNameField = new ImportField(LM.nameSubCategory);
    private ImportField relationField = new ImportField(LM.relationCustomCategory10SubCategory);
    private ImportField countryIdField = new ImportField(LM.getSidOrigin2Country());
    private ImportField countryNameField = new ImportField(LM.name);
    private ImportField minPriceField = new ImportField(LM.minPriceCustomCategory10SubCategory);

    public TNVEDMinPricesImporter(FormInstance executeForm, ObjectValue value, RomanLogicsModule LM) {
        super(executeForm, value, LM);
    }

    public void doImport() throws IOException, xBaseJException, SQLException {
        category10sids = getFullCategory10();
        readData();

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        List<ImportProperty<?>> propertiesCountry = new ArrayList<ImportProperty<?>>();

        ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(category10IdField));
        ImportKey<?> subcategoryKey = new ImportKey(LM.subCategory, LM.nameToSubCategory.getMapping(subcategoryNameField));
        ImportKey<?> countryKey = new ImportKey(LM.getCountryClass(), LM.sidOrigin2ToCountry.getMapping(countryIdField));

        properties.add(new ImportProperty(subcategoryNameField, LM.nameSubCategory.getMapping(subcategoryKey)));
        properties.add(new ImportProperty(relationField, LM.relationCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
        properties.add(new ImportProperty(minPriceField, LM.minPriceCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
        ImportKey<?>[] keysArray = {category10Key, subcategoryKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize();

        propertiesCountry.add(new ImportProperty(subcategoryNameField, LM.nameSubCategory.getMapping(subcategoryKey)));
        propertiesCountry.add(new ImportProperty(countryIdField, LM.getSidOrigin2Country().getMapping(countryKey)));
        propertiesCountry.add(new ImportProperty(countryNameField, LM.name.getMapping(countryKey)));
        propertiesCountry.add(new ImportProperty(relationField, LM.relationCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
        propertiesCountry.add(new ImportProperty(minPriceField, LM.minPriceCustomCategory10SubCategoryCountry.getMapping(category10Key, subcategoryKey, countryKey)));
        ImportKey<?>[] keysArrayCountry = {category10Key, subcategoryKey, countryKey};
        new IntegrationService(session, tableCountry, Arrays.asList(keysArrayCountry), propertiesCountry).synchronize();
    }

    private void readData() throws IOException, xBaseJException, SQLException {
        File tempFile = File.createTempFile("tempTnved", ".dbf");
        byte buf[] = (byte[]) value.getValue();
        IOUtils.putFileBytes(tempFile, buf);

        DBF file = new DBF(tempFile.getPath());

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> dataCountry = new ArrayList<List<Object>>();

        int recordCount = file.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            file.read();
            String code = new String(file.getField("G33").getBytes(), "Cp866");
            String name = new String(file.getField("NAME").getBytes(), "Cp866");
            Double minPrice = Double.valueOf(new String(file.getField("G46_MD1").getBytes(), "Cp866"));
            String countryCode = new String(file.getField("STRANA").getBytes(), "Cp866");
            String countryName = new String(file.getField("NAME_STRAN").getBytes(), "Cp866");

            if (name.trim().isEmpty()) {
                name = "Группа по умолчанию";
            }

            for (String code10 : getCategory10(code.trim())) {
                List<Object> row = new ArrayList<Object>();
                row.add(code10);
                row.add(name);
                row.add(minPrice);
                row.add(true);
                if (!countryCode.equals("**") && !countryCode.trim().isEmpty()) {
                    row.add(countryCode);
                    row.add(countryName);
                    dataCountry.add(row);
                } else {
                    data.add(row);
                }
            }
        }
        List<ImportField> fields = BaseUtils.toList(category10IdField, subcategoryNameField, minPriceField, relationField);
        table = new ImportTable(fields, data);

        List<ImportField> fieldsCountry = BaseUtils.toList(category10IdField, subcategoryNameField, minPriceField, relationField, countryIdField, countryNameField);
        tableCountry = new ImportTable(fieldsCountry, dataCountry);
        file.close();
        tempFile.delete();
    }
}
