package lsfusion.server.language.resolving;

import lsfusion.server.logics.LogicsModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Поиск элемента системы по пространству имен и простому имени среди набора модулей (необязательно в этом пространстве имен)
 * @param <T> тип искомого элемента системы
 * @param <P> тип параметра finder'а 
 */

public class NamespaceElementFinder<T, P> {
    private ModuleFinder<T, P> finder;
    private Collection<LogicsModule> modules;
    
    public static class FoundItem<T> {
        public T value;
        public LogicsModule module;

        public FoundItem(T value, LogicsModule module) {
            this.value = value;
            this.module = module;
        }

        @Override
        public String toString() {
            return module.getName() + " (" + value.toString() + ")";
        }
    }

    public NamespaceElementFinder(ModuleFinder<T, P> finder, Collection<LogicsModule> modules) {
        this.finder = finder;
        this.modules = modules;
    }

    public List<FoundItem<T>> findInNamespace(String namespaceName, String name) {
        return findInNamespace(namespaceName, name, null);
    }
    
    public List<FoundItem<T>> findInNamespace(String namespaceName, String name, P param) {
        List<FoundItem<T>> result = new ArrayList<>();
        for (LogicsModule module : modules) {
            if (namespaceName.equals(module.getNamespace())) {
                List<T> moduleResult = finder.resolveInModule(module, name, param);
                for (T element : moduleResult) {
                    result.add(new FoundItem<>(element, module));
                }
            }
        }
        return finalizeResult(result);
    }

    protected List<FoundItem<T>> finalizeResult(final List<FoundItem<T>> result) {
        return result;
    }
}
