package retail.actions;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import retail.RetailBusinessLogics;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ImportDataActionProperty extends ScriptingActionProperty {
    private ScriptingLogicsModule retailLM;

    public ImportDataActionProperty(RetailBusinessLogics BL) {
        super(BL);
        retailLM = BL.getLM();

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
            conn = DriverManager.getConnection(((String) retailLM.getLCPByName("importUrl").read(context)).trim(),
                    ((String) retailLM.getLCPByName("importLogin").read(context)).trim(),
                    ((String) retailLM.getLCPByName("importPassword").read(context)).trim());

            importItemGroup(context, conn);

            // Close the result set, statement and the connection
        } catch (SQLException e) {
            context.pendUserInterfaction(new MessageClientAction("Ошибка при подключении к базе данных : " + e.getLocalizedMessage(), "Импорт данных"));
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    private void importItemGroup(ExecutionContext context, Connection conn) throws SQLException {

        ResultSet rs = conn.createStatement().executeQuery(
                "SELECT num_class AS ext_id, name_u AS name, par AS par_id FROM klass");

        ImportField itemGroupID = new ImportField(BL.LM.extSID);
        ImportField itemGroupName = new ImportField(BL.LM.name);
        ImportField parentGroupID = new ImportField(BL.LM.extSID);

        ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                BL.LM.extSIDToObject.getMapping(itemGroupID));
        ImportProperty<?> itemGroupIDProperty = new ImportProperty(itemGroupID, BL.LM.extSID.getMapping(itemGroupKey));
        ImportProperty<?> itemGroupNameProperty = new ImportProperty(itemGroupName, BL.LM.name.getMapping(itemGroupKey));

        ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("itemGroup"),
                BL.LM.extSIDToObject.getMapping(parentGroupID));

        ImportProperty<?> parentGroupProperty = new ImportProperty(parentGroupID, retailLM.getLCPByName("parentItemGroup").getMapping(itemGroupKey),
                BL.LM.object((ConcreteCustomClass) retailLM.getClassByName("itemGroup")).getMapping(parentGroupKey));

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
