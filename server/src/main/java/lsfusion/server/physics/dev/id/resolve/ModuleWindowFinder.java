package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.navigator.NavigatorEntity;
import lsfusion.server.logics.navigator.window.AbstractWindow;

public final class ModuleWindowFinder extends ModuleNavigatorFinder<AbstractWindow> {
    public ModuleWindowFinder(LogicsModule startModule) {
        super(startModule);
    }

    @Override
    protected AbstractWindow getNF(NavigatorEntity navigator, String canonicalName, Version version) {
        return navigator.getNFWindow(canonicalName, version);
    }
}
