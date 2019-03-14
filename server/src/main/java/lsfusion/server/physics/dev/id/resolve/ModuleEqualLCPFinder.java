package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LCP;

public class ModuleEqualLCPFinder extends ModuleEqualLAPFinder<LCP<?>> {
    protected final boolean findLocals;

    public ModuleEqualLCPFinder(boolean findLocals) {
        this.findLocals = findLocals;
    }

    @Override
    public boolean isFiltered(LCP<?> property) {
        return (!findLocals && property.property.isLocal());
    }

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
