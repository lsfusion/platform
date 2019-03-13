package lsfusion.server.language.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.group.AbstractGroup;

public class ModuleGroupFinder extends ModuleSingleElementFinder<AbstractGroup, Object> {
    @Override
    protected AbstractGroup getElement(LogicsModule module, String simpleName, Object param) {
        return module.getGroup(simpleName);
    }
}
