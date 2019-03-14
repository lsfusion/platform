package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.LogicsModule;

public class ModuleWindowFinder extends ModuleSingleElementFinder<AbstractWindow, Object> {
    @Override
    protected AbstractWindow getElement(LogicsModule module, String simpleName, Object param) {
        return module.getWindow(simpleName);
    }
}
