package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.LogicsModule;

public class ModuleLAFinder extends ModuleLAPFinder<LA<?>> {
    @Override
    protected Iterable<LA<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
