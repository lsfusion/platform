package fdk.region.by.integration.formular;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ImportFormularActionProperty extends ScriptingActionProperty {
    public ImportFormularActionProperty(ScriptingLogicsModule LM) {
        super(LM);

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        Connection conn = null;

        try {
            // Get a connection to the database
            conn = DriverManager.getConnection(((String) getLCP("importUrl").read(context)).trim(),
                    ((String) getLCP("importLogin").read(context)).trim(),
                    ((String) getLCP("importPassword").read(context)).trim());

            importItemGroup(context, conn);

            // Close the result set, statement and the connection
        } catch (SQLException e) {
            context.delayUserInterfaction(new MessageClientAction("Ошибка при подключении к базе данных : " + e.getLocalizedMessage(), "Импорт данных"));
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

        ImportField idItemGroup = new ImportField(getLCP("idItemGroup"));
        ImportField itemGroupName = new ImportField(getLCP("nameItemGroup"));
        ImportField idParentGroup = new ImportField(getLCP("idItemGroup"));

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLCP("itemGroupId").getMapping(idItemGroup));
        ImportProperty<?> itemGroupIDProperty = new ImportProperty(idItemGroup, getLCP("idItemGroup").getMapping(itemGroupKey));
        ImportProperty<?> itemGroupNameProperty = new ImportProperty(itemGroupName, getLCP("nameItemGroup").getMapping(itemGroupKey));

        ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) getClass("itemGroup"),
                getLCP("itemGroupId").getMapping(idParentGroup));

        ImportProperty<?> parentGroupProperty = new ImportProperty(idParentGroup, getLCP("parentItemGroup").getMapping(itemGroupKey),
                LM.object(getClass("itemGroup")).getMapping(parentGroupKey));

        Collection<? extends ImportKey<?>> keys = Arrays.asList(itemGroupKey, parentGroupKey);
        Collection<ImportProperty<?>> properties = Arrays.asList(itemGroupIDProperty, itemGroupNameProperty, parentGroupProperty);

        new IntegrationService(context.getSession(),
                new ImportTable(Arrays.asList(idItemGroup, itemGroupName, idParentGroup), createData(rs)),
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
