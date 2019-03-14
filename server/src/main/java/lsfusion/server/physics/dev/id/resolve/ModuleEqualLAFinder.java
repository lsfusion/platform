package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.linear.LA;
import lsfusion.server.logics.LogicsModule;

public class ModuleEqualLAFinder extends ModuleEqualLAPFinder<LA<?>> {
    @Override
    public boolean isFiltered(LA<?> property) {
        return false;
    }

    @Override
    protected Iterable<LA<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
