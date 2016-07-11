package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class KillActiveTaskActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public KillActiveTaskActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            getActiveTasksFromDatabase(context);
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }

    }


    private void getActiveTasksFromDatabase(ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        DataObject currentObject = context.getDataKeyValue(integerInterface);
        Integer pid = (Integer) findProperty("idActiveTask[INTEGER]").read(context, currentObject);
        
        context.getDbManager().getAdapter().killProcess(pid);

        findProperty("idActiveTask[INTEGER]").change((Object) null, context, currentObject);

    }
}
