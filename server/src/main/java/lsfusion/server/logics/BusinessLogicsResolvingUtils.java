package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.resolving.*;
import lsfusion.server.logics.resolving.NamespaceElementFinder.FoundItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BusinessLogicsResolvingUtils {
    
    public static class AmbiguousElementException extends RuntimeException {
        public String name;
        public List<? extends FoundItem> foundItems;

        public AmbiguousElementException(String name, List<? extends FoundItem> foundItems) {
            this.name = name;
            this.foundItems = foundItems;
        }

        @Override
        public String getMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Ambiguous element '%s' was found in modules: ", name));
            for (FoundItem item : foundItems) {
                if (item != foundItems.get(0)) {
                    builder.append(", ");    
                }
                builder.append(item.toString());
            }
            return builder.toString();
        }
    }

    public static <R, P> R findElementByCanonicalName(BusinessLogics<?> BL, String canonicalName, P param, ModuleFinder<R, P> moduleFinder) {
        assert canonicalName != null;
        String namespaceName = CanonicalNameUtils.getNamespace(canonicalName);
        String elementName = CanonicalNameUtils.getName(canonicalName);

        FoundItem<R> found = findElementInNamespace(BL, namespaceName, elementName, param, moduleFinder);
        return found == null ? null : found.value;
    }

    public static <R, P> R findElementByCompoundName(BusinessLogics<?> BL, String compoundName, P param, ModuleFinder<R, P> moduleFinder) {
        assert compoundName != null;
        String elementName = CompoundNameUtils.getName(compoundName);
        Collection<String> namespaces = getConsideredNamespaces(BL, compoundName);

        List<FoundItem<R>> foundItems = new ArrayList<>();
        for (String namespaceName : namespaces) {
            FoundItem<R> result = findElementInNamespace(BL, namespaceName, elementName, param, moduleFinder);
            if (result != null) {
                foundItems.add(result);
            }
        }
        return findElementResult(compoundName, foundItems);
    }

    private static <R, P> FoundItem<R> findElementInNamespace(BusinessLogics<?> BL, String namespace, String name, P param, ModuleFinder<R, P> moduleFinder) {
        NamespaceElementFinder<R, P> finder = new NamespaceElementFinder<>(moduleFinder, BL.getNamespaceModules(namespace));
        List<FoundItem<R>> resList = finder.findInNamespace(namespace, name, param);
        assert resList.size() <= 1;
        return resList.size() == 0 ? null : resList.get(0);
    }

    private static Collection<String> getConsideredNamespaces(BusinessLogics<?> BL, String compoundName) {
        if (CompoundNameUtils.hasNamespace(compoundName)) {
            return Collections.singletonList(CompoundNameUtils.getNamespace(compoundName));
        } else {
            return BL.getNamespacesList();
        }
    }

    private static <R> R findElementResult(String name, List<? extends FoundItem<R>> foundItems) {
        if (foundItems.size() > 1) {
            throw new BusinessLogicsResolvingUtils.AmbiguousElementException(name, foundItems);
        } else {
            return foundItems.size() == 0 ? null : foundItems.get(0).value;
        }
    }

    public static <L extends LP<?,?>> L findPropertyByCanonicalName(BusinessLogics<?> BL, String canonicalName, ModuleEqualLPFinder<L> finder) {
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(BL, canonicalName);
        List<FoundItem<L>> foundElements = findProperties(BL, parser.getNamespace(), parser.getName(),
                parser.getSignature(), finder);
        assert foundElements.size() <= 1;
        return foundElements.size() == 0 ? null : foundElements.get(0).value;
    }
    
    public static <L extends LP<?,?>> L findLPByCompoundName(BusinessLogics<?> BL, String compoundName, ModuleLPFinder<L> moduleLPFinder) {
        PropertyCompoundNameParser parser = new PropertyCompoundNameParser(BL, compoundName);
        return findLP(BL, parser.getNamespace(), parser.getName(), parser.getSignature(), compoundName, moduleLPFinder);
    }
    
    private static <L extends LP<?, ?>> L findLP(BusinessLogics<?> BL, String namespace, String name, List<ResolveClassSet> signature, String sourceName, ModuleLPFinder<L> moduleLPFinder) {
        Collection<String> namespaces = getPropertyConsideredNamespaces(BL, namespace);

        List<FoundItem<L>> foundItems = new ArrayList<>();
        for (String namespaceName : namespaces) {
            foundItems.addAll(findProperties(BL, namespaceName, name, signature, moduleLPFinder));
        }

        List<FoundItem<L>> filteredResult = NamespaceLPFinder.filterFoundProperties(foundItems);
        return findElementResult(sourceName, filteredResult);
    }
    
    private static Collection<String> getPropertyConsideredNamespaces(BusinessLogics<?> BL, String namespace) {
        if (namespace != null) {
            return Collections.singletonList(namespace);
        } else {
            return BL.getNamespacesList();
        }
    }
    
    private static <L extends LP<?, ?>> List<FoundItem<L>> findProperties(BusinessLogics<?> BL, String namespace, String name, 
                                                            List<ResolveClassSet> classes, 
                                                            ModulePropertyOrActionFinder<L> finder) {
        NamespaceLPFinder<L> nsFinder = new NamespaceLPFinder<L>(finder, BL.getNamespaceModules(namespace));
        return nsFinder.findInNamespace(namespace, name, classes);
    }
}

