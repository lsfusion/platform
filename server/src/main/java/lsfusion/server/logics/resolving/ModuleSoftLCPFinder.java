package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LCP;

public class ModuleSoftLCPFinder extends ModuleSoftLPFinder<LCP<?>> {

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
