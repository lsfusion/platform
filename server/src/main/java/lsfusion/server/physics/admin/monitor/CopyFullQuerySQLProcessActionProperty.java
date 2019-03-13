package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.interop.action.CopyToClipboardClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class CopyFullQuerySQLProcessActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public CopyFullQuerySQLProcessActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            String fullQuery = (String) findProperty("fullQuerySQLProcess[VARSTRING[10]]").read(context, currentObject);
            if(fullQuery != null && !fullQuery.isEmpty()) {
                if (!(boolean) context.requestUserInteraction(new CopyToClipboardClientAction(fullQuery)))
                    findProperty("copiedFullQuerySQLProcess[VARSTRING[10]]").change(fullQuery, context, currentObject);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
                     