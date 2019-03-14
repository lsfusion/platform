package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class KillSQLProcessActionProperty extends ScriptingAction {
    private final ClassPropertyInterface integerInterface;

    public KillSQLProcessActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer processId = (Integer) findProperty("idSQLProcess[VARSTRING[10]]").read(context, currentObject);
            context.getDbManager().getAdapter().killProcess(processId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
