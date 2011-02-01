package tmc.integration.imp;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.IOUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;
import roman.RomanBusinessLogics;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            TnvedFiller filler = new TnvedFiller(executeForm);

            File tempFile = File.createTempFile("tempTnved", ".dbf");
            tempFile.deleteOnExit();
            byte buf[] = (byte[]) value.getValue();
            IOUtils.putFileBytes(tempFile, buf);

            DBF dbfFile = new DBF(tempFile.getPath());
            int recordCount = dbfFile.getRecordCount();

            for (int i = 0; i < recordCount; i++) {
                dbfFile.read();

                String firstField = new String(dbfFile.getField("KOD").getBytes(), "Cp866");

                String secondField = "";
                for (int j = 2; j <= dbfFile.getFieldCount(); j++) {
                    String namePart = new String(dbfFile.getField(j).getBytes(), "Cp866");
                    if (!namePart.startsWith("   ")) {
                        secondField += namePart;
                    } else {
                        break;
                    }
                }

                if (importType.equals("belarusian") && firstField.substring(4).equals("      ")) {
                    filler.add4Item(firstField.substring(0, 4), secondField);
                } else if (importType.equals("belarusian") && firstField.substring(6).equals("    ") && !firstField.substring(5).equals("     ")) {
                    filler.add6Item(firstField.substring(0, 6), secondField);
                } else if (importType.equals("belarusian") && firstField.substring(9).equals(" ") && !firstField.substring(8).equals("  ")) {
                    filler.add9Item(firstField.substring(0, 9), secondField);
                } else if (importType.equals("belarusian") && !firstField.substring(9).equals(" ") && !firstField.equals("··········")) {
                    filler.add10Item(firstField, secondField);
                } else if (importType.equals("origin") && !firstField.substring(9).equals(" ") && !firstField.equals("··········")) {
                    filler.addOriginItem(firstField, secondField);
                }
            }
        } catch (IOException e) {
        } catch (xBaseJException e) {
        } catch (SQLException e) {
        }
    }

    @Override
    protected DataClass getValueClass() {
        return FileActionClass.getInstance("Файл базы данных \"DBF\"", "dbf");
    }

    private class TnvedFiller {
        DataSession session;
        Map<String, Object> map = new HashMap<String, Object>();

        public TnvedFiller(RemoteForm executeForm) {
            FormInstance<?> form = (FormInstance<?>)executeForm.form;
            session = form.session;
        }

        private Object read(LP property, String field) throws SQLException {
            Object object = map.get(field);
            if(object == null) {
                Object newItem = property.read(session, new DataObject(field, StringClass.get(field.length())));
                map.put(field, newItem);
                return newItem;
            } else {
                return object;
            }
        }

        private int add4Item(String field1, String field2) throws SQLException {
            Integer item = (Integer) read(BL.sidToCustomCategory4, field1);
            if (item == null) {
                DataObject category4Object = session.addObject(BL.customCategory4, session.modifier);
                BL.sidCustomCategory4.execute(field1, session, category4Object);
                BL.nameCustomCategory.execute(field2, session, category4Object);
                return (Integer) read(BL.sidToCustomCategory4, field1);
            } else {
                return item;
            }
        }

        private int add6Item(String field1, String field2) throws SQLException {
            Integer item = (Integer) read(BL.sidToCustomCategory6, field1);
            if (item == null) {
                DataObject category6Object = session.addObject(BL.customCategory6, session.modifier);
                BL.sidCustomCategory6.execute(field1, session, category6Object);
                BL.nameCustomCategory.execute(field2, session, category6Object);
                int category4Id = add4Item(field1.substring(0, 4), field2);
                BL.customCategory4CustomCategory6.execute(category4Id, session, category6Object);
                return (Integer) read(BL.sidToCustomCategory6, field1);
            } else {
                int category4Id = add4Item(field1.substring(0, 4), field2);
                DataObject category6Object = session.getDataObject(item, BL.customCategory6.getType());
                Integer category4IdIn6 = (Integer)BL.customCategory4CustomCategory6.read(session, category6Object);
                if(category4IdIn6 == null || category4IdIn6 != category4Id) {
                    BL.customCategory4CustomCategory6.execute(category4Id, session, category6Object);
                }
                return item;
            }
        }

        private int add9Item(String field1, String field2) throws SQLException {
            Integer item = (Integer) read(BL.sidToCustomCategory9, field1);
            if (item == null) {
                DataObject category9Object = session.addObject(BL.customCategory9, session.modifier);
                BL.sidCustomCategory9.execute(field1, session, category9Object);
                BL.nameCustomCategory.execute(field2, session, category9Object);
                int category6Id = add6Item(field1.substring(0, 6), field2);
                BL.customCategory6CustomCategory9.execute(category6Id, session, category9Object);
                return (Integer) read(BL.sidToCustomCategory9, field1);
            } else {
                int category6Id = add6Item(field1.substring(0, 6), field2);
                DataObject category9Object = session.getDataObject(item, BL.customCategory9.getType());
                Integer category6IdIn9 = (Integer)BL.customCategory6CustomCategory9.read(session, category9Object);
                if(category6IdIn9 == null || category6IdIn9 != category6Id) {
                    BL.customCategory6CustomCategory9.execute(category6Id, session, category9Object);
                }
                return item;
            }
        }

        private int add10Item(String field1, String field2) throws SQLException {
            Integer item = (Integer) read(BL.sidToCustomCategory10, field1);
            if (item == null) {
                DataObject category10Object = session.addObject(BL.customCategory10, session.modifier);
                BL.sidCustomCategory10.execute(field1, session, category10Object);
                BL.nameCustomCategory.execute(field2, session, category10Object);
                int category9Id = add9Item(field1.substring(0, 9), field2);
                BL.customCategory9CustomCategory10.execute(category9Id, session, category10Object);
                return (Integer) read(BL.sidToCustomCategory10, field1);
            } else {
                int category9Id = add9Item(field1.substring(0, 9), field2);
                DataObject category10Object = session.getDataObject(item, BL.customCategory10.getType());
                Integer category9IdIn10 = (Integer)BL.customCategory9CustomCategory10.read(session, category10Object);
                if(category9IdIn10 == null || category9IdIn10 != category9Id) {
                    BL.customCategory9CustomCategory10.execute(category9Id, session, category10Object);
                }
                return item;
            }
        }

        private int addOriginItem(String field1, String field2) throws SQLException {
            Integer item = (Integer) read(BL.sidToCustomCategoryOrigin, field1);
            if (item == null) {
                DataObject categoryOriginObject = session.addObject(BL.customCategoryOrigin, session.modifier);
                BL.sidCustomCategoryOrigin.execute(field1, session, categoryOriginObject);
                BL.nameCustomCategory.execute(field2, session, categoryOriginObject);

                int category6Id = add6Item(field1.substring(0, 6), field2);
                BL.customCategory6CustomCategoryOrigin.execute(category6Id, session, categoryOriginObject);

                int category10Id = add10Item(field1, field2);
                BL.customCategory10CustomCategoryOrigin.execute(category10Id, session, categoryOriginObject);

                BL.relationCustomCategory10CustomCategoryOrigin.execute(true, session, new DataObject(category10Id, BL.customCategory10), categoryOriginObject);
                return (Integer) read(BL.sidToCustomCategoryOrigin, field1);
            } else {
                DataObject categoryOriginObject = session.getDataObject(item, BL.customCategoryOrigin.getType());

                int category6Id = add6Item(field1.substring(0, 6), field2);
                Integer category6IdInOrigin = (Integer)BL.customCategory6CustomCategoryOrigin.read(session, categoryOriginObject);
                if(category6IdInOrigin == null || category6IdInOrigin != category6Id) {
                    BL.customCategory6CustomCategoryOrigin.execute(category6Id, session, categoryOriginObject);
                }

                int category10Id = add10Item(field1, field2);
                Integer category10IdInOrigin = (Integer)BL.customCategory10CustomCategoryOrigin.read(session, categoryOriginObject);
                if(category10IdInOrigin == null || category10IdInOrigin != category10Id) {
                    BL.customCategory10CustomCategoryOrigin.execute(category10Id, session, categoryOriginObject);
                    BL.relationCustomCategory10CustomCategoryOrigin.execute(true, session, session.getDataObject(item, BL.customCategory9.getType()), categoryOriginObject);
                }

                return item;
            }
        }
    }
}
