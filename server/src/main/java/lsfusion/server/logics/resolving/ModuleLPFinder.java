package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.List;

public class ModuleLPFinder extends ModulePropertyOrActionFinder<LP<?, ?>> {
    @Override
    protected Iterable<LP<?, ?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedPropertiesAndActions(name);
    }

    @Override
    protected boolean accepted(LogicsModule module, LP<?, ?> property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false);
    }
}
