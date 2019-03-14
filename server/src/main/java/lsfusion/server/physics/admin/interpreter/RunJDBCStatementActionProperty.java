package lsfusion.server.physics.admin.interpreter;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class RunJDBCStatementActionProperty extends ScriptingAction {
    private final ClassPropertyInterface connectionStringInterface;
    private final ClassPropertyInterface jdbcStatementInterface;

    public RunJDBCStatementActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        connectionStringInterface = i.next();
        jdbcStatementInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext context) throws SQLException, SQLHandledException {

        String connectionString = (String) context.getKeyValue(connectionStringInterface).getValue();
        String jdbcStatement = (String) context.getKeyValue(jdbcStatementInterface).getValue();

        if (connectionString != null && jdbcStatement != null) {

            Connection conn = null;
            try {

                //todo: делать class.forName в зависимости от типа строки
                Class.forName("com.mysql.jdbc.Driver");

                conn = DriverManager.getConnection(connectionString);
                conn.setAutoCommit(false);
                Statement statement = conn.createStatement();
                statement.execute(jdbcStatement);
                conn.commit();
            } catch (ClassNotFoundException e) {
                throw Throwables.propagate(e);
            } finally {
                if (conn != null)
                    conn.close();
            }
        }
    }
}