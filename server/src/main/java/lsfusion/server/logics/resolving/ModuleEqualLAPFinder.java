package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LP;

public class ModuleEqualLAPFinder extends ModuleEqualLPFinder<LAP<?>> {

    public ModuleEqualLAPFinder() {
        super(false);
    }

    private ModuleEqualLAPFinder(boolean findLocals) {
        super(true);
        assert findLocals;
    }

    @Override
    public ModuleEqualLPFinder<LAP<?>> findLocals() {
        assert !findLocals;
        return new ModuleEqualLAPFinder(true);
    }

    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }

}
