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
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ClassifierTNVEDImportActionProperty extends ActionProperty {
    private RomanLogicsModule LM;
    private String importType;

    public ClassifierTNVEDImportActionProperty(String sID, String caption, RomanLogicsModule LM, String importType) {
        super(sID, caption, new ValueClass[]{});
        this.LM = LM;
        this.importType = importType;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions,
                        RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) {
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
        return FileActionClass.getDefinedInstance(false, "Файл базы данных \"DBF\"", "dbf");
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
            cat4IdField = new ImportField(LM.sidCustomCategory4);
            cat4NameField = new ImportField(LM.nameCustomCategory);
            cat6IdField = new ImportField(LM.sidCustomCategory6);
            cat6NameField = new ImportField(LM.nameCustomCategory);
            cat9IdField = new ImportField(LM.sidCustomCategory9);
            cat9NameField = new ImportField(LM.nameCustomCategory);
            cat10IdField = new ImportField(LM.sidCustomCategory10);
            cat10NameField = new ImportField(LM.nameCustomCategory);
            cat10OriginRelationField = new ImportField(LM.relationCustomCategory10CustomCategoryOrigin);
        }

        public void doImport() throws IOException, xBaseJException, SQLException {
            initFields();
            readData();

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

            if (importType.equals("origin")) {
                ImportKey<?> categoryOriginKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(cat10IdField));
                ImportKey<?> category6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(cat6IdField));
                ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(cat10IdField));
                properties.add(new ImportProperty(cat10IdField, LM.sidCustomCategoryOrigin.getMapping(categoryOriginKey)));
                properties.add(new ImportProperty(cat10NameField, LM.nameCustomCategory.getMapping(categoryOriginKey)));
                properties.add(new ImportProperty(cat6IdField, LM.customCategory6CustomCategoryOrigin.getMapping(categoryOriginKey), LM.object(LM.customCategory6).getMapping(category6Key)));
                properties.add(new ImportProperty(cat10IdField, LM.customCategory10CustomCategoryOrigin.getMapping(categoryOriginKey), LM.object(LM.customCategory10).getMapping(category10Key)));
                properties.add(new ImportProperty(cat10OriginRelationField, LM.relationCustomCategory10CustomCategoryOrigin.getMapping(category10Key, categoryOriginKey)));

                ImportKey<?>[] keysArray = {category6Key, category10Key, categoryOriginKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
                tempFile.delete();
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

            ImportKey<?>[] keysArray = {category4Key, category6Key, category9Key, category10Key};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
            tempFile.delete();
        }
    }
}
