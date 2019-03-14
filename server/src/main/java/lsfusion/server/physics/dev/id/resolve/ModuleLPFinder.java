package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;

import java.util.List;

public abstract class ModuleLPFinder<L extends LP<?, ?>> extends ModulePropertyOrActionFinder<L> {
    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false);
    }
}
