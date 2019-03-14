package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LA;

public class ModuleEqualLAFinder extends ModuleEqualLPFinder<LA<?>> {
    @Override
    public boolean isFiltered(LA<?> property) {
        return false;
    }

    @Override
    protected Iterable<LA<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
