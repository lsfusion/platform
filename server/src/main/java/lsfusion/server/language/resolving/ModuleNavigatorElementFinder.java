package lsfusion.server.language.resolving;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.LogicsModule;

public class ModuleNavigatorElementFinder extends ModuleSingleElementFinder<NavigatorElement, Object> {
    @Override
    protected NavigatorElement getElement(LogicsModule module, String simpleName, Object param) {
        return module.getNavigatorElement(simpleName);
    }
}
