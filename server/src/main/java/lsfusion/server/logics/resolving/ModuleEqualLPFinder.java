package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.List;

// Находит идентичные по имени и сигнатуре свойства.   
public class ModuleEqualLPFinder extends ModulePropertyOrActionFinder<LP<?, ?>> {
    private final boolean findLocals;

    public ModuleEqualLPFinder(boolean findLocals) {
        this.findLocals = findLocals;
    }

    @Override
    protected Iterable<LP<?, ?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedPropertiesAndActions(name);
    }

    @Override
    protected boolean accepted(LogicsModule module, LP<?, ?> property, List<ResolveClassSet> signature) {
        return (findLocals || !property.property.isLocal()) && 
                SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false) && 
                SignatureMatcher.isCompatible(signature, module.getParamClasses(property), false, false);
    }
}
