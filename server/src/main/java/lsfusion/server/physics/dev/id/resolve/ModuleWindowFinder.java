package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.navigator.window.AbstractWindow;

public class ModuleWindowFinder extends ModuleSingleElementFinder<AbstractWindow, Object> {
    @Override
    protected AbstractWindow getElement(LogicsModule module, String simpleName, Object param) {
        return module.getWindow(simpleName);
    }
}
