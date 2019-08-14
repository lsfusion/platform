package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class RunJDBCStatementAction extends InternalAction {
    private final ClassPropertyInterface connectionStringInterface;
    private final ClassPropertyInterface jdbcStatementInterface;

    public RunJDBCStatementAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        connectionStringInterface = i.next();
        jdbcStatementInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext context) throws SQLException {

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