package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ModulePropertyOrActionFinder<T extends LAP<?, ?>> implements ModuleFinder<T, List<ResolveClassSet>> {
    @Override
    public List<T> resolveInModule(LogicsModule module, String simpleName, List<ResolveClassSet> signature) {
        // resolving an unqualified name walks every module of the required closure, and for all but a handful of them nothing
        // matches - so the list is allocated only once something does, and the callers just iterate what they get back
        List<T> result = null;
        for (T property : getSourceList(module, simpleName)) {
            if (accepted(module, property, signature)) {
                if (result == null)
                    result = new ArrayList<>();
                result.add(property);
            }
        }
        return result == null ? Collections.emptyList() : result;
    }
    
    protected abstract Iterable<T> getSourceList(LogicsModule module, String name);
    
    protected abstract boolean accepted(LogicsModule module, T property, List<ResolveClassSet> signature);
}
