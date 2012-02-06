package rublevski;

import net.sf.jasperreports.engine.JRException;
import platform.interop.action.MessageClientAction;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RublevskiBusinessLogics extends BusinessLogics<RublevskiBusinessLogics> {
    ScriptingLogicsModule rublevskiLM;

    public RublevskiBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        rublevskiLM = new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Rublevski.lsf"), LM, this);
        addLogicsModule(rublevskiLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    LP importData;

    @Override
    protected void initModules() throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException {
        super.initModules();

        importData = rublevskiLM.addAProp(new ImportDataActionProperty());
        rublevskiLM.addFormEntity(new ImportDataFormEntity(LM.adminElement, "importData", "Импорт данных"));
    }

    private class ImportDataActionProperty extends ActionProperty {
        
        private ImportDataActionProperty() {
            super("importData", "Импортировать данные", new ValueClass[] {});

            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {

            Connection conn = null;
            
            try {
                // Get a connection to the database
                conn = DriverManager.getConnection(((String)rublevskiLM.getLPByName("importUrl").read(context)).trim(),
                                                   ((String)rublevskiLM.getLPByName("importLogin").read(context)).trim(),
                                                   ((String)rublevskiLM.getLPByName("importPassword").read(context)).trim());

                importItemGroup(context, conn);

                // Close the result set, statement and the connection
            } catch (SQLException e) {
                context.addAction(new MessageClientAction("Ошибка при подключении к базе данных : " + e.getLocalizedMessage(), "Импорт данных"));
            } finally {
                if (conn != null)
                    conn.close() ;
            }
        }

        private void importItemGroup(ExecutionContext context, Connection conn) throws SQLException {

            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT num_class AS ext_id, name_u AS name, par AS par_id FROM klass") ;

            ImportField itemGroupID = new ImportField(LM.extSID);
            ImportField itemGroupName = new ImportField(LM.name);
            ImportField parentGroupID = new ImportField(LM.extSID);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass)rublevskiLM.getClassByName("itemGroup"),
                                                   LM.extSIDToObject.getMapping(itemGroupID));
            ImportProperty<?> itemGroupIDProperty = new ImportProperty(itemGroupID, LM.extSID.getMapping(itemGroupKey));
            ImportProperty<?> itemGroupNameProperty = new ImportProperty(itemGroupName, LM.name.getMapping(itemGroupKey));

            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass)rublevskiLM.getClassByName("itemGroup"),
                                                   LM.extSIDToObject.getMapping(parentGroupID));

            ImportProperty<?> parentGroupProperty = new ImportProperty(parentGroupID, rublevskiLM.getLPByName("parentItemGroup").getMapping(itemGroupKey),
                                                   LM.object((ConcreteCustomClass)rublevskiLM.getClassByName("itemGroup")).getMapping(parentGroupKey));

            Collection<? extends ImportKey<?>> keys = Arrays.asList(itemGroupKey, parentGroupKey);
            Collection<ImportProperty<?>> properties = Arrays.asList(itemGroupIDProperty, itemGroupNameProperty, parentGroupProperty);

            new IntegrationService(context.getSession(),
                    new ImportTable(Arrays.asList(itemGroupID, itemGroupName, parentGroupID), createData(rs)),
                    keys,
                    properties).synchronize();
        }
        
        private List<List<Object>> createData(ResultSet rs) throws SQLException {

            ResultSetMetaData rsmd = rs.getMetaData();

            int columnCount = rsmd.getColumnCount();

            List<List<Object>> data = new ArrayList<List<Object>>();
            while (rs.next()) {
                List<Object> row = new ArrayList<Object>();
                for (int i = 0; i < columnCount; i++)
                    row.add(rs.getObject(i));
                data.add(row);
            }

            return data;
        }
    }
    
    private class ImportDataFormEntity extends FormEntity {
        private ImportDataFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            
            addPropertyDraw(new LP[] {importData, rublevskiLM.getLPByName("importUrl"), rublevskiLM.getLPByName("importLogin"), rublevskiLM.getLPByName("importPassword")});
        }
    }
}

