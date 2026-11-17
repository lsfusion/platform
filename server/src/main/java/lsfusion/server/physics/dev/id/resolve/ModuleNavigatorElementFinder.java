package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorEntity;

public final class ModuleNavigatorElementFinder extends ModuleNavigatorFinder<NavigatorElement> {
    public ModuleNavigatorElementFinder(LogicsModule startModule) {
        super(startModule);
    }

    @Override
    protected NavigatorElement getNF(NavigatorEntity navigator, String canonicalName, Version version) {
        return navigator.getNFElement(canonicalName, version);
    }
}
