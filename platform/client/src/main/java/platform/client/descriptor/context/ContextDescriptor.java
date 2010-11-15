package platform.client.descriptor.context;

import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextHolder;

public class ContextDescriptor implements ApplicationContextHolder {

    protected ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public void updateDependency(Object object, String field) {
        context.updateDependency(object, field);
    }
}
