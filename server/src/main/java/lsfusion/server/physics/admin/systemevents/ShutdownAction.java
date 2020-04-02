package lsfusion.server.physics.admin.systemevents;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class ShutdownAction extends InternalAction {
    private final ClassPropertyInterface connectionInterface;

    public ShutdownAction(SystemEventsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        connectionInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        DataObject connection = context.getDataKeyValue(connectionInterface);
        context.getLogicsInstance().getNavigatorsManager().shutdownConnection(connection);
    }
}