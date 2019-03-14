package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.linear.LA;
import lsfusion.server.logics.LogicsModule;

public class ModuleIndirectLAFinder extends ModuleIndirectLPFinder<LA<?>> {

    @Override
    protected Iterable<LA<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
