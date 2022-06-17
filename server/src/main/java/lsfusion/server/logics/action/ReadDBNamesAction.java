package lsfusion.server.logics.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.List;

public class ReadDBNamesAction extends InternalAction {
    public ReadDBNamesAction(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String currentDBName = context.getBL().getDbManager().getAdapter().getDBName();
        List<String> unusedDBNames = DataAdapter.getAllDBNames();
        unusedDBNames.remove(currentDBName);
        unusedDBNames.forEach(dbName -> changeProperty("unusedDBNames[STRING]", dbName, context));
        changeProperty("currentDBName[]", currentDBName, context);
    }

    private void changeProperty(String propertyName, String value, ExecutionContext<ClassPropertyInterface> context) {
        try {
            findProperty(propertyName).change(value, context, new DataObject(value, StringClass.text));
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
