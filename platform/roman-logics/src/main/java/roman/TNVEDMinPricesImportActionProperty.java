package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
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

public class TNVEDMinPricesImportActionProperty extends ActionProperty {
    private RomanLogicsModule LM;

    public TNVEDMinPricesImportActionProperty(String sID, String caption, RomanLogicsModule LM) {
        super(sID, caption, new ValueClass[]{});
        this.LM = LM;
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions,
                        RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        try {
            TnvedImporter importer = new TnvedImporter(executeForm, value);
            importer.doImport();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        List<String> category10sids;

        private ImportTable table, tableCountry;
        private ImportField category10IdField = new ImportField(LM.sidCustomCategory10);
        private ImportField subcategoryNameField = new ImportField(LM.nameSubCategory);
        private ImportField relationField = new ImportField(LM.relationCustomCategory10SubCategory);
        private ImportField countryIdField = new ImportField(LM.baseLM.sidCountry);
        private ImportField countryNameField = new ImportField(LM.baseLM.name);
        private ImportField minPriceField = new ImportField(LM.minPriceCustomCategory10SubCategory);

        public TnvedImporter(RemoteForm executeForm, ObjectValue value) {
            FormInstance<?> form = (FormInstance<?>) executeForm.form;
            session = form.session;
            this.value = value;
        }

        private List<String> getFullCategory10() throws SQLException {
            List<String> list = new ArrayList<String>();
            Map<Object, KeyExpr> keys = LM.sidCustomCategory10.getMapKeys();
            Expr expr = LM.sidCustomCategory10.property.getExpr(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.properties.put("sid", expr);
            query.and(expr.getWhere());
            query.and(LM.customCategory4CustomCategory10.getExpr(BaseUtils.singleValue(keys)).getWhere());
            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
            for (Map<Object, Object> key : result.keySet()) {
                list.add(result.get(key).get("sid").toString());
            }
            return list;
        }

        public void doImport() throws IOException, xBaseJException, SQLException {
            category10sids = getFullCategory10();
            readData();

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> propertiesCountry = new ArrayList<ImportProperty<?>>();

            ImportKey<?> category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(category10IdField));
            ImportKey<?> subcategoryKey = new ImportKey(LM.subCategory, LM.nameToSubCategory.getMapping(subcategoryNameField));
            ImportKey<?> countryKey = new ImportKey(LM.baseLM.country, LM.baseLM.sidToCountry.getMapping(countryIdField));

            properties.add(new ImportProperty(subcategoryNameField, LM.nameSubCategory.getMapping(subcategoryKey)));
            properties.add(new ImportProperty(relationField, LM.relationCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
            properties.add(new ImportProperty(minPriceField, LM.minPriceCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
            ImportKey<?>[] keysArray = {category10Key, subcategoryKey};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

            propertiesCountry.add(new ImportProperty(subcategoryNameField, LM.nameSubCategory.getMapping(subcategoryKey)));
            propertiesCountry.add(new ImportProperty(countryNameField, LM.baseLM.name.getMapping(countryKey)));
            propertiesCountry.add(new ImportProperty(relationField, LM.relationCustomCategory10SubCategory.getMapping(category10Key, subcategoryKey)));
            propertiesCountry.add(new ImportProperty(minPriceField, LM.minPriceCustomCategory10SubCategoryCountry.getMapping(category10Key, subcategoryKey, countryKey)));
            ImportKey<?>[] keysArrayCountry = {category10Key, subcategoryKey, countryKey};
            new IntegrationService(session, tableCountry, Arrays.asList(keysArrayCountry), propertiesCountry).synchronize(true, true, false);
        }

        private void readData() throws IOException, xBaseJException, SQLException {
            tempFile = File.createTempFile("tempTnved", ".dbf");
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
                    if (!countryCode.equals("***")) {
                        if (!countryCode.trim().equals("")) {
                            row.add(Integer.valueOf(countryCode));
                        } else {
                            row.add(null);
                        }
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
        }

        private List<String> getCategory10(String code) throws SQLException {
            List<String> list = new ArrayList<String>();
            for (String sid10 : category10sids) {
                if (sid10.startsWith(code)) {
                    list.add(sid10);
                }
            }
            return list;
        }
    }
}
