package lsfusion.server.language.resolving;

import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.logics.LogicsModule;

public class ModuleWindowFinder extends ModuleSingleElementFinder<AbstractWindow, Object> {
    @Override
    protected AbstractWindow getElement(LogicsModule module, String simpleName, Object param) {
        return module.getWindow(simpleName);
    }
}
