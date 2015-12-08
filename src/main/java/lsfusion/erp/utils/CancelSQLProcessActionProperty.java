package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class CancelSQLProcessActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public CancelSQLProcessActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer processId = (Integer) findProperty("idSQLProcess").read(context, currentObject);
            SQLSession cancelSession = SQLSession.getSQLSessionMap().get(processId);
            if (cancelSession != null)
                cancelSession.setForcedCancel();
            context.getSession().sql.executeDDL(context.getDbManager().getAdapter().getCancelActiveTaskQuery(processId));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
