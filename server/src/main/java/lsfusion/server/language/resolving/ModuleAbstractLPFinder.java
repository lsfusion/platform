package lsfusion.server.language.resolving;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.property.Property;

import java.util.List;

public abstract class ModuleAbstractLPFinder<L extends LP<?, ?>> extends ModulePropertyOrActionFinder<L> {

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return isAbstract(property.property) && 
                SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false);
    }

    protected abstract boolean isAbstract(Property property);
}
