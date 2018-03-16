package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LAP;

public class ModuleLAPFinder extends ModuleLPFinder<LAP<?>> {
    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
