package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;

import java.util.Collections;
import java.util.List;

public abstract class ModuleSingleElementFinder<T, P> implements ModuleFinder<T, P> {
    @Override
    public final List<T> resolveInModule(LogicsModule module, String simpleName, P param) {
        T element = getElement(module, simpleName, param);
        return element == null ? Collections.<T>emptyList() : Collections.singletonList(element);
    }
    
    protected abstract T getElement(LogicsModule module, String simpleName, P param);
}
