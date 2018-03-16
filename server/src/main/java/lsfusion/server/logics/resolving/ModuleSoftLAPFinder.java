package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;

public class ModuleSoftLAPFinder extends ModuleSoftLPFinder<LAP<?>> {

    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
