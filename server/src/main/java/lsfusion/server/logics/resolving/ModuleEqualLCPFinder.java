package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LCP;

public class ModuleEqualLCPFinder extends ModuleEqualLPFinder<LCP<?>> {

    public ModuleEqualLCPFinder() {
        super(false);
    }
    private ModuleEqualLCPFinder(boolean findLocals) {
        super(true);
        assert findLocals;
    }

    @Override
    public ModuleEqualLPFinder<LCP<?>> findLocals() {
        assert !findLocals;
        return new ModuleEqualLCPFinder(true);
    }

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
