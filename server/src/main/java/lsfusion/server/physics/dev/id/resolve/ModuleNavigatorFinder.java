package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.navigator.NavigatorEntity;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

// an element, a window of the navigator is no module's : it is found in the navigator, as a name of a form's is found in
// the form (FormEntity.getNFObject), among what the module that reads the name sees of it at its version, by the
// canonical name the namespace of the module asked gives it. Every module of a namespace would give the same, so only the
// first of them that the reading module requires is asked
public abstract class ModuleNavigatorFinder<T> extends ModuleSingleElementFinder<T, Object> {
    private final LogicsModule startModule;

    protected ModuleNavigatorFinder(LogicsModule startModule) {
        this.startModule = startModule;
    }

    @Override
    protected T getElement(LogicsModule module, String simpleName, Object param) {
        String namespace = module.getNamespace();
        if (startModule.getRequiredModules(namespace).get(0) != module)
            return null;
        return getNF(startModule.getNavigator(), CanonicalNameUtils.createCanonicalName(namespace, simpleName), startModule.getVersion());
    }

    protected abstract T getNF(NavigatorEntity navigator, String canonicalName, Version version);
}
