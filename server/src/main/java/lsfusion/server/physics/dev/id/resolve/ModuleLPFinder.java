package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;

public class ModuleLPFinder extends ModuleLAPFinder<LP<?>> {
    @Override
    protected Iterable<LP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
