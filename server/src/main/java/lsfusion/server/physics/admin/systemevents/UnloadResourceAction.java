package lsfusion.server.physics.admin.systemevents;

import lsfusion.interop.action.UnloadResourceClientAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class UnloadResourceAction extends InternalAction {
    private final ClassPropertyInterface resourceNameInterface;

    public UnloadResourceAction(SystemEventsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourceNameInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String resourceName = (String) context.getKeyValue(resourceNameInterface).getValue();
        context.delayUserInteraction(new UnloadResourceClientAction(resourceName));
    }
}