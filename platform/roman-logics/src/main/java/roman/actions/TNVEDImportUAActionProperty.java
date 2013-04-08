package roman.actions;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomReadValueActionProperty;
import roman.RomanLogicsModule;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

public class TNVEDImportUAActionProperty extends CustomReadValueActionProperty {
    private RomanLogicsModule LM;
    private final ClassPropertyInterface customsZoneInterface;

    public TNVEDImportUAActionProperty(String sID, String caption, RomanLogicsModule LM) {
        super(sID, caption, new ValueClass[]{LM.getClassByName("CustomsZone")});
        this.LM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customsZoneInterface = i.next();
    }

    private DataClass getFileClass() {
        return CustomStaticFormatFileClass.getDefinedInstance(false, "Файл базы данных \"DBF\"", "dbf");
    }

    protected DataClass getReadType() {
        return getFileClass();
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        try {
            ObjectValue userObjectValue = context.getSession().getObjectValue(getFileClass(), userValue);
            DataObject customsZoneObject = context.getKeyValue(customsZoneInterface);

            File tempFile = File.createTempFile("tempTNVED", ".dbf");
            byte buf[] = (byte[]) userObjectValue.getValue();
            IOUtils.putFileBytes(tempFile, buf);

            DBF file = new DBF(tempFile.getPath());

            List<List<Object>> data = new ArrayList<List<Object>>();

            int recordCount = file.getRecordCount();
            for (int i = 0; i < recordCount; i++) {
                file.read();

                String numberField = new String(file.getField("NUM").getBytes(), "Cp1251").trim();
                String nameField = new String(file.getField("NAME").getBytes(), "Cp1251").trim();

                List<Object> row = new ArrayList<Object>();
                switch (numberField.length()) {
                    case 4:
                    case 6:
                        row.add(null);
                        row.add(null);
                        row.add(null);
                        row.add(null);
                        row.add(numberField);
                        row.add(nameField);
                        data.add(row);
                        break;
                    case 10:
                        row.add(numberField.substring(0, 4));
                        row.add(numberField.substring(0, 6));
                        row.add(numberField.substring(0, 9));
                        row.add(nameField);
                        row.add(numberField);
                        row.add(nameField);
                        data.add(row);
                        break;
                }
            }

            file.close();
            tempFile.delete();

            ImportField cat4IdField = new ImportField(LM.sidCustomCategory4);
            ImportField cat6IdField = new ImportField(LM.sidCustomCategory6);
            ImportField cat9IdField = new ImportField(LM.sidCustomCategory9);
            ImportField cat9NameField = new ImportField(LM.nameCustomCategory);
            ImportField cat10IdField = new ImportField(LM.sidCustomCategory10);
            ImportField cat10NameField = new ImportField(LM.nameCustomCategory);

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> category4Key = new ImportKey(LM.customCategory4, LM.sidToCustomCategory4.getMapping(cat4IdField));
            properties.add(new ImportProperty(cat4IdField, LM.sidCustomCategory4.getMapping(category4Key)));

            ImportKey<?> category6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(cat6IdField));
            properties.add(new ImportProperty(cat6IdField, LM.sidCustomCategory6.getMapping(category6Key)));
            properties.add(new ImportProperty(cat4IdField, LM.customCategory4CustomCategory6.getMapping(category6Key), LM.object(LM.customCategory4).getMapping(category4Key)));

            ImportKey<?> category9Key = new ImportKey(LM.customCategory9, LM.sidToCustomCategory9.getMapping(cat9IdField, customsZoneObject));
            properties.add(new ImportProperty(cat9IdField, LM.sidCustomCategory9.getMapping(category9Key)));
            properties.add(new ImportProperty(cat9NameField, LM.nameCustomCategory.getMapping(category9Key)));
            properties.add(new ImportProperty(cat6IdField, LM.customCategory6CustomCategory9.getMapping(category9Key), LM.object(LM.customCategory6).getMapping(category6Key)));
            properties.add(new ImportProperty(customsZoneObject, LM.customsZoneCustomCategory9.getMapping(category9Key)));

            ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(cat10IdField, customsZoneObject));

            properties.add(new ImportProperty(cat10IdField, LM.sidCustomCategory10.getMapping(category10Key)));
            properties.add(new ImportProperty(cat10NameField, LM.nameCustomCategory.getMapping(category10Key)));
            properties.add(new ImportProperty(cat9IdField, LM.customCategory9CustomCategory10.getMapping(category10Key), LM.object(LM.customCategory9).getMapping(category9Key)));
            properties.add(new ImportProperty(customsZoneObject, LM.customsZoneCustomCategory10.getMapping(category10Key)));

            ImportKey<?>[] keysArray = {category4Key, category6Key, category9Key, category10Key};
            List<ImportField> fields = BaseUtils.toList(cat4IdField, cat6IdField,
                    cat9IdField, cat9NameField, cat10IdField, cat10NameField);
            ImportTable table = new ImportTable(fields, data);
            new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize();

        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}