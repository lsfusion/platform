package retail.actions;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ImportDataActionProperty extends ScriptingActionProperty {
    public ImportDataActionProperty(ScriptingLogicsModule LM) {
        super(LM);

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
            conn = DriverManager.getConnection(((String) getLP("importUrl").read(context)).trim(),
                    ((String) getLP("importLogin").read(context)).trim(),
                    ((String) getLP("importPassword").read(context)).trim());

            importItemGroup(context, conn);

            // Close the result set, statement and the connection
        } catch (SQLException e) {
            context.addAction(new MessageClientAction("Ошибка при подключении к базе данных : " + e.getLocalizedMessage(), "Импорт данных"));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    private void importItemGroup(ExecutionContext context, Connection conn) throws SQLException, ScriptingErrorLog.SemanticErrorException {

        ResultSet rs = conn.createStatement().executeQuery(
                "SELECT num_class AS ext_id, name_u AS name, par AS par_id FROM klass");

        ImportField itemGroupID = new ImportField(getLP("extSID"));
        ImportField itemGroupName = new ImportField(getLP("name"));
        ImportField parentGroupID = new ImportField(getLP("extSID"));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLP("extSIDToObject").getMapping(itemGroupID));
        ImportProperty<?> itemGroupIDProperty = new ImportProperty(itemGroupID, getLP("extSID").getMapping(itemGroupKey));
        ImportProperty<?> itemGroupNameProperty = new ImportProperty(itemGroupName, getLP("name").getMapping(itemGroupKey));

        ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLP("extSIDToObject").getMapping(parentGroupID));

        ImportProperty<?> parentGroupProperty = new ImportProperty(parentGroupID, getLP("parentItemGroup").getMapping(itemGroupKey),
                LM.object((ConcreteCustomClass) getClass("itemGroup")).getMapping(parentGroupKey));

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
