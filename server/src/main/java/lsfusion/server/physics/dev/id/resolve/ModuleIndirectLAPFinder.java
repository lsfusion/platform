package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LAP;

import java.util.List;

public abstract class ModuleIndirectLAPFinder<L extends LAP<?, ?>> extends ModulePropertyOrActionFinder<L> {

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isSoftCompatible(module.getParamClasses(property), signature);
    }
}
