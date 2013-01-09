package platform.server.logics.service;

import platform.server.classes.ValueClass;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.scripted.ScriptingActionProperty;

public class GarbageCollectorActionProperty extends ScriptingActionProperty {
    public GarbageCollectorActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        System.runFinalization();
        System.gc();
    }

}