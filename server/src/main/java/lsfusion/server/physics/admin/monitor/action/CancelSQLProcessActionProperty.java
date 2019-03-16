package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class CancelSQLProcessActionProperty extends InternalAction {
    private final ClassPropertyInterface integerInterface;

    public CancelSQLProcessActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer processId = (Integer) findProperty("idSQLProcess[VARSTRING[10]]").read(context, currentObject);
            SQLSession cancelSession = SQLSession.getSQLSessionMap().get(processId);
            if (cancelSession != null)
                cancelSession.setForcedCancel(true);
            context.getSession().sql.executeDDL(context.getDbSyntax().getCancelActiveTaskQuery(processId));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
