package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

public class GarbageCollectorActionProperty extends ScriptingActionProperty {
    public GarbageCollectorActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        System.runFinalization();
        System.gc();
    }

}