package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LCP;

public class ModuleIndirectLCPFinder extends ModuleIndirectLAPFinder<LCP<?>> {

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
