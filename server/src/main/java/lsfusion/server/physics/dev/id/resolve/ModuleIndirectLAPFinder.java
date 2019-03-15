package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public abstract class ModuleIndirectLAPFinder<L extends LAP<?, ?>> extends ModulePropertyOrActionFinder<L> {

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isSoftCompatible(module.getParamClasses(property), signature);
    }
}
