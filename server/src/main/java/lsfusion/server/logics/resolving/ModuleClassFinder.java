package lsfusion.server.logics.resolving;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.LogicsModule;

public class ModuleClassFinder extends ModuleSingleElementFinder<CustomClass, Object> {
    @Override
    protected CustomClass getElement(LogicsModule module, String simpleName, Object param) {
        return module.getClass(simpleName);
    }
}
