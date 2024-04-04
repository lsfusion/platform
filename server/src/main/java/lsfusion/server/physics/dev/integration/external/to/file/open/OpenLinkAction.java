package lsfusion.server.physics.dev.integration.external.to.file.open;

import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.net.URI;
import java.util.Iterator;

public class OpenLinkAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface noWaitInterface;

    public OpenLinkAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        noWaitInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        ObjectValue sourceObject = context.getKeyValue(sourceInterface);
        boolean noWait = context.getKeyValue(noWaitInterface).getValue() != null;
        if (sourceObject != null) {
            for (URI file : ((LinkClass) sourceObject.getType()).getFiles(sourceObject.getValue())) {
                OpenUriClientAction action = new OpenUriClientAction(file.toString(), true);
                if (noWait) {
                    context.delayUserInteraction(action);
                } else {
                    context.requestUserInteraction(action);
                }
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}