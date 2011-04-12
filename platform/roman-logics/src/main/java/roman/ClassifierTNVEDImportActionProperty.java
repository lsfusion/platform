package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ClassifierTNVEDImportActionProperty extends ActionProperty {
    private RomanBusinessLogics BL;
    private String importType;

    public ClassifierTNVEDImportActionProperty(String sID, String caption, RomanBusinessLogics BL, String importType) {
        super(sID, caption, new ValueClass[]{});
        this.BL = BL;
        this.importType = importType;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions,
                        RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        try {
            TnvedImporter importer = new TnvedImporter(executeForm, value);
            importer.doImport();
        } catch (xBaseJException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getInstance("Файл базы данных \"DBF\"", "dbf");
    }

    private class TnvedImporter {
        private DataSession session;
        private ObjectValue value;
        private File tempFile;

        private ImportTable table;
        private ImportField cat4IdField, cat4NameField, cat6IdField, cat6NameField, cat9IdField, cat9NameField, cat10IdField, cat10NameField, cat10OriginRelationField;
        private Map<String, String> items = new HashMap<String, String>();
        private Map children = new HashMap() {{
            put(4, 6);
            put(6, 9);
            put(9, 10);
        }};

        public TnvedImporter(RemoteForm executeForm, ObjectValue value) {
            FormInstance<?> form = (FormInstance<?>) executeForm.form;
            session = form.session;
            this.value = value;
        }

        private void readData() throws xBaseJException, IOException {
            tempFile = File.createTempFile("tempTnved", ".dbf");
            byte buf[] = (byte[]) value.getValue();
            IOUtils.putFileBytes(tempFile, buf);

            DBF file = new DBF(tempFile.getPath());

            List<List<Object>> data = new ArrayList<List<Object>>();

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
                if (!idField.substring(9).equals(" ") && !idField.equals("··········")) {
                    List<Object> row = new ArrayList<Object>();
                    row.add(idField);
                    row.add(nameField);
                    row.add(idField.substring(0, 9));
                    row.add(getItem(idField, 9));
                    row.add(idField.substring(0, 6));
                    row.add(getItem(idField, 6));
                    row.add(idField.substring(0, 4));
                    row.add(getItem(idField, 4));
                    row.add(true);
                    data.add(row);
                }
            }

            List<ImportField> fields = BaseUtils.toList(cat10IdField, cat10NameField, cat9IdField, cat9NameField, cat6IdField,
                    cat6NameField, cat4IdField, cat4NameField, cat10OriginRelationField);
            table = new ImportTable(fields, data);
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
            cat4IdField = new ImportField(BL.sidCustomCategory4);
            cat4NameField = new ImportField(BL.nameCustomCategory);
            cat6IdField = new ImportField(BL.sidCustomCategory6);
            cat6NameField = new ImportField(BL.nameCustomCategory);
            cat9IdField = new ImportField(BL.sidCustomCategory9);
            cat9NameField = new ImportField(BL.nameCustomCategory);
            cat10IdField = new ImportField(BL.sidCustomCategory10);
            cat10NameField = new ImportField(BL.nameCustomCategory);
            cat10OriginRelationField = new ImportField(BL.relationCustomCategory10CustomCategoryOrigin);
        }

        public void doImport() throws IOException, xBaseJException, SQLException {
            initFields();
            readData();

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

            if (importType.equals("origin")) {
                ImportKey<?> categoryOriginKey = new ImportKey(BL.customCategoryOrigin, BL.sidToCustomCategoryOrigin.getMapping(cat10IdField));
                ImportKey<?> category6Key = new ImportKey(BL.customCategory6, BL.sidToCustomCategory6.getMapping(cat6IdField));
                ImportKey<?> category10Key = new ImportKey(BL.customCategory10, BL.sidToCustomCategory10.getMapping(cat10IdField));
                properties.add(new ImportProperty(cat10IdField, BL.sidCustomCategoryOrigin.getMapping(categoryOriginKey)));
                properties.add(new ImportProperty(cat10NameField, BL.nameCustomCategory.getMapping(categoryOriginKey)));
                properties.add(new ImportProperty(cat6IdField, BL.customCategory6CustomCategoryOrigin.getMapping(categoryOriginKey), BL.object(BL.customCategory6).getMapping(category6Key)));
                properties.add(new ImportProperty(cat10IdField, BL.customCategory10CustomCategoryOrigin.getMapping(categoryOriginKey), BL.object(BL.customCategory10).getMapping(category10Key)));
                properties.add(new ImportProperty(cat10OriginRelationField, BL.relationCustomCategory10CustomCategoryOrigin.getMapping(category10Key, categoryOriginKey)));

                ImportKey<?>[] keysArray = {category6Key, category10Key, categoryOriginKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
                tempFile.delete();
                return;
            }

            ImportKey<?> category4Key = new ImportKey(BL.customCategory4, BL.sidToCustomCategory4.getMapping(cat4IdField));
            properties.add(new ImportProperty(cat4IdField, BL.sidCustomCategory4.getMapping(category4Key)));
            properties.add(new ImportProperty(cat4NameField, BL.nameCustomCategory.getMapping(category4Key)));

            ImportKey<?> category6Key = new ImportKey(BL.customCategory6, BL.sidToCustomCategory6.getMapping(cat6IdField));
            properties.add(new ImportProperty(cat6IdField, BL.sidCustomCategory6.getMapping(category6Key)));
            properties.add(new ImportProperty(cat6NameField, BL.nameCustomCategory.getMapping(category6Key)));
            properties.add(new ImportProperty(cat4IdField, BL.customCategory4CustomCategory6.getMapping(category6Key), BL.object(BL.customCategory4).getMapping(category4Key)));

            ImportKey<?> category9Key = new ImportKey(BL.customCategory9, BL.sidToCustomCategory9.getMapping(cat9IdField));
            properties.add(new ImportProperty(cat9IdField, BL.sidCustomCategory9.getMapping(category9Key)));
            properties.add(new ImportProperty(cat9NameField, BL.nameCustomCategory.getMapping(category9Key)));
            properties.add(new ImportProperty(cat6IdField, BL.customCategory6CustomCategory9.getMapping(category9Key), BL.object(BL.customCategory6).getMapping(category6Key)));

            ImportKey<?> category10Key = new ImportKey(BL.customCategory10, BL.sidToCustomCategory10.getMapping(cat10IdField));
            properties.add(new ImportProperty(cat10IdField, BL.sidCustomCategory10.getMapping(category10Key)));
            properties.add(new ImportProperty(cat10NameField, BL.nameCustomCategory.getMapping(category10Key)));
            properties.add(new ImportProperty(cat9IdField, BL.customCategory9CustomCategory10.getMapping(category10Key), BL.object(BL.customCategory9).getMapping(category9Key)));

            ImportKey<?>[] keysArray = {category4Key, category6Key, category9Key, category10Key};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
            tempFile.delete();
        }
    }
}
