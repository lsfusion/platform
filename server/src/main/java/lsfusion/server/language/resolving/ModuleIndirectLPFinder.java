package lsfusion.server.language.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;

import java.util.List;

public abstract class ModuleIndirectLPFinder<L extends LP<?, ?>> extends ModulePropertyOrActionFinder<L> {

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isSoftCompatible(module.getParamClasses(property), signature);
    }
}
