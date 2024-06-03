package lsfusion.server.physics.admin.systemevents;

import lsfusion.base.file.FileData;
import lsfusion.interop.action.LoadResourceClientAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class LoadResourceAction extends InternalAction {
    private final ClassPropertyInterface resourceNameInterface;
    private final ClassPropertyInterface resourceFileInterface;

    public LoadResourceAction(SystemEventsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourceNameInterface = i.next();
        resourceFileInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String resourceName = (String) context.getKeyValue(resourceNameInterface).getValue();
        FileData resourceFile = (FileData) context.getDataKeyValue(resourceFileInterface).getValue();
        context.delayUserInteraction(new LoadResourceClientAction(resourceName, resourceFile));
    }
}