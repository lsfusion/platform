package lsfusion.server.physics.admin.service.action;

import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class PopSettingAction extends InternalAction {
    private ClassPropertyInterface nameInterface;

    public PopSettingAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        nameInterface = i.next();
    }
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String name = (String) context.getDataKeyValue(nameInterface).getValue();
        ThreadLocalContext.popSettings(name);
    }
}