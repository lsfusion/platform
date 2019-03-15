package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Iterator;

public class RunSQLScriptActionProperty extends ScriptingAction {
    private final ClassPropertyInterface stringInterface;

    public RunSQLScriptActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String script = (String) context.getDataKeyValue(stringInterface).object;
            if(script != null) {
                ServerLoggers.sqlLogger.info("Executing SQL: " + script);
                context.getSession().sql.executeDDL(script, StaticExecuteEnvironmentImpl.NOREADONLY);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}