package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.*;

import java.util.HashMap;
import java.util.Map;

public class ViewProxyFactory {
    private final static class InstanceHolder {
        private final static ViewProxyFactory instance = new ViewProxyFactory();
    }

    public static ViewProxyFactory getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<Class<?>, Class<? extends ViewProxy>> proxyClasses = new HashMap();

    private ViewProxyFactory() {
        proxyClasses.put(ComponentView.class, ComponentViewProxy.class);
        proxyClasses.put(ClassChooserView.class, ClassChooserViewProxy.class);
        proxyClasses.put(ContainerView.class, ContainerViewProxy.class);
        proxyClasses.put(FormView.class, FormViewProxy.class);
        proxyClasses.put(GridView.class, GridViewProxy.class);
        proxyClasses.put(GroupObjectView.class, GroupObjectViewProxy.class);
        proxyClasses.put(PropertyDrawView.class, PropertyDrawViewProxy.class);
        proxyClasses.put(ToolbarView.class, ToolbarViewProxy.class);
        proxyClasses.put(FilterView.class, FilterViewProxy.class);
        proxyClasses.put(TreeGroupView.class, TreeGroupViewProxy.class);
    }

    public ViewProxy createViewProxy(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("Object can't be null");
        }

        Class cz = target.getClass();
        while (cz != null && !proxyClasses.containsKey(cz)) {
            cz = cz.getSuperclass();
        }

        if (cz == null) {
            throw new RuntimeException("View proxy isn't supported for the object!");
        }

        Class<? extends ViewProxy> proxyClass = proxyClasses.get(cz);
        try {
            return proxyClass.getConstructor(cz).newInstance(target);
        } catch (Exception e) {
            throw new RuntimeException("Can't create object: ", e);
        }
    }
}
