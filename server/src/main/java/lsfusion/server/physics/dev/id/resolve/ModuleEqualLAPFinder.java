package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.sets.ResolveClassSet;

import java.util.List;

// Находит идентичные по имени и сигнатуре свойства.   
public abstract class ModuleEqualLAPFinder<L extends LAP<?, ?>> extends ModulePropertyOrActionFinder<L> {
    public abstract boolean isFiltered(L property);
    
    @Override
    protected boolean accepted(LogicsModule module, L property, List<ResolveClassSet> signature) {
        if (!isFiltered(property)) {
            List<ResolveClassSet> paramClasses = module.getParamClasses(property);
            boolean equals = SignatureMatcher.isEqualsCompatible(paramClasses, signature); // чтобы не вызывать ветку в ResolveOrObjectClassSet.containsAll
            assert equals == (SignatureMatcher.isCompatible(paramClasses, signature, false, false) &&
                              SignatureMatcher.isCompatible(signature, paramClasses, false, false));
            return equals;
        }
        return false;
    }
}
