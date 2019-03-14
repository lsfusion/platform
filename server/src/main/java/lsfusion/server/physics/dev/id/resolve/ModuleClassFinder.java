package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.LogicsModule;

public class ModuleClassFinder extends ModuleSingleElementFinder<CustomClass, Object> {
    @Override
    protected CustomClass getElement(LogicsModule module, String simpleName, Object param) {
        return module.getClass(simpleName);
    }
}
