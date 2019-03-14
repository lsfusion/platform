package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;

public class ModuleIndirectLPFinder extends ModuleIndirectLAPFinder<LP<?>> {

    @Override
    protected Iterable<LP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
