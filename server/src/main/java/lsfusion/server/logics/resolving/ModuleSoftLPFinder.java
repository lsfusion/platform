package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.List;

public class ModuleSoftLPFinder extends ModulePropertyOrActionFinder<LP<?, ?>> {
    @Override
    protected List<LP<?, ?>> getSourceList(LogicsModule module, String name) {
        return module.getAllLPByName(name);
    }

    @Override
    protected boolean accepted(LogicsModule module, LP<?, ?> property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isSoftCompatible(module.getParamClasses(property), signature);
    }
}
