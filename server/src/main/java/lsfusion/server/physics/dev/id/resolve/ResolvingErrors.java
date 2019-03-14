package lsfusion.server.physics.dev.id.resolve;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;

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

    // java не позволяет generics от throwable делать
    public static class ResolvingAmbiguousPropertyError extends ResolvingError {
        public List<NamespaceElementFinder.FoundItem<LP<?, ?>>> foundItems;
        public String name;
        
        public <L extends LP<?,?>> ResolvingAmbiguousPropertyError(List<NamespaceElementFinder.FoundItem<L>> items, String name) {
            foundItems = BaseUtils.<List<NamespaceElementFinder.FoundItem<LP<?, ?>>>>immutableCast(items);
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
