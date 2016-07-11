package lsfusion.server.logics;

import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.util.*;

/**
 * Created by DAle on 09.04.14.
 */

public class NamespaceElementFinder<T, P> {
    private LogicsModule.ModuleFinder<T, P> finder;
    private List<LogicsModule> modules;
    
    public static class FoundItem<T> {
        public T value;
        public LogicsModule module;

        public FoundItem(T value, LogicsModule module) {
            this.value = value;
            this.module = module;
        }
    }

    public NamespaceElementFinder(LogicsModule.ModuleFinder<T, P> finder, List<LogicsModule> modules) {
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

    protected List<FoundItem<T>> finalizeResult(List<FoundItem<T>> result) {
        return result;
    }
}
