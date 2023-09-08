package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.CopyToClipboardClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class CopyFullQuerySQLProcessAction extends InternalAction {
    private final ClassPropertyInterface integerInterface;

    public CopyFullQuerySQLProcessAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            String fullQuery = (String) findProperty("fullQuerySQLProcess[STRING[10]]").read(context, currentObject);
            if (fullQuery != null && !fullQuery.isEmpty()) {
                context.requestUserInteraction(new CopyToClipboardClientAction(fullQuery));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
                     