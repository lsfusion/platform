package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;

import java.util.List;

public abstract class ModuleAbstractLPFinder<L extends LP<?, ?>> extends ModulePropertyOrActionFinder<L> {

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        return isAbstract(property.property) && 
                SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false);
    }

    protected abstract boolean isAbstract(Property property);
}
