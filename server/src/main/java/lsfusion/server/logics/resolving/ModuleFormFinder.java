package lsfusion.server.logics.resolving;

import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.LogicsModule;

public class ModuleFormFinder extends ModuleSingleElementFinder<NavigatorElement, Object> {
    // todo [dale]: при переходе на ветку нужно изменить
    @Override
    protected NavigatorElement getElement(LogicsModule module, String simpleName, Object param) {
        return module.getNavigatorElement(simpleName);
    }
}
