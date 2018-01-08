package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.resolving.NamespaceElementFinder.FoundItem;
import lsfusion.server.logics.resolving.ResolvingErrors.ResolvingError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementResolver<T, P> {
    protected ModuleFinder<T, P> finder;
    protected LogicsModule LM;
    
    public ElementResolver(LogicsModule LM, ModuleFinder<T, P> finder) {
        this.LM = LM;
        this.finder = finder;
    }

    public final T resolve(String compoundName) throws ResolvingError {
        return resolve(compoundName, null);
    }

    public final T resolve(String compoundName, P param) throws ResolvingError {
        T result;
        int dotPosition = compoundName.indexOf('.');
        if (dotPosition > 0) {
            String namespaceName = compoundName.substring(0, dotPosition);
            checkNamespace(namespaceName);
            String name = compoundName.substring(dotPosition + 1);
            List<FoundItem<T>> foundItems = findInNamespace(namespaceName, name, param);
            return finalizeResult(foundItems, compoundName, param).value;
        } else {
            List<String> namespaces = new ArrayList<>();
            namespaces.add(LM.getNamespace());
            namespaces.addAll(LM.getNamespacePriority());
            result = findInRequiredModules(compoundName, param, namespaces);
        }
        return result;
    }
    
    protected List<FoundItem<T>> findInNamespace(String namespaceName, String name, P param) throws ResolvingError {
        NamespaceElementFinder<T, P> nsFinder = new NamespaceElementFinder<>(finder, LM.getRequiredModules(namespaceName));
        return finalizeNamespaceResult(nsFinder.findInNamespace(namespaceName, name, param), name, param);
    }

    protected List<FoundItem<T>> finalizeNamespaceResult(List<FoundItem<T>> result, String name, P param) throws ResolvingError {
        FoundItem<T> finalRes = finalizeResult(result, name, param);
        return finalRes.value == null ? new ArrayList<FoundItem<T>>() : Collections.singletonList(finalRes);
    }

    // реализация по умолчанию, предполагающая, что не может быть более одного подходящего объекта
    protected FoundItem<T> finalizeResult(List<FoundItem<T>> result, String name, P param) throws ResolvingError {
        if (result.isEmpty()) return new FoundItem<>(null, null);
        if (result.size() > 1) {
            List<LogicsModule> resModules = new ArrayList<>();
            for (FoundItem<T> item : result) {
                resModules.add(item.module);
            }
            throw new ResolvingErrors.ResolvingAmbiguousError(resModules, name);
        }
        return result.get(0);
    }

    private T findInRequiredModules(String name, P param, List<String> namespaces) throws ResolvingError {
        for (String namespaceName : namespaces) {
            List<FoundItem<T>> result = findInNamespace(namespaceName, name, param);
            if (!result.isEmpty()) {
                return finalizeResult(result, name, param).value;
            }
        }

        List<FoundItem<T>> resultList = new ArrayList<>();
        for (List<LogicsModule> modules : LM.getNamespaceToModules().values()) {
            for (LogicsModule module : modules) {
                List<T> moduleResult = finder.resolveInModule(module, name, param);
                for (T obj : moduleResult) {
                    resultList.add(new FoundItem<>(obj, module));
                }
            }
        }
        return finalizeResult(resultList, name, param).value;
    }

    private void checkNamespace(String namespaceName) throws ResolvingError {
        if (!LM.getRequiredNamespaces().contains(namespaceName)) {
            throw new ResolvingErrors.ResolvingNamespaceError(namespaceName);
        }
    }
}
