package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.List;

public class ResolvingErrors {

    public static abstract class ResolvingError extends Exception {
        
    }

    public static class ResolvingAmbiguousError extends ResolvingError {
        public String name;
        public List<LogicsModule> modules;

        public ResolvingAmbiguousError(List<LogicsModule> modules, String name) {
            this.modules = modules;
            this.name = name;
        }
    }

    public static class ResolvingAmbiguousPropertyError extends ResolvingError {
        public List<NamespaceElementFinder.FoundItem<LP<?, ?>>> foundItems;
        public String name;
        
        public ResolvingAmbiguousPropertyError(List<NamespaceElementFinder.FoundItem<LP<?, ?>>> items, String name) {
            foundItems = items;
            this.name = name;
        }
    }

    public static class ResolvingNamespaceError extends ResolvingError {
        public String namespaceName;

        public ResolvingNamespaceError(String namespaceName) {
            this.namespaceName = namespaceName;
        }
    }
}
