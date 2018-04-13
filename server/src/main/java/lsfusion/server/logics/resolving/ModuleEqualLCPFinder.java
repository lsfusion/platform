package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LCP;

public class ModuleEqualLCPFinder extends ModuleEqualLPFinder<LCP<?>> {
    protected final boolean findLocals;

    public ModuleEqualLCPFinder(boolean findLocals) {
        this.findLocals = findLocals;
    }

    @Override
    public boolean isFiltered(LCP<?> property) {
        return (findLocals || !property.property.isLocal());
    }

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
