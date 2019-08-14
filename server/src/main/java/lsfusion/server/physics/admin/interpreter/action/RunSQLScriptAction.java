package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.query.exec.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class RunSQLScriptAction extends InternalAction {
    private final ClassPropertyInterface stringInterface;

    public RunSQLScriptAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
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