package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class CancelJavaProcessAction extends InternalAction {
    private final ClassPropertyInterface integerInterface;

    public CancelJavaProcessAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer id = Integer.parseInt((String) findProperty("idThreadProcess[STRING[10]]").read(context, currentObject));
            Thread thread = ThreadUtils.getThreadById(id);
            if (thread != null) {
                ThreadUtils.interruptThread(context, thread);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
                     