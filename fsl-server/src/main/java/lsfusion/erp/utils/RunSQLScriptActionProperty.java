package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class RunSQLScriptActionProperty extends ScriptingActionProperty {
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