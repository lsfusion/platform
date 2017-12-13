package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;

import java.sql.SQLException;

public class GetAllTasksActionProperty extends GetTasksActionProperty {

    public GetAllTasksActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            
            getTasksFromDatabase(context, false); 

        } catch (SQLHandledException | ScriptingModuleErrorLog.SemanticError e) {
            throw Throwables.propagate(e);
        }

    }
}