package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

// what the connection balancing does with a session whose owner closed it after the round took it : the session is
// closed first, then balanced as the round does it (balanceConnection, private, reached by reflection). What it ended
// with goes into cbFailure
public class BalanceClosedSessionAction extends InternalAction {

    public BalanceClosedSessionAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        SQLSession sql = context.getDbManager().createSQL();
        sql.close();
        String failure;
        try {
            Method balance = SQLSession.class.getDeclaredMethod("balanceConnection", DataAdapter.NeedServer.class, boolean.class);
            balance.setAccessible(true);
            Object moved = balance.invoke(sql, DataAdapter.NeedServer.BEST, false);
            failure = Boolean.FALSE.equals(moved) ? null : "a closed session's connection was moved";
        } catch (InvocationTargetException e) {
            failure = "balancing a session closed after the balancing took it failed: " + e.getCause();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }

        try {
            findProperty("cbFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
