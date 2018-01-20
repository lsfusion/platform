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
        if (findLocals || !property.property.isLocal()) {
            List<ResolveClassSet> paramClasses = module.getParamClasses(property);
            boolean equals = SignatureMatcher.isEqualsCompatible(paramClasses, signature); // чтобы не вызывать ветку в ResolveOrObjectClassSet.containsAll
            assert equals == (SignatureMatcher.isCompatible(paramClasses, signature, false, false) &&
                              SignatureMatcher.isCompatible(signature, paramClasses, false, false));
            return equals;
        }
        return false;
    }
}
