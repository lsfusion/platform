package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class CancelActiveTaskActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public CancelActiveTaskActionProperty(ScriptingLogicsModule LM) {
        super(LM, IntegerClass.instance);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            getActiveTasksFromDatabase(context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }

    }


    private void getActiveTasksFromDatabase(ExecutionContext context) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        DataObject currentObject = context.getDataKeyValue(integerInterface);
        Integer pid = (Integer) findProperty("idActiveTask").read(context, currentObject);
        
        context.getSession().sql.executeDDL(context.getDbManager().getAdapter().getCancelActiveTaskQuery(pid));
        
        findAction("getActiveTasksAction").execute(context);

    }
}
