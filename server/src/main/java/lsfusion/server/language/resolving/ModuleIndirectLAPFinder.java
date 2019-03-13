package lsfusion.server.language.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LAP;

public class ModuleIndirectLAPFinder extends ModuleIndirectLPFinder<LAP<?>> {

    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }
}
