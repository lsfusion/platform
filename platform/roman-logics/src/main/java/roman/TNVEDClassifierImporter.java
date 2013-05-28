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
import java.util.*;

public class TNVEDClassifierImporter extends TNVEDImporter {
    private ImportTable table;
    private ImportField cat4IdField, cat4NameField, cat6IdField, cat6NameField, cat9IdField, cat9NameField, cat10IdField, cat10NameField, cat10OriginRelationField, numberIdField;
    private Map<String, String> items = new HashMap<String, String>();
    private Map children = new HashMap() {{
        put(4, 6);
        put(6, 9);
        put(9, 10);
    }};

    public TNVEDClassifierImporter(FormInstance executeForm, ObjectValue value, RomanLogicsModule LM, String classifierType) {
        super(executeForm, value, LM, classifierType);
    }

    private void readData() throws xBaseJException, IOException {
        File tempFile = File.createTempFile("tempTnved", ".dbf");
        byte buf[] = (byte[]) value.getValue();
        IOUtils.putFileBytes(tempFile, buf);

        DBF file = new DBF(tempFile.getPath());

        List<List<Object>> data = new ArrayList<List<Object>>();

        List<String> last = new ArrayList<String>(9);    //храним последние прочитанные значения уровней с различным количеством дефисов
        last.addAll(BaseUtils.toList("", "", "", "", "", "", "", "", ""));

        int recordCount = file.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            file.read();

            String idField = new String(file.getField("KOD").getBytes(), "Cp866");

            String nameField = "";
            for (int j = 2; j <= file.getFieldCount(); j++) {
                String namePart = new String(file.getField(j).getBytes(), "Cp866");
                if (!namePart.startsWith("   ")) {
                    nameField += namePart;
                } else {
                    break;
                }
            }

            items.put(idField.trim(), nameField);
            last.set(dashCount(nameField), idField.trim());
            List<Object> row = new ArrayList<Object>();
            if (idField.startsWith("6204")) {
                System.out.println();
            }
            if (!idField.substring(9).equals(" ") && !idField.equals("··········")) {
                row.add(idField);
                row.add(nameField);
                row.add(idField.substring(0, 9));
                row.add(getItem(idField, 9));
                addParents(row, idField);
            } else if (idField.equals("··········")) {
                int dashCount = dashCount(nameField);
                String newCode = generateCode(last.get(dashCount - 1));
                last.set(dashCount, newCode.substring(0, newCode.lastIndexOf("-")));

                row.add(newCode);
                row.add(nameField);
                if (last.get(dashCount).length() == 6) {
                    row.add(newCode);
                    row.add(nameField);
                    addParents(row, newCode);
                } else {
                    addNullParents(row);
                }
            } else {
                row.add(idField);
                row.add(nameField);
                addNullParents(row);
            }

            row.add(true);
            row.add((i + 2) * 100);
            data.add(row);
        }

        List<ImportField> fields = BaseUtils.toList(cat10IdField, cat10NameField, cat9IdField, cat9NameField, cat6IdField,
                cat6NameField, cat4IdField, cat4NameField, cat10OriginRelationField, numberIdField);
        table = new ImportTable(fields, data);
        file.close();
        tempFile.delete();
    }

    private void addParents(List<Object> row, String code) {
        row.add(code.substring(0, 6));
        row.add(getItem(code, 6));
        row.add(code.substring(0, 4));
        row.add(getItem(code, 4));
    }

    private void addNullParents(List<Object> row) {
        for (int i = 0; i < 6; i++) {
            row.add(null);
        }
    }

    Map<String, Integer> counter = new HashMap<String, Integer>();// родители уровней с точками и порядковай номер этих уровней

    private String generateCode(String code) {
        Integer number = counter.get(code);
        if (number == null) {
            counter.put(code, 1);
        } else {
            counter.put(code, number + 1);
        }
        String name = code + "-" + counter.get(code);

        if (name.length() <= 10) {
            return name;
        } else return name.substring(0, 10);
    }

    private int dashCount(String name) {//количество дефисов в названии
        int index = (name.lastIndexOf("- ") + 2) / 2;
        if (index > 8) {
            return dashCount(name.substring(0, index - 1));
        } else {
            return index;
        }
    }

    private String getItem(String id, int size) {
        String item = items.get(id.substring(0, size));
        if (item == null) {
            item = items.get(id.substring(0, (Integer) children.get(size)));
            items.put(id.substring(0, size), item);
        }
        return item;
    }

    private void initFields() {
        cat4IdField = new ImportField(LM.sidCustomCategory4);
        cat4NameField = new ImportField(LM.nameCustomCategory);
        cat6IdField = new ImportField(LM.sidCustomCategory6);
        cat6NameField = new ImportField(LM.nameCustomCategory);
        cat9IdField = new ImportField(LM.sidCustomCategory9);
        cat9NameField = new ImportField(LM.nameCustomCategory);
        cat10IdField = new ImportField(LM.sidCustomCategory10);
        cat10NameField = new ImportField(LM.nameCustomCategory);
        cat10OriginRelationField = new ImportField(LM.relationCustomCategory10CustomCategoryOrigin);
        if (classifierType.equals("origin")) {
            numberIdField = new ImportField(LM.numberIdCustomCategoryOrigin);
        } else {
            numberIdField = new ImportField(LM.numberIdCustomCategory10);
        }
    }

    public void doImport() throws IOException, xBaseJException, SQLException {
        initFields();
        readData();

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        if (classifierType.equals("origin")) {
            ImportKey<?> categoryOriginKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(cat10IdField));
            ImportKey<?> category6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(cat6IdField));
            ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(cat10IdField));
            properties.add(new ImportProperty(cat10IdField, LM.sidCustomCategoryOrigin.getMapping(categoryOriginKey)));
            properties.add(new ImportProperty(cat10NameField, LM.nameCustomCategory.getMapping(categoryOriginKey)));
            properties.add(new ImportProperty(cat6IdField, LM.customCategory6CustomCategoryOrigin.getMapping(categoryOriginKey), LM.object(LM.customCategory6).getMapping(category6Key)));
            properties.add(new ImportProperty(cat10IdField, LM.customCategory10CustomCategoryOrigin.getMapping(categoryOriginKey), LM.object(LM.customCategory10).getMapping(category10Key)));
//            properties.add(new ImportProperty(cat10OriginRelationField, LM.relationCustomCategory10CustomCategoryOrigin.getMapping(category10Key, categoryOriginKey)));
            properties.add(new ImportProperty(numberIdField, LM.numberIdCustomCategoryOrigin.getMapping(categoryOriginKey)));

            ImportKey<?>[] keysArray = {category6Key, category10Key, categoryOriginKey};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize();
            return;
        }

        ImportKey<?> category4Key = new ImportKey(LM.customCategory4, LM.sidToCustomCategory4.getMapping(cat4IdField));
        properties.add(new ImportProperty(cat4IdField, LM.sidCustomCategory4.getMapping(category4Key)));
        properties.add(new ImportProperty(cat4NameField, LM.nameCustomCategory.getMapping(category4Key)));

        ImportKey<?> category6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(cat6IdField));
        properties.add(new ImportProperty(cat6IdField, LM.sidCustomCategory6.getMapping(category6Key)));
        properties.add(new ImportProperty(cat6NameField, LM.nameCustomCategory.getMapping(category6Key)));
        properties.add(new ImportProperty(cat4IdField, LM.customCategory4CustomCategory6.getMapping(category6Key), LM.object(LM.customCategory4).getMapping(category4Key)));

        ImportKey<?> category9Key = new ImportKey(LM.customCategory9, LM.sidToCustomCategory9.getMapping(cat9IdField));
        properties.add(new ImportProperty(cat9IdField, LM.sidCustomCategory9.getMapping(category9Key)));
        properties.add(new ImportProperty(cat9NameField, LM.nameCustomCategory.getMapping(category9Key)));
        properties.add(new ImportProperty(cat6IdField, LM.customCategory6CustomCategory9.getMapping(category9Key), LM.object(LM.customCategory6).getMapping(category6Key)));

        ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(cat10IdField));
        properties.add(new ImportProperty(cat10IdField, LM.sidCustomCategory10.getMapping(category10Key)));
        properties.add(new ImportProperty(cat10NameField, LM.nameCustomCategory.getMapping(category10Key)));
        properties.add(new ImportProperty(cat9IdField, LM.customCategory9CustomCategory10.getMapping(category10Key), LM.object(LM.customCategory9).getMapping(category9Key)));

        properties.add(new ImportProperty(numberIdField, LM.numberIdCustomCategory10.getMapping(category10Key)));

        ImportKey<?>[] keysArray = {category4Key, category6Key, category9Key, category10Key};
        new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize();
    }
}
