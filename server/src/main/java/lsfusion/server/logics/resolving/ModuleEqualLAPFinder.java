package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LAP;

public class ModuleEqualLAPFinder extends ModuleEqualLPFinder<LAP<?>> {
    @Override
    public boolean isFiltered(LAP<?> property) {
        return false;
    }

    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
