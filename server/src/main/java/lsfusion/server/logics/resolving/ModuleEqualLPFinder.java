package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.List;

// Находит идентичные по имени и сигнатуре свойства.   
public abstract class ModuleEqualLPFinder<L extends LP<?, ?>> extends ModulePropertyOrActionFinder<L> {
    protected final boolean findLocals;

    protected ModuleEqualLPFinder(boolean findLocals) {
        this.findLocals = findLocals;
    }
    
    public abstract ModuleEqualLPFinder<L> findLocals();

    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
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
